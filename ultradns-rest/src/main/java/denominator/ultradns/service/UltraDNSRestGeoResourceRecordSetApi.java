package denominator.ultradns.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import denominator.Provider;
import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.ultradns.service.integration.UltraDNSRest;
import denominator.ultradns.exception.UltraDNSRestException;
import denominator.ultradns.iterator.GroupGeoRecordByNameTypeCustomIterator;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.RRSetList;
import denominator.ultradns.model.RDataInfo;
import denominator.ultradns.model.GeoInfo;
import denominator.ultradns.model.Profile;
import denominator.ultradns.model.DirectionalRecord;
import denominator.ultradns.model.Region;
import denominator.ultradns.util.Constants;
import denominator.ultradns.util.RRSetUtil;
import denominator.ultradns.util.RegionUtil;
import denominator.ResourceTypeToValue.ResourceTypes;
import org.apache.commons.lang.StringUtils;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.concat;
import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import org.apache.log4j.Logger;

public final class UltraDNSRestGeoResourceRecordSetApi implements GeoResourceRecordSetApi {

  private static final Filter<ResourceRecordSet<?>> IS_GEO = new Filter<ResourceRecordSet<?>>() {
    @Override
    public boolean apply(ResourceRecordSet<?> in) {
      return in != null && in.geo() != null;
    }
  };
  private static final int DEFAULT_TTL = 300;
  private static final Logger LOGGER = Logger.getLogger(UltraDNSRestGeoResourceRecordSetApi.class);

  private final Collection<String> supportedTypes;
  private final Lazy<Map<Region, Collection<Region>>> regions;
  private final UltraDNSRest api;
  private final GroupGeoRecordByNameTypeCustomIterator.Factory iteratorFactory;
  private final String zoneName;
  private final Filter<DirectionalRecord> isCNAME = new Filter<DirectionalRecord>() {
    @Override
    public boolean apply(DirectionalRecord input) {
      return ResourceTypes.CNAME.name().equals(input.getType());
    }
  };

  UltraDNSRestGeoResourceRecordSetApi(Collection<String> supportedTypes,
                                      Lazy<Map<Region, Collection<Region>>> regions,
                                      UltraDNSRest api,
                                      GroupGeoRecordByNameTypeCustomIterator.Factory iteratorFactory,
                                      String zoneName) {
    this.supportedTypes = supportedTypes;
    this.regions = regions;
    this.api = api;
    this.iteratorFactory = iteratorFactory;
    this.zoneName = zoneName;
  }

  /**
   * Returns a map contains key as the region & value as all it's child regions/territories.
   * @return Map
   */
  private Map<Region, Collection<Region>> getAvailableRegions() {
    return regions.get();
  }

  /**
   * Returns supported region names.
   * @return map
   */
  @Override
  public Map<String, Collection<String>> supportedRegions() {
    return RegionUtil.getRegionNameHierarchy(getAvailableRegions());
  }

  /**
   * Iterates across all record sets in the zone. Implementations are lazy when possible.
   *
   * @return iterator which is lazy where possible
   * @throws IllegalArgumentException if the zone is not found.
   */
  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    List<Iterable<ResourceRecordSet<?>>> eachPool = new ArrayList<Iterable<ResourceRecordSet<?>>>();
    List<RRSet> rrSets = new ArrayList<RRSet>();
    try {
      RRSetList rrSetList = api.getDirectionalPoolsOfZone(zoneName);
      if (rrSetList != null) {
        rrSets = rrSetList.rrSets();
      }
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.DATA_NOT_FOUND) {
        throw e;
      }
    }
    final Map<String, Integer> nameAndType = RRSetUtil.getNameAndType(rrSets);
    for (final String poolName : nameAndType.keySet()) {
      eachPool.add(new Iterable<ResourceRecordSet<?>>() {
        public Iterator<ResourceRecordSet<?>> iterator() {
          return iteratorForDNameAndDirectionalType(poolName, nameAndType.get(poolName));
        }
      });
    }
    return concat(eachPool);
  }

  /**
   * a listing of all resource record sets which have the specified name.
   *
   * @return iterator which is lazy where possible, empty if there are no records with that name.
   * @throws IllegalArgumentException if the zone is not found.
   */
  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return iteratorForDNameAndDirectionalType(checkNotNull(name, "description"), ResourceTypes.ALL.code());
  }

  /**
   * a listing of all resource record sets by name and type.
   *
   * @param name {@link ResourceRecordSet#name() name} of the rrset
   * @param type {@link ResourceRecordSet#type() type} of the rrset
   * @return iterator which is lazy where possible, empty if there are no records with that name.
   * @throws IllegalArgumentException if the zone is not found.
   */
  @Override
  public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
    checkNotNull(name, "description");
    checkNotNull(type, "type");
    Filter<ResourceRecordSet<?>> filter = nameAndTypeEqualTo(name, type);
    if (!supportedTypes.contains(type)) {
      return Collections.<ResourceRecordSet<?>>emptyList().iterator();
    }
    if (ResourceTypes.CNAME.name().equals(type)) {
      // retain original type (this will filter out A, AAAA)
      return filter(
          concat(
            iteratorForDNameAndDirectionalType(name, lookup(ResourceTypes.A.name())),
            iteratorForDNameAndDirectionalType(name, lookup(ResourceTypes.AAAA.name()))
          ), filter
      );
    } else if (ResourceTypes.A.name().equals(type) || ResourceTypes.AAAA.name().equals(type)) {
      int dirType = ResourceTypes.AAAA.name().equals(type) ? lookup(ResourceTypes.AAAA.name())
              : lookup(ResourceTypes.A.name());
      Iterator<ResourceRecordSet<?>> iterator = iteratorForDNameAndDirectionalType(name, dirType);
      // retain original type (this will filter out CNAMEs)
      return filter(iterator, filter);
    } else {
      return iteratorForDNameAndDirectionalType(name, RRSetUtil.directionalRecordType(type));
    }
  }

  /**
   * retrieve a resource record set by name, type, and qualifier.
   *
   * @param name      {@link ResourceRecordSet#name() name} of the rrset
   * @param type      {@link ResourceRecordSet#type() type} of the rrset
   * @param qualifier {@link ResourceRecordSet#qualifier() qualifier} of the rrset
   * @return null unless a resource record exists with the same {@code name}, {@code type}, and
   * {@code qualifier}
   * @throws IllegalArgumentException if the zone is not found.
   */
  @Override
  public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type,
                                                        String qualifier) {
    checkNotNull(name, "description");
    checkNotNull(type, "type");
    checkNotNull(qualifier, "qualifier");
    if (!supportedTypes.contains(type)) {
      return null;
    }
    Iterator<DirectionalRecord> records = recordsByNameTypeAndQualifier(name, type, qualifier);
    return nextOrNull(iteratorFactory.create(records, zoneName, getAvailableRegions()));
  }

  /**
   * retrieve an iterator of a directional record.
   *
   * @param name      {@link ResourceRecordSet#name() name} of the rrset
   * @param type      {@link ResourceRecordSet#type() type} of the rrset
   * @param qualifier {@link ResourceRecordSet#qualifier() qualifier} of the rrset
   * @return null unless a resource record exists with the same {@code name}, {@code type}, and
   * {@code qualifier}
   * @throws IllegalArgumentException if the zone is not found.
   */
  private Iterator<DirectionalRecord> recordsByNameTypeAndQualifier(String name, String type,
                                                                                 String qualifier) {
    if (ResourceTypes.CNAME.name().equals(type)) {
      return filter(
          concat(
              recordsForNameTypeAndQualifier(name, ResourceTypes.A.name(), qualifier),
              recordsForNameTypeAndQualifier(name, ResourceTypes.AAAA.name(), qualifier)
          ), isCNAME
      );
    } else {
      return recordsForNameTypeAndQualifier(name, type, qualifier);
    }
  }

  /**
   * retrieve an iterator of a directional record.
   *
   * @param name      {@link ResourceRecordSet#name() name} of the rrset
   * @param type      {@link ResourceRecordSet#type() type} of the rrset
   * @param qualifier {@link ResourceRecordSet#qualifier() qualifier} of the rrset
   * @return null unless a resource record exists with the same {@code name}, {@code type}, and
   * {@code qualifier}
   * @throws IllegalArgumentException if the zone is not found.
   */
  private Iterator<DirectionalRecord> recordsForNameTypeAndQualifier(String name, String type,
                                                                                  String qualifier) {
    try {
      return RRSetUtil.getDirectionalRecordsByGroup(
              api.getDirectionalDNSRecordsForHost(zoneName, name, RRSetUtil.directionalRecordType(type)).rrSets(),
              qualifier).iterator();
    } catch (UltraDNSRestException e) {
      switch (e.code()) {
        case UltraDNSRestException.GROUP_NOT_FOUND:
        case UltraDNSRestException.DIRECTIONALPOOL_NOT_FOUND:
        case UltraDNSRestException.DATA_NOT_FOUND:
          return Collections.<DirectionalRecord>emptyList().iterator();
        default:
          throw e;
      }
    }
  }

  /**
   * This method will update Geo Records for a combination
   * of owner, resource type & group.
   * @param rrset contains the {@code rdata} elements ensure exist. If {@link
   *              ResourceRecordSet#ttl() ttl} is not present, zone default is used.
   */
  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(rrset.qualifier() != null, "no qualifier on: %s", rrset);
    checkArgument(IS_GEO.apply(rrset), "%s failed on: %s", IS_GEO, rrset);
    checkArgument(supportedTypes.contains(rrset.type()), "%s not a supported type for geo: %s",
            rrset.type(), supportedTypes);

    final String ownerName = rrset.name();
    final String type = rrset.type();

    final List<Map<String, Object>> recordsLeftToCreate = new ArrayList<Map<String, Object>>(rrset.records());
    final int ttlToApply = rrset.ttl() != null ? rrset.ttl() : DEFAULT_TTL;
    final String groupName = rrset.qualifier();
    final TreeSet<String> geoCodes = getTerritoryCodes(rrset.geo().regions());

    RRSetList rrSetList = null;
    try {
      rrSetList = api.getDirectionalDNSRecordsForHost(zoneName, ownerName, RRSetUtil.directionalRecordType(type));
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.DATA_NOT_FOUND) {
        throw e;
      }
    }

    if (rrSetList == null) {
      // Creating new pool with new records.
      LOGGER.debug("Creating new pool with new geo records and owner name " + ownerName + "and type " + type);
      final RRSet newRRSet = new RRSet();
      final List<String> newRData = new ArrayList<String>();
      final List<RDataInfo> newRDataInfoList = new ArrayList<RDataInfo>();

      for (Map<String, Object> record: recordsLeftToCreate) {
        newRData.add(buildRecordData(record));
        newRDataInfoList.add(createRDataInfo(type, ttlToApply, groupName, geoCodes));
      }
      newRRSet.setOwnerName(ownerName);
      newRRSet.setRdata(newRData);
      newRRSet.setProfile(new Profile(Constants.DIRECTIONAL_POOL_SCHEMA, "", newRDataInfoList));
      try {
        api.createResourceRecord(zoneName, RRSetUtil.directionalRecordType(type), ownerName, newRRSet);
      } catch (UltraDNSRestException e) {
          throw e;
      }
    } else {
      // Updating the existing pool records.
      LOGGER.debug("Updating the existing pool geo records with owner name " + ownerName + "and type " + type);
      final RRSet rs = rrSetList.getRrSets().get(0);
      final List<String> rdata = rs.getRdata();
      final Profile profile = rs.getProfile();
      final List<RDataInfo> rdataInfoList = profile.getRdataInfo();

      for (Map<String, Object> record: recordsLeftToCreate) {
        final String data = buildRecordData(record);
        int index = rdata.indexOf(data);
        if (index >= 0) {
          rdataInfoList.set(index, createRDataInfo(type, ttlToApply, groupName, geoCodes));
        } else {
          rdata.add(data);
          rdataInfoList.add(createRDataInfo(type, ttlToApply, groupName, geoCodes));
        }
      }
      rs.setRdata(rdata);
      profile.setRdataInfo(rdataInfoList);
      rs.setProfile(profile);
      try {
        api.updateDirectionalPool(zoneName, ownerName, type, rs);
      } catch (UltraDNSRestException e) {
        throw e;
      }
    }
  }

  /**
   * Build RData form from Map of record data.
   *
   * @param record Map of record data
   * @return RData
   */
  private String buildRecordData(Map<String, Object> record) {
    final List<String> dataList = new ArrayList<String>();
    for (Map.Entry<String, Object> r : record.entrySet()) {
      dataList.add(String.valueOf(r.getValue()));
    }
    return StringUtils.join(dataList, " ");
  }

  /**
   * Returns RDataInfo.
   *
   * @param type
   * @param ttlToApply
   * @param groupName
   * @param geoCodes
   * @return RDataInfo
   */
  private RDataInfo createRDataInfo(String type, int ttlToApply, String groupName, TreeSet<String> geoCodes) {
    final GeoInfo geoInfo = new GeoInfo();
    geoInfo.setName(groupName);
    geoInfo.setCodes(geoCodes);

    final RDataInfo rdi = new RDataInfo();
    rdi.setGeoInfo(geoInfo);
    rdi.setTtl(ttlToApply);
    rdi.setType(type);

    return rdi;
  }

  /**
   * Deletes directional pool records for the specified name,type and qualifier.
   *
   * @param name
   * @param type
   * @param qualifier
   */
  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
    LOGGER.debug("Deleting record(s) with owner name " + name + "type " + type +
            "qualifier " + qualifier);
    Iterator<DirectionalRecord> record = recordsByNameTypeAndQualifier(name, type, qualifier);
    while (record.hasNext()) {
      deleteDirectionalPoolRecord(record.next());
    }
  }

  /**
   * Returns iterator for resource record with specified name and directional type.
   *
   * @param name
   * @param dirType
   */
  private Iterator<ResourceRecordSet<?>> iteratorForDNameAndDirectionalType(String name,
                                                                            int dirType) {
    List<DirectionalRecord> list;
    try {
      list = RRSetUtil.buildDirectionalRecords(api
              .getDirectionalDNSRecordsForHost(zoneName, name, dirType)
              .rrSets());
    } catch (UltraDNSRestException e) {
      switch (e.code()) {
        case UltraDNSRestException.DIRECTIONALPOOL_NOT_FOUND:
        case UltraDNSRestException.DATA_NOT_FOUND:
          list = Collections.emptyList();
          break;
        default:
          throw e;
      }
    }
    return iteratorFactory.create(list.iterator(), zoneName, getAvailableRegions());
  }

  /**
   * Deletes a single directional pool record.
   *
   * @param record
   */
  private void deleteDirectionalPoolRecord(DirectionalRecord record) {
    if (record.getRdata() != null && !record.getRdata().isEmpty()) {
      if ("No Data Response".equals(record.getRdata().get(0))) {
        try {
          api.deleteDirectionalNoResponseRecord(zoneName, record.getName(), record.getType());
        } catch (UltraDNSRestException e) {
          if (e.code() != UltraDNSRestException.PATH_NOT_FOUND_TO_PATCH) {
            throw e;
          }
        }
      } else {
        int indexToDelete = -1;
        String rData = StringUtils.join(record.getRdata(), " ");
        try {
          List<RRSet> rrSets = api.getDirectionalDNSRecordsForHost(zoneName, record.getName(),
                  record.getTypeCode()).getRrSets();
          if (rrSets != null && !rrSets.isEmpty()) {
            RRSet rrSet = rrSets.get(0);
            if (rrSet != null && rrSet.getRdata() != null) {
              indexToDelete = rrSet.getRdata().indexOf(rData);
            }
          }
        } catch (UltraDNSRestException e) {
          if (e.code() != UltraDNSRestException.DATA_NOT_FOUND) {
            throw e;
          }
        }
        if (indexToDelete >= 0) {
          try {
            api.deleteDirectionalPoolRecord(zoneName, record.getName(), record.getType(), indexToDelete);
          } catch (UltraDNSRestException e) {
            if (e.code() != UltraDNSRestException.PATH_NOT_FOUND_TO_PATCH) {
              throw e;
            }
          }
        }
      }
    }
  }

  /**
   * Converts Territory/Regions to it's GEO code.
   * @param regionToTerritories
   * @return Set of GEO codes.
   */
  private TreeSet<String> getTerritoryCodes(Map<String, Collection<String>> regionToTerritories) {
    final TreeSet<String> territoryCodes = new TreeSet<String>();
    final Set<Region> allRegions = new TreeSet<Region>();
    final Set<String> allRegionNames = new TreeSet<String>();

    for (Map.Entry<Region, Collection<Region>> entry : getAvailableRegions().entrySet()) {
      allRegions.add(entry.getKey());
      allRegions.addAll(entry.getValue());
    }

    for (Map.Entry<String, Collection<String>> regionToTerritory : regionToTerritories.entrySet()) {
      allRegionNames.addAll(regionToTerritory.getValue());
    }

    Iterator<String> regionNamesIterator = allRegionNames.iterator();
    while (regionNamesIterator.hasNext()) {
      String regionName = regionNamesIterator.next();
      Iterator<Region> regionsIterator = allRegions.iterator();
      while (regionsIterator.hasNext()) {
        Region region = regionsIterator.next();
        if (regionName.equals(region.getName())) {
          territoryCodes.add(region.getEffectiveCodeForGeo());
          break;
        }
      }
    }
    return territoryCodes;
  }

  public static final class Factory implements GeoResourceRecordSetApi.Factory {

    private final Collection<String> supportedTypes;
    private final Lazy<Map<Region, Collection<Region>>> regions;
    private final UltraDNSRest api;
    private final GroupGeoRecordByNameTypeCustomIterator.Factory iteratorFactory;

    @Inject
    Factory(Provider provider, @Named("geo") Lazy<Map<Region, Collection<Region>>> regions,
            UltraDNSRest api,
            GroupGeoRecordByNameTypeCustomIterator.Factory iteratorFactory) {
      this.supportedTypes = provider.profileToRecordTypes().get("geo");
      this.regions = regions;
      this.api = api;
      this.iteratorFactory = iteratorFactory;
    }

    @Override
    public GeoResourceRecordSetApi create(String name) {
      checkNotNull(name, "name was null");
      // Eager fetch of regions to determine if directional records are supported or not.
      try {
        regions.get();
      } catch (UltraDNSRestException e) {
        if (e.code() == UltraDNSRestException.DIRECTIONAL_NOT_ENABLED) {
          return null;
        }
        throw e;
      }
      return new UltraDNSRestGeoResourceRecordSetApi(supportedTypes, regions, api, iteratorFactory, name);
    }
  }
}
