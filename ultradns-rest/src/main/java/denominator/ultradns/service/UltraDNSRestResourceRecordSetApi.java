package denominator.ultradns.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Arrays;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.ultradns.service.integration.UltraDNSRest;
import denominator.ultradns.exception.UltraDNSRestException;
import denominator.ultradns.iterator.GroupByRecordNameAndTypeCustomIterator;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.Record;
import denominator.ultradns.util.RRSetUtil;
import denominator.ultradns.model.RRSetList;
import denominator.ultradns.model.Profile;
import static denominator.common.Util.flatten;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.nextOrNull;
import static denominator.ultradns.exception.UltraDNSRestException.processUltraDnsException;

import denominator.ResourceTypeToValue.ResourceTypes;
import org.apache.log4j.Logger;
import denominator.ultradns.util.Constants;

public final class UltraDNSRestResourceRecordSetApi implements denominator.ResourceRecordSetApi {

  private static final int DEFAULT_TTL = 300;
  private final UltraDNSRest api;
  private final String zoneName;
  private final UltraDNSRestRoundRobinPoolApi roundRobinPoolApi;

  UltraDNSRestResourceRecordSetApi(UltraDNSRest api, String zoneName,
                                   UltraDNSRestRoundRobinPoolApi roundRobinPoolApi) {
    this.api = api;
    this.zoneName = zoneName;
    this.roundRobinPoolApi = roundRobinPoolApi;
  }

  private static final Logger LOGGER = Logger.getLogger(UltraDNSRestResourceRecordSetApi.class);

  /**
   * Iterates across all record sets in the zone. Implementations are lazy when possible.
   *
   * @return iterator which is lazy where possible
   * @throws IllegalArgumentException if the zone is not found.
   */
  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    Iterator<Record> orderedRecords = RRSetUtil.buildRecords(api
            .getResourceRecordsOfZone(zoneName)
            .rrSets())
            .iterator();
    return new GroupByRecordNameAndTypeCustomIterator(orderedRecords);
  }

  /**
   * a listing of all resource record sets which have the specified name.
   *
   * @return iterator which is lazy where possible, empty if there are no records with that name.
   * @throws IllegalArgumentException if the zone is not found.
   */
  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    checkNotNull(name, "name");
    Iterator<Record> ordered = RRSetUtil.buildRecords(api
            .getResourceRecordsOfDNameByType(zoneName, name, ResourceTypes.ALL.code())
            .rrSets())
            .iterator();
    return new GroupByRecordNameAndTypeCustomIterator(ordered);
  }

  /**
   * retrieve a resource record set by name, type.
   *
   * @param name      {@link ResourceRecordSet#name() name} of the rrset
   * @param type      {@link ResourceRecordSet#type() type} of the rrset
   * @return null unless a resource record exists with the same {@code name}, {@code type}, and
   * {@code qualifier}
   * @throws IllegalArgumentException if the zone is not found.
   */
  @Override
  public ResourceRecordSet<?> getByNameAndType(String name, String type) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");
    Iterator<Record> orderedRecords = recordsByNameAndType(name, type)
            .iterator();
    return nextOrNull(new GroupByRecordNameAndTypeCustomIterator(
            orderedRecords));
  }

  /**
   * Retrieve a list of a records.
   *
   * @param name      {@link ResourceRecordSet#name() name} of the rrset
   * @param type      {@link ResourceRecordSet#type() type} of the rrset
   * @return null unless a resource record exists with the same {@code name}, {@code type}
   * @throws IllegalArgumentException if the zone is not found.
   */
  private List<Record> recordsByNameAndType(String name, String type) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");
    int typeValue = checkNotNull(lookup(type), "typeValue for %s", type);
    List<Record> records = null;
    try {
      records = RRSetUtil.buildRecords(api
              .getResourceRecordsOfDNameByType(zoneName, name, typeValue)
              .rrSets());
    } catch (UltraDNSRestException e) {
        processUltraDnsException(e,
                new HashSet<Integer>(Arrays.asList(
                        UltraDNSRestException.DATA_NOT_FOUND,
                        UltraDNSRestException.RESOURCE_RECORD_POOL_NOT_FOUND
                )));
      records = new ArrayList<Record>();
    }
    return records;
  }

  /**
   * This method will update Records for a combination
   * of owner, resource type and group.
   * @param rrset If {@link
   *              ResourceRecordSet#ttl() ttl} is not present, zone default is used.
   */
  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(!rrset.records().isEmpty(), "rrset was empty %s", rrset);
    int ttlToApply = rrset.ttl() != null ? rrset.ttl() : DEFAULT_TTL;
    int typeCode = lookup(rrset.type());

    RRSetList rrSetList = getRrSetList(rrset.name(), typeCode);

    if (rrSetList == null || rrSetList.getRrSets() == null) {
      add(rrset, ttlToApply, typeCode);

    } else {
      replace(rrSetList, rrset, ttlToApply, typeCode);
    }
  }

  /**
   * Adds resource record(s) given resource record set, type, ttlToApply.
   *
   * @param rrset resource record set
   * @param ttlToApply ttl for the record
   * @param typeCode resource record type code
   * @throws IllegalArgumentException if the zone is not found.
   */
  private void add(ResourceRecordSet<?> rrset, int ttlToApply, int typeCode) {
    LOGGER.debug("Adding record(s) to the zone:" + zoneName + " domain name:" + rrset.name()
            + " type:" + typeCode);
    RRSet rrSetNew = new RRSet();
    rrSetNew.setOwnerName(rrset.name());
    rrSetNew.setRrtype(rrset.type());
    rrSetNew.setTtl(ttlToApply);
    // Extract the rdata from the resource record set passed in as
    // the parameter to this method.
    setData(rrSetNew, rrset);

    if (roundRobinPoolApi.isPoolType(rrset.type())) {
      Profile profile = new Profile();
      profile.setContext(Constants.RD_POOL_SCHEMA);
      profile.setOrder(Constants.ROUND_ROBIN);
      rrSetNew.setProfile(profile);
    }
    // Send a POST request.
    api.createResourceRecord(zoneName, typeCode, rrset.name(), rrSetNew);
  }

  /**
   * Updates resource record(s) given rrSetList, resource record set, type, ttlToApply.
   *
   * @param rrSetList resource record set list RRSetList
   * @param rrset resource record set ResourceRecordSet
   * @param ttlToApply ttl for the record
   * @param typeCode resource record type code
   * @throws IllegalArgumentException if the zone is not found.
   */
  private void replace(RRSetList rrSetList, ResourceRecordSet<?> rrset , int ttlToApply, int typeCode) {
    LOGGER.debug("Updating resource record(s) of the zone:" + zoneName + " domain name:" + rrset.name()
            + " type:" + typeCode);
    // Update the resource record set.
    List<RRSet> rrSetsExisting = rrSetList.rrSets();
    RRSet rrSetExisting = rrSetsExisting.remove(0);
    // Set the TTL value in the resource record set.
    rrSetExisting.setTtl(ttlToApply);
    setData(rrSetExisting, rrset);
    // Send a PUT request.
    api.updateResourceRecord(zoneName, typeCode, rrset.name(), rrSetExisting);
  }

  /**
   * Updates the passed rdatas to the existing RRSet.
   *
   * @param rrSet RRSet
   * @param rrset ResourceRecordSet
   * @throws IllegalArgumentException if the zone is not found.
   */
  private void setData(RRSet rrSet, ResourceRecordSet<?> rrset) {
    List<String> rdatas = new ArrayList<String>();
    // Extract the rdata from the resource record set passed in as
    // the parameter to this method and put them into rdatas.
    List<Map<String, Object>> toCreate = new ArrayList<Map<String, Object>>(rrset.records());
    for (Iterator<Map<String, Object>> shouldCreate = toCreate.iterator(); shouldCreate.hasNext();) {
      String rdata = flatten(shouldCreate.next());
      rdatas.add(rdata);
    }
    rrSet.setRdata(rdatas);
  }

  /**
   * Returns RRSetList given owner name and type.
   *
   * @param dName the owner name
   * @param type resource record type code
   * @throws IllegalArgumentException if the zone is not found.
   */
  private RRSetList getRrSetList(String dName, int type) {
    RRSetList rrSetList;
    try {
      rrSetList = api.getResourceRecordsOfDNameByType(zoneName, dName, type);
    } catch (UltraDNSRestException e) {
      processUltraDnsException(e,
              new HashSet<Integer>(Arrays.asList(
                      UltraDNSRestException.DATA_NOT_FOUND,
                      UltraDNSRestException.RESOURCE_RECORD_POOL_NOT_FOUND
              )));
      rrSetList = null;
    }
    return rrSetList;
  }

  /**
   * Deletes records for the specified name and type.
   *
   * @param name owner name
   * @param type resource record type code
   */
  @Override
  public void deleteByNameAndType(String name, String type) {
    LOGGER.debug("Deleting resource record(s) for the zone:" + zoneName + " domain name:" + name + " type:" + type);
    int intType = lookup(type);
    try {
      api.deleteResourceRecordByNameType(zoneName, intType, name);
    } catch (UltraDNSRestException e) {
      processUltraDnsException(e, UltraDNSRestException.RESOURCE_RECORD_POOL_NOT_FOUND);
    }
  }


  public static final class Factory implements denominator.ResourceRecordSetApi.Factory {

    private final UltraDNSRest api;

    @Inject
    Factory(UltraDNSRest api) {
      this.api = api;
    }

    @Override
    public ResourceRecordSetApi create(String name) {
      return new UltraDNSRestResourceRecordSetApi(api, name, new UltraDNSRestRoundRobinPoolApi(api, name));
    }
  }
}
