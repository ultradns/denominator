package denominator.ultradns;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.gson.Gson;
import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.Record;
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
    // this will list all basic or RR pool records.
    // In Progress - Arghya 31/01/17
    Iterator<Record> orderedRecords = api
            .getResourceRecordsOfZone(zoneName)
            .buildRecords()
            .iterator();
    return new GroupByRecordNameAndTypeCustomIterator(orderedRecords);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    checkNotNull(name, "name");
    final int ANY_RR_TYPE = 255;
    Iterator<Record> ordered = api
            .getResourceRecordsOfDNameByType(zoneName, name, ANY_RR_TYPE)
            .buildRecords()
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
      records = api
              .getResourceRecordsOfDNameByType(zoneName, name, typeValue)
              .buildRecords();
    } catch (UltraDNSRestException e) {
      if (e.code() == UltraDNSRestException.DATA_NOT_FOUND) {
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
      Map<String, Object> rdata = toMap(rrset.type(), record.rdata);
      if (toCreate.contains(rdata)) {
        toCreate.remove(rdata);
        if (ttlToApply == record.ttl) {
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
    if (roundRobinPoolApi.isPoolType(type)) {
      // RRSet rrSet = roundRobinPoolApi.getPoolByNameAndType(name, type);
      Gson gson = new Gson();
      for (Record record : toUpdate) {
        // api.updateRecordOfRRPool(record.id, lbPoolId, record.rdata.get(0), ttlToApply);
        api.updateRecordOfRRPool(zoneName, lookup(type), name, ttlToApply, gson.toJson(record.getRdata()), gson.toJson(record.getProfile()));
        // api.updateRecordOfRRPool(zoneName, lookup(type), name, ttlToApply, record.getName());
      }
    } else {
      for (Record record : toUpdate) {
        record.ttl = ttlToApply;
        api.partialUpdateResourceRecord(zoneName, record.getTypeCode(), name, record.buildRRSet());
      }
    }
  }

  private void create(String name, String type, int ttl, List<Map<String, Object>> rdatas) {
    if (roundRobinPoolApi.isPoolType(type)) {
      roundRobinPoolApi.add(name, type, ttl, rdatas);
    } else {
      Record record = new Record();
      record.name = name;
      record.typeCode = lookup(type);
      record.ttl = ttl;

      for (Map<String, Object> rdata : rdatas) {
        for (Object rdatum : rdata.values()) {
          record.rdata.add(rdatum.toString());
        }
        api.createResourceRecord(zoneName, record.getTypeCode(), name, record.buildRRSet());
      }
    }
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    try {
      api.deleteResourceRecordByNameType(zoneName, lookup(type), name);
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.RESOURCE_RECORD_NOT_FOUND) {
        throw e;
      }
    }
    if (roundRobinPoolApi.isPoolType(type)) {
      roundRobinPoolApi.deletePool(name, type);
    }
  }

  private void remove(String name, String type, Record record) {
    int indexToDelete = -1;
    String rData = "";
    int intType = lookup(type);

    if (record.getRdata() != null && !record.getRdata().isEmpty()) {
      rData = StringUtils.join(record.getRdata(), " ");
    }

    try {
      List<RRSet> rrSets = api.getResourceRecordsOfDNameByType(zoneName, name, intType).getRrSets();
      if (rrSets != null && !rrSets.isEmpty()) {
        RRSet rrSet = rrSets.get(0);
        if (rrSet != null & rrSet.getRdata() != null) {
          indexToDelete = rrSet.getRdata().indexOf(rData);
        }
      }
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.DATA_NOT_FOUND) {
        throw e;
      }
    }

    if (indexToDelete >= 0 ) {
      try {
        api.deleteResourceRecord(zoneName, intType, name, indexToDelete);
      } catch (UltraDNSRestException e) {
        if (e.code() != UltraDNSRestException.PATH_NOT_FOUND_TO_PATCH) {
          throw e;
        }
      }
    }
    if (roundRobinPoolApi.isPoolType(type)) {
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
