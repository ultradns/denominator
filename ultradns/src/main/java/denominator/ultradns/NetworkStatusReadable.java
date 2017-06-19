package denominator.ultradns;

import javax.inject.Inject;

import denominator.CheckConnection;
import denominator.ultradns.UltraDNS.NetworkStatus;

/**
 * @deprecated UltraDNS SOAP API is close to EOL (End of Life),
 * use {@link denominator.ultradns.service.NetworkConnection} instead.
 *
 * <p>
 * UltraDNS-REST provider details :
 * <ul>
 * <li>Provider name : ultradnsrest</li>
 * <li>URL : https://restapi.ultradns.com/v2</li>
 * </ul>
 */
@Deprecated
class NetworkStatusReadable implements CheckConnection {

  private final UltraDNS api;

  @Inject
  NetworkStatusReadable(UltraDNS api) {
    this.api = api;
  }

  @Override
  public boolean ok() {
    try {
      return NetworkStatus.GOOD == api.getNeustarNetworkStatus();
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "NetworkStatusReadable";
  }
}
