package denominator.ultradns;

import java.util.List;
import java.util.Map;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Preconditions.checkState;

import denominator.ResourceTypeToValue.ResourceTypes;

class UltraDNSRestRoundRobinPoolApi {

  private final UltraDNSRest api;
  private final String zoneName;

  UltraDNSRestRoundRobinPoolApi(UltraDNSRest api, String zoneName) {
    this.api = api;
    this.zoneName = zoneName;
  }

  boolean isPoolType(String type) {
    return type.equals(ResourceTypes.A.name()) || type.equals(ResourceTypes.AAAA.name());
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
      // Somehow Feign does not convert %7B to { and %7D to }. Need to
      // investigate. For now, work around is to pass the body as a
      // parameter. Ugly but works.
      String requestBody = "{" +
          "\"ttl\": 300, " +
          "\"rdata\": [], " +
          "\"profile\": {" +
            "\"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\", " +
            "\"order\": \"ROUND_ROBIN\", " +
            "\"description\": \"This is a great RD Pool\"" +
          "}" +
        "}";
      api.addRRLBPool(zoneName, name, lookup(type), requestBody);
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.POOL_ALREADY_EXISTS) {
        throw e;
      }
    }
  }

  void deletePool(String name, String type) {
    name = checkNotNull(name, "pool name was null");
    type = checkNotNull(type, "pool record type was null");
    final int typeCode = lookup(type);
    try {
      api.deleteLBPool(zoneName, typeCode, name);
    } catch (UltraDNSRestException e) {
      switch (e.code()) {
        // lost race
        case UltraDNSRestException.RESOURCE_RECORD_POOL_NOT_FOUND:
        case UltraDNSRestException.POOL_NOT_FOUND:
        case UltraDNSRestException.RESOURCE_RECORD_NOT_FOUND:
          return;
        default:
          throw e;
      }
    }
  }
}
