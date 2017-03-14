package denominator.ultradns;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import denominator.Provider;

import static denominator.Providers.list;
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

}
