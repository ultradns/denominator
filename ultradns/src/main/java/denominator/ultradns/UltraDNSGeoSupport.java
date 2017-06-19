package denominator.ultradns;

import java.util.Collection;
import java.util.Map;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * @deprecated UltraDNS SOAP API is close to EOL (End of Life),
 * use {@link denominator.ultradns.service.UltraDNSRestGeoSupport} instead.
 *
 * <p>
 * UltraDNS-REST provider details :
 * <ul>
 * <li>Provider name : ultradnsrest</li>
 * <li>URL : https://restapi.ultradns.com/v2</li>
 * </ul>
 */
@Deprecated
@Module(injects = UltraDNSGeoResourceRecordSetApi.Factory.class, complete = false)
public class UltraDNSGeoSupport {

  @Provides
  @Named("geo")
  Map<String, Collection<String>> regions(UltraDNS api) {
    return api.getAvailableRegions();
  }
}
