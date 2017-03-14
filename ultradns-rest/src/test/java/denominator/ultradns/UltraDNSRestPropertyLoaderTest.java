package denominator.ultradns;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class UltraDNSRestPropertyLoaderTest {

    String propertyName;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testWhenPropertyPresent() throws Exception {
        propertyName = "ultradns.rest.cte";
        assertThat(UltraDNSRestPropertyLoader.getProperty(propertyName)).isEqualTo("https://test-restapi.ultradns.com/v2");
    }

    @Test
    public void testWhenPropertyAbsent() throws Exception {
        propertyName = "ultradns.rest.new.cte";

        thrown.expect(UltraDNSRestException.class);
        thrown.expectMessage("Could not load property with name " + propertyName + " !! Please check property configuration.");

        assertThat(UltraDNSRestPropertyLoader.getProperty(propertyName)).isEqualTo("https://test-restapi.ultradns.com/v2");
    }
}
