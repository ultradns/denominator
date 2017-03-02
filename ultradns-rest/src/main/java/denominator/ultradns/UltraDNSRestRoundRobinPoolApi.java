package denominator.ultradns;

import java.util.List;
import java.util.Map;

import denominator.ultradns.UltraDNSRest.NameAndType;
import denominator.ultradns.model.RRSet;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Preconditions.checkState;

class UltraDNSRestRoundRobinPoolApi {

  private final UltraDNSRest api;
  private final String zoneName;

  UltraDNSRestRoundRobinPoolApi(UltraDNSRest api, String zoneName) {
    this.api = api;
    this.zoneName = zoneName;
  }

  boolean isPoolType(String type) {
    return type.equals("A") || type.equals("AAAA");
  }

  void add(String name, String type, int ttl, List<Map<String, Object>> rdatas) {
    checkState(isPoolType(type), "%s not A or AAAA type", type);
    // String poolId = reuseOrCreatePoolForNameAndType(name, type);
    reuseOrCreatePoolForNameAndType(name, type);
    final int typeCode = lookup(type);
    for (Map<String, Object> rdata : rdatas) {
      String address = rdata.get("address").toString();
      api.addRecordToRRPool(typeCode, ttl, address, name, zoneName);
    }
  }

  private void reuseOrCreatePoolForNameAndType(String name, String type) {
    try {
      api.addRRLBPool(zoneName, name, lookup(type));
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.POOL_ALREADY_EXISTS) {
        throw e;
      }
    }
  }

  RRSet getPoolByNameAndType(String name, String type) {
    NameAndType nameAndType = new NameAndType();
    nameAndType.name = name;
    nameAndType.type = type;
    return api.getLoadBalancingPoolsByZone(zoneName, lookup(type)).rrSetByNameAndType(name, type);
  }

  void deletePool(String name, String type) {
    NameAndType nameAndType = new NameAndType();
    nameAndType.name = checkNotNull(name, "pool name was null");
    nameAndType.type = checkNotNull(type, "pool record type was null");
    final int typeCode = lookup(type);
    RRSet poolId = api.getLoadBalancingPoolsByZone(zoneName, typeCode).rrSetByNameAndType(name, type);
    if (poolId != null) {
      if (poolId.recordsFromRdata().isEmpty()) {
        try {
          api.deleteLBPool(zoneName, typeCode, name);
        } catch (UltraDNSRestException e) {
          switch (e.code()) {
            // lost race
            case UltraDNSRestException.POOL_NOT_FOUND:
            case UltraDNSRestException.RESOURCE_RECORD_NOT_FOUND:
              return;
          }
          throw e;
        }
      }
    }
  }
}
