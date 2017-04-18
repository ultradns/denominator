package denominator.ultradns;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.gson.Gson;
import dagger.Lazy;
import denominator.Provider;
import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.RDataInfo;
import denominator.ultradns.model.GeoInfo;
import denominator.ultradns.model.Profile;
import denominator.ultradns.model.DirectionalRecord;
import denominator.ultradns.model.DirectionalGroup;
import denominator.ultradns.util.RRSetUtil;
import denominator.ResourceTypeToValue.ResourceTypes;
import org.apache.commons.lang.StringUtils;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.concat;
import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.common.Util.toMap;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;

final class UltraDNSRestGeoResourceRecordSetApi implements GeoResourceRecordSetApi {
  private static final Filter<ResourceRecordSet<?>> IS_GEO = new Filter<ResourceRecordSet<?>>() {
    @Override
    public boolean apply(ResourceRecordSet<?> in) {
      return in != null && in.geo() != null;
    }
  };
  private static final int DEFAULT_TTL = 300;

  private final Collection<String> supportedTypes;
  private final Lazy<Map<String, Collection<String>>> regions;
  private final UltraDNSRest api;
  private final GroupGeoRecordByNameTypeCustomIterator.Factory iteratorFactory;
  private final String zoneName;
  private final Filter<DirectionalRecord> isCNAME = new Filter<DirectionalRecord>() {
    @Override
    public boolean apply(DirectionalRecord input) {
      return ResourceTypes.CNAME.name().equals(input.getType());
    }
  };
  private final UltraDNSRestGeoSupport ultraDNSRestGeoSupport;

  UltraDNSRestGeoResourceRecordSetApi(Collection<String> supportedTypes,
                                      Lazy<Map<String, Collection<String>>> regions,
                                      UltraDNSRest api,
                                      GroupGeoRecordByNameTypeCustomIterator.Factory iteratorFactory,
                                      String zoneName,
                                      UltraDNSRestGeoSupport ultraDNSRestGeoSupport
                                      ) {
    this.supportedTypes = supportedTypes;
    this.regions = regions;
    this.api = api;
    this.iteratorFactory = iteratorFactory;
    this.zoneName = zoneName;
    this.ultraDNSRestGeoSupport = ultraDNSRestGeoSupport;
  }

  @Override
  public Map<String, Collection<String>> supportedRegions() {
    return regions.get();
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    List<Iterable<ResourceRecordSet<?>>> eachPool = new ArrayList<Iterable<ResourceRecordSet<?>>>();
    final Map<String, Integer> nameAndType = RRSetUtil.getNameAndType(api
            .getDirectionalPoolsOfZone(zoneName)
            .rrSets());
    for (final String poolName : nameAndType.keySet()) {
      eachPool.add(new Iterable<ResourceRecordSet<?>>() {
        public Iterator<ResourceRecordSet<?>> iterator() {
          return iteratorForDNameAndDirectionalType(poolName, nameAndType.get(poolName));
        }
      });
    }
    return concat(eachPool);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return iteratorForDNameAndDirectionalType(checkNotNull(name, "description"), ResourceTypes.ALL.code());
  }

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
    return nextOrNull(iteratorFactory.create(records, zoneName));
  }

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
          return Collections.<DirectionalRecord>emptyList().iterator();
        default:
          throw e;
      }
    }
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(rrset.qualifier() != null, "no qualifier on: %s", rrset);
    checkArgument(IS_GEO.apply(rrset), "%s failed on: %s", IS_GEO, rrset);
    checkArgument(supportedTypes.contains(rrset.type()), "%s not a supported type for geo: %s",
            rrset.type(), supportedTypes);

    int ttlToApply = rrset.ttl() != null ? rrset.ttl() : DEFAULT_TTL;
    String group = rrset.qualifier();
    Map<String, Collection<String>> regions1 = rrset.geo().regions();

    DirectionalGroup directionalGroup = new DirectionalGroup();
    directionalGroup.setName(group);
    directionalGroup.setRegionToTerritories(regions1);

    List<Map<String, Object>> recordsLeftToCreate = new ArrayList<Map<String, Object>>(rrset.records());
    Iterator<DirectionalRecord> iterator = recordsByNameTypeAndQualifier(rrset.name(), rrset.type(), group);

    while (iterator.hasNext()) {
      DirectionalRecord record = iterator.next();
      Map<String, Object> rdata = toMap(record.getType(), record.getRdata());

      if (recordsLeftToCreate.contains(rdata)) {
        recordsLeftToCreate.remove(rdata);
        boolean shouldUpdate = false;
        if (ttlToApply != record.getTtl()) {
          record.setTtl(ttlToApply);
          shouldUpdate = true;
        } else {
          directionalGroup = ultraDNSRestGeoSupport.getDirectionalDNSGroupByName(zoneName, record.getName(),
                  RRSetUtil.directionalRecordType(record.getType()), record.getGeoGroupName());
          if (!regions1.equals(directionalGroup.getRegionToTerritories())) {
            directionalGroup.setRegionToTerritories(regions1);
            shouldUpdate = true;
          }
        }
        if (shouldUpdate) {
            updateDirectionalPoolRecord(zoneName, record, directionalGroup);
        }
      } else {
        deleteDirectionalPoolRecord(record);
      }
    }

    if (!recordsLeftToCreate.isEmpty()) {
      String poolName = rrset.name();
      try {
        String type = rrset.type();
        if (ResourceTypes.CNAME.name().equals(type)) {
          type = ResourceTypes.A.name();
        }
        api.addDirectionalPool(zoneName, poolName, type);
      } catch (UltraDNSRestException e) {
        if (e.code() != UltraDNSRestException.POOL_ALREADY_EXISTS) {
          throw e;
        }
      }

      DirectionalRecord record = new DirectionalRecord();
      record.setType(rrset.type());
      record.setTtl(ttlToApply);

      for (Map<String, Object> rdata : recordsLeftToCreate) {
        for (Object rDatum : rdata.values()) {
          record.getRdata().add(rDatum.toString());
        }
        addDirectionalPoolRecord(zoneName, poolName, record, directionalGroup);
      }
    }
  }

  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
    Iterator<DirectionalRecord> record = recordsByNameTypeAndQualifier(name, type, qualifier);
    while (record.hasNext()) {
      deleteDirectionalPoolRecord(record.next());
    }
  }

  private Iterator<ResourceRecordSet<?>> iteratorForDNameAndDirectionalType(String name,
                                                                            int dirType) {
    List<DirectionalRecord> list;
    try {
      list = RRSetUtil.buildDirectionalRecords(api
              .getDirectionalDNSRecordsForHost(zoneName, name, dirType)
              .rrSets());
    } catch (UltraDNSRestException e) {
      if (e.code() == UltraDNSRestException.DIRECTIONALPOOL_NOT_FOUND) {
        list = Collections.emptyList();
      } else {
        throw e;
      }
    }
    return iteratorFactory.create(list.iterator(), zoneName);
  }

  private void addDirectionalPoolRecord(String zoneName1, String hostName,
                                        DirectionalRecord record, DirectionalGroup directionalGroup) {

    List<String> rdata = record.getRdata();

    GeoInfo geoInfo = new GeoInfo();
    geoInfo.setCodes(ultraDNSRestGeoSupport.getTerritoryCodes(directionalGroup));
    geoInfo.setName(directionalGroup.getName());

    RDataInfo rDataInfo = new RDataInfo();
    rDataInfo.setGeoInfo(geoInfo);
    List<RDataInfo> rDataInfos = Arrays.asList(rDataInfo);

    Profile profile = new Profile();
    profile.setRdataInfo(rDataInfos);

    RRSet rrSet = new RRSet();
    rrSet.setProfile(profile);
    rrSet.setRdata(rdata);

    try {
      api.addDirectionalPoolRecord(zoneName1, hostName, record.getType(), rrSet);
    } catch (UltraDNSRestException e) {
      throw e;
    }
  }

  public void updateDirectionalPoolRecord(String zoneName2, DirectionalRecord record,
                                          DirectionalGroup directionalGroup) {
    int indexToUpdate = -1;
    String rData = "";
    int intType = lookup(record.getType());
    List<RRSet> rrSets = new ArrayList<RRSet>();

    if (record.getRdata() != null && !record.getRdata().isEmpty()) {
      rData = StringUtils.join(record.getRdata(), " ");
    }

    try {
      rrSets = api.getDirectionalDNSRecordsForHost(zoneName2, record.getName(), intType).getRrSets();
      if (rrSets != null && !rrSets.isEmpty()) {
        RRSet rrSet = rrSets.get(0);
        if (rrSet != null && rrSet.getRdata() != null) {
          indexToUpdate = rrSet.getRdata().indexOf(rData);
        }
      }
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.DATA_NOT_FOUND) {
        throw e;
      }
    }

    if (indexToUpdate >= 0) {
      RDataInfo rDataInfo = rrSets.get(0).getProfile().getRdataInfo().get(indexToUpdate);
      GeoInfo geoInfo = rDataInfo.getGeoInfo();
      geoInfo.setCodes(ultraDNSRestGeoSupport.getTerritoryCodes(directionalGroup));
      geoInfo.setName(directionalGroup.getName());
      rDataInfo.setGeoInfo(geoInfo);
      rDataInfo.setTtl(record.getTtl());

      try {
        Gson gson = new Gson();
        api.updateDirectionalPoolRecord(zoneName2, record.getName(), record.getType(), gson.toJson(rDataInfo),
                indexToUpdate);
      } catch (UltraDNSRestException e) {
        if (e.code() != UltraDNSRestException.PATH_NOT_FOUND_TO_PATCH) {
          throw e;
        }
      }
    }
  }

  private void deleteDirectionalPoolRecord(DirectionalRecord record) {
    if (record.getRdata() != null && !record.getRdata().isEmpty()) {
      if (record.getRdata().get(0).equals("No Data Response")) {
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

  static final class Factory implements GeoResourceRecordSetApi.Factory {

    private final Collection<String> supportedTypes;
    private final Lazy<Map<String, Collection<String>>> regions;
    private final UltraDNSRest api;
    private final GroupGeoRecordByNameTypeCustomIterator.Factory iteratorFactory;

    @Inject
    Factory(Provider provider, @Named("geo") Lazy<Map<String, Collection<String>>> regions,
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
      return new UltraDNSRestGeoResourceRecordSetApi(supportedTypes,
              regions, api, iteratorFactory, name, new UltraDNSRestGeoSupport(api));
    }
  }
}
