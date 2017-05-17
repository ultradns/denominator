package denominator.ultradns.service;

import denominator.ResourceTypeToValue.ResourceTypes;
import denominator.ultradns.service.integration.UltraDNSRest;
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
   * @param type Resource record type
   * @return {@code true} if the type is of A or AAAA code;
   *         {@code false} otherwise.
   */
  boolean isPoolType(String type) {
    return ResourceTypes.A.name().equals(type) || ResourceTypes.AAAA.name().equals(type);
  }

}
