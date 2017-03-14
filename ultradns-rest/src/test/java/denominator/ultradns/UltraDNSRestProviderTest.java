package denominator.ultradns;

import denominator.DNSApiManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import dagger.ObjectGraph;
import denominator.Credentials.MapCredentials;
import denominator.Provider;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Providers.list;
import static denominator.Providers.provide;
import static org.assertj.core.api.Assertions.assertThat;

public class UltraDNSRestProviderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private static final Provider PROVIDER = new UltraDNSRestProvider();

  @Test
  public void testUltraDNSRestMetadata() {
    assertThat(PROVIDER.name()).isEqualTo("ultradnsrest");
    assertThat(PROVIDER.supportsDuplicateZoneNames()).isFalse();
    assertThat(PROVIDER.credentialTypeToParameterNames())
        .containsEntry("password", Arrays.asList("username", "password"));
  }

  @Test
  public void testUltraDNSRestRegistered() {
    assertThat(list()).contains(PROVIDER);
  }

  @Test
  public void testProviderWiresUltraDNSRestZoneApi() {
    DNSApiManager manager = create(PROVIDER, credentials("username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(UltraDNSRestZoneApi.class);
    manager = create("ultradnsrest", credentials("username", "password"));
    assertThat(manager.api().zones()).isInstanceOf(UltraDNSRestZoneApi.class);

    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("username", "U");
    map.put("password", "P");
    manager = create("ultradnsrest", credentials(MapCredentials.from(map)));
    assertThat(manager.api().zones()).isInstanceOf(UltraDNSRestZoneApi.class);
  }

  @Test
  public void testCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("no credentials supplied. ultradnsrest requires username,password");

    create(PROVIDER).api().zones().iterator();
  }

  @Test
  public void testTwoPartCredentialsRequired() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("incorrect credentials supplied. ultradnsrest requires username,password");

    create(PROVIDER, credentials("customer", "username", "password")).api().zones().iterator();
  }

  @Test
  public void testViaDagger() {
    DNSApiManager manager = ObjectGraph
            .create(provide(
                    new UltraDNSRestProvider()),
                    new UltraDNSRestProvider.Module(),
                    credentials("username", "password"))
            .get(DNSApiManager.class);
    assertThat(manager.api().zones()).isInstanceOf(UltraDNSRestZoneApi.class);
  }

}
