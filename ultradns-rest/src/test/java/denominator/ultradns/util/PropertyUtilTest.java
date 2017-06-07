package denominator.ultradns.util;

import denominator.ultradns.exception.UltraDNSRestException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyUtilTest {

    private String propertyName;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testWhenPropertyPresent() throws Exception {
        propertyName = "ultradns.rest.url";
        assertThat(PropertyUtil.getProperty(propertyName)).isEqualTo("https://restapi.ultradns.com/v2");
    }

    @Test
    public void testWhenPropertyAbsent() throws Exception {
        propertyName = "ultradns.rest.new.cte";

        thrown.expect(UltraDNSRestException.class);
        thrown.expectMessage("Could not load property with name " + propertyName +
                " !! Please check property configuration.");

        assertThat(PropertyUtil.getProperty(propertyName)).isEqualTo("https://restapi.ultradns.com/v2");
    }
}
