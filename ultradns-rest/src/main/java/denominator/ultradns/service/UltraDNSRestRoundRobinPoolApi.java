package denominator.ultradns.service;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.common.Preconditions.checkNotNull;

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
