package denominator.ultradns.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.ultradns.service.integration.UltraDNSRest;
import denominator.ultradns.exception.UltraDNSRestException;
import denominator.ultradns.iterator.GroupByRecordNameAndTypeCustomIterator;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.Record;
import denominator.ultradns.util.RRSetUtil;
import org.apache.commons.lang.StringUtils;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.nextOrNull;
import static denominator.common.Util.toMap;
import denominator.ResourceTypeToValue.ResourceTypes;
import org.apache.log4j.Logger;

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
      if (e.code() == UltraDNSRestException.DATA_NOT_FOUND ||
          e.code() == UltraDNSRestException.RESOURCE_RECORD_POOL_NOT_FOUND) {
        records = new ArrayList<Record>();
      } else {
        throw e;
      }
    }
    return records;
  }

  /**
   * This method will update Records for a combination
   * of owner, resource type & group.
   * @param rrset contains the {@code rdata} elements ensure exist. If {@link
   *              ResourceRecordSet#ttl() ttl} is not present, zone default is used.
   */
  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(!rrset.records().isEmpty(), "rrset was empty %s", rrset);
    int ttlToApply = rrset.ttl() != null ? rrset.ttl() : DEFAULT_TTL;

    LOGGER.debug("Retrieving the list of records to update ");
    List<Record> toUpdate = recordsByNameAndType(rrset.name(), rrset.type());

    List<Map<String, Object>> toCreate = new ArrayList<Map<String, Object>>(rrset.records());

    for (Iterator<Record> shouldUpdate = toUpdate.iterator(); shouldUpdate.hasNext();) {
      Record record = shouldUpdate.next();
      Map<String, Object> rdata = toMap(rrset.type(), record.getRdata());
      if (toCreate.contains(rdata)) {
        toCreate.remove(rdata);
        if (ttlToApply == record.getTtl()) {
          shouldUpdate.remove();
        }
      } else {
        shouldUpdate.remove();
        remove(rrset.name(), rrset.type(), record);
      }
    }
    if (!toUpdate.isEmpty()) {
      update(rrset.name(), rrset.type(), ttlToApply, toUpdate);
    }
    if (!toCreate.isEmpty()) {
      create(rrset.name(), rrset.type(), ttlToApply, toCreate);
    }
  }

  /**
   * Update resource record(s) given name, type, ttlToApply, list of records.
   *
   * @param name      {@link ResourceRecordSet#name() name} of the rrset
   * @param type      {@link ResourceRecordSet#type() type} of the rrset
   * @param ttlToApply      {@link ResourceRecordSet#ttl() ttl} of the rrset
   * @param toUpdate list of records
   * @throws IllegalArgumentException if the zone is not found.
   */
  private void update(String name, String type, int ttlToApply, List<Record> toUpdate) {
    LOGGER.debug("Updating the record(s) with owner name " + name + "type " + type + "ttl " + ttlToApply);
    for (Record record : toUpdate) {
      record.setTtl(ttlToApply);
      api.partialUpdateResourceRecord(zoneName, record.getTypeCode(), name, record.buildRRSet());
    }
  }

  /**
   * Create resource record(s) given name, type, ttlToApply, list of records.
   *
   * @param name      {@link ResourceRecordSet#name() name} of the rrset
   * @param type      {@link ResourceRecordSet#type() type} of the rrset
   * @param ttl      {@link ResourceRecordSet#ttl() ttl} of the rrset
   * @param rdatas list of map of records
   * @throws IllegalArgumentException if the zone is not found.
   */
  private void create(String name, String type, int ttl, List<Map<String, Object>> rdatas) {
    LOGGER.debug("Creating the record(s) with owner name " + name + "type " + type + "ttl " + ttl);
    if (roundRobinPoolApi.isPoolType(type)) {
      roundRobinPoolApi.add(name, type, ttl, rdatas);
    } else {
      Record record = new Record();
      record.setName(name);
      record.setTypeCode(lookup(type));
      record.setTtl(ttl);

      for (Map<String, Object> rdata : rdatas) {
        for (Object rdatum : rdata.values()) {
          record.getRdata().add(rdatum.toString());
        }
        api.createResourceRecord(zoneName, record.getTypeCode(), name, record.buildRRSet());
      }
    }
  }

  /**
   * Deletes records for the specified name and type.
   *
   * @param name
   * @param type
   */
  @Override
  public void deleteByNameAndType(String name, String type) {
    for (Record record : recordsByNameAndType(name, type)) {
      remove(name, type, record);
    }
  }

  /**
   * Deletes record for the specified name and type.
   *
   * @param name
   * @param type
   * @param record
   */
  private void remove(String name, String type, Record record) {
    String rData = "";
    int intType = lookup(type);

    if (record.getRdata() != null && !record.getRdata().isEmpty()) {
      rData = StringUtils.join(record.getRdata(), " ");
    }

    List<RRSet> rrSets = null;
    try {
      rrSets = api.getResourceRecordsOfDNameByType(zoneName, name, intType).getRrSets();
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.DATA_NOT_FOUND &&
          e.code() != UltraDNSRestException.RESOURCE_RECORD_POOL_NOT_FOUND) {
        throw e;
      }
    }

    RRSet rrSet = null;
    if (rrSets != null && !rrSets.isEmpty()) {
      rrSet = rrSets.get(0);
      if (rrSet != null && rrSet.getRdata() != null) {
        int indexToDelete = rrSet.getRdata().indexOf(rData);
        if (indexToDelete >= 0) {
          try {
            api.deleteResourceRecord(zoneName, intType, name, indexToDelete);
          } catch (UltraDNSRestException e) {
            if (e.code() != UltraDNSRestException.PATH_NOT_FOUND_TO_PATCH) {
              throw e;
            }
          }
        }
      }
    }

    // If the last record in the pool is deleted successfully or if there are
    // no records in the pool and if it is a pool then delete the pool itself.
    if (rrSet != null && rrSet.getRdata().size() <= 1 && roundRobinPoolApi.isPoolType(type)) {
      roundRobinPoolApi.deletePool(name, type);
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
