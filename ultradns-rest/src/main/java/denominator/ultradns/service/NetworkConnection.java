package denominator.ultradns.service;

import javax.inject.Inject;

import denominator.CheckConnection;
import denominator.ultradns.service.integration.UltraDNSRest;

public class NetworkConnection implements CheckConnection {

  private final UltraDNSRest api;

  @Inject
  NetworkConnection(UltraDNSRest api) {
    this.api = api;
  }

  @Override
  public boolean ok() {
    try {
      return "GOOD".equalsIgnoreCase(api.getNeustarNetworkStatus().getMessage());
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "NetworkConnection";
  }
}
