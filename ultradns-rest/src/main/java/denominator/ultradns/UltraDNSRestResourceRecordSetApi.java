package denominator.ultradns;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.Record;
import denominator.ultradns.util.RRSetUtil;
import org.apache.commons.lang.StringUtils;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.nextOrNull;
import static denominator.common.Util.toMap;

final class UltraDNSRestResourceRecordSetApi implements denominator.ResourceRecordSetApi {

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

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    Iterator<Record> orderedRecords = RRSetUtil.buildRecords(api
            .getResourceRecordsOfZone(zoneName)
            .rrSets())
            .iterator();
    return new GroupByRecordNameAndTypeCustomIterator(orderedRecords);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    checkNotNull(name, "name");
    final int anyRrType = 255;
    Iterator<Record> ordered = RRSetUtil.buildRecords(api
            .getResourceRecordsOfDNameByType(zoneName, name, anyRrType)
            .rrSets())
            .iterator();
    return new GroupByRecordNameAndTypeCustomIterator(ordered);
  }

  @Override
  public ResourceRecordSet<?> getByNameAndType(String name, String type) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");
    Iterator<Record> orderedRecords = recordsByNameAndType(name, type)
            .iterator();
    return nextOrNull(new GroupByRecordNameAndTypeCustomIterator(
            orderedRecords));
  }

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

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(!rrset.records().isEmpty(), "rrset was empty %s", rrset);
    int ttlToApply = rrset.ttl() != null ? rrset.ttl() : DEFAULT_TTL;

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

  private void update(String name, String type, int ttlToApply, List<Record> toUpdate) {
    for (Record record : toUpdate) {
      record.setTtl(ttlToApply);
      api.partialUpdateResourceRecord(zoneName, record.getTypeCode(), name, record.buildRRSet());
    }
  }

  private void create(String name, String type, int ttl, List<Map<String, Object>> rdatas) {
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

  @Override
  public void deleteByNameAndType(String name, String type) {
    for (Record record : recordsByNameAndType(name, type)) {
      remove(name, type, record);
    }
  }

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

  static final class Factory implements denominator.ResourceRecordSetApi.Factory {

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
