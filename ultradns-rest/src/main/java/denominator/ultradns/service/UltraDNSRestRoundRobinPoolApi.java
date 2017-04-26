package denominator.ultradns.service;

import java.util.List;
import java.util.Map;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Preconditions.checkState;

import denominator.ResourceTypeToValue.ResourceTypes;
import denominator.ultradns.service.integration.UltraDNSRest;
import denominator.ultradns.exception.UltraDNSRestException;
import org.apache.log4j.Logger;

public class UltraDNSRestRoundRobinPoolApi {

  private final UltraDNSRest api;
  private final String zoneName;

  UltraDNSRestRoundRobinPoolApi(UltraDNSRest api, String zoneName) {
    this.api = api;
    this.zoneName = zoneName;
  }

  private static final Logger LOGGER = Logger.getLogger(UltraDNSRestRoundRobinPoolApi.class);

  /**
   * Returns true if the specified pool is of type A or AAAA,false otherwise.
   * @param type
   * @return {@code true} if the type is of A or AAAA code;
   *         {@code false} otherwise.
   */
  boolean isPoolType(String type) {
    return type.equals(ResourceTypes.A.name()) || type.equals(ResourceTypes.AAAA.name());
  }

  /**
   * Add a zone with ttl & address.
   * @param name
   * @param type
   * @param ttl
   * @param rdatas
   */
  void add(String name, String type, int ttl, List<Map<String, Object>> rdatas) {
    checkState(isPoolType(type), "%s not A or AAAA type", type);
    // String poolId = reuseOrCreatePoolForNameAndType(name, type);
    LOGGER.debug("Creating or using pool with zone name " + zoneName +
            ",name " + name + ",type " + type + " and ttl " + ttl);
    reuseOrCreatePoolForNameAndType(name, type);
    final int typeCode = lookup(type);
    for (Map<String, Object> rdata : rdatas) {
      String address = rdata.get("address").toString();
      api.addRecordToRRPool(typeCode, ttl, address, name, zoneName);
    }
  }

  /**
   * Adds the zone to the pool if exists else creates and adds it.
   * @param name
   * @param type
   */
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

  /**
   * Deletes pool with given domain name and record type.
   * @param name
   * @param type
   */
  void deletePool(String name, String type) {
    name = checkNotNull(name, "pool name was null");
    type = checkNotNull(type, "pool record type was null");
    final int typeCode = lookup(type);
    LOGGER.debug("Deleting pool with name " + name + " and type " + type);
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
