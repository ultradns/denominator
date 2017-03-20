package denominator.ultradns.model;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.TreeSet;

import static denominator.assertj.ModelAssertions.assertThat;

public class RegionTest {

    @Test
    public void testSetNameAndGetName() throws Exception {
        Region region = new Region("North America", "SAM", "Region", 12);
        region.setName("South America");
        assertThat(region.getName()).isEqualTo("South America");
    }

    @Test
    public void testSetCodeAndGetCode() throws Exception {
        Region region = new Region("South America", "NAM", "Region", 12);
        region.setCode("SAM");
        assertThat(region.getCode()).isEqualTo("SAM");
    }

    @Test
    public void testSetTypeAndGetType() throws Exception {
        Region region = new Region("South America", "SAM", "State", 12);
        region.setType("Region");
        assertThat(region.getType()).isEqualTo("Region");
    }

    @Test
    public void testSetIdAndGetId() throws Exception {
        Region region = new Region("South America", "SAM", "Region", 1200);
        region.setId(12);
        assertThat(region.getId()).isEqualTo(12);
    }

    @Test
    public void testGetEffectiveCodesOfChildRegions() throws Exception {
        Region nam = new Region("North America", "NAM", "Region", 13);
        nam.setEffectiveCode();
        Region usa = new Region("United States of America", "US", "Country", 14);
        Region mexico = new Region("Mexico", "MX", "Country", 15);
        Region canada = new Region("Canada", "CA", "Country", 16);
        nam.setChildRegions(Arrays.asList(usa, mexico, canada));
        TreeSet<String> tmpExpectedEffectiveCodes = new TreeSet<String>(Arrays.asList("NAM-CA", "NAM-MX", "NAM-US"));
        String[] expectedEffectiveCodes = new String[tmpExpectedEffectiveCodes.size()];
        tmpExpectedEffectiveCodes.toArray(expectedEffectiveCodes);
        assertThat(nam.getEffectiveCodesOfChildRegions()).isEqualTo(expectedEffectiveCodes);
    }

    @Test
    public void testToString() throws Exception {
        Region region = new Region("South America", "SAM", "Region", 12);
        region.setEffectiveCode();
        assertThat(region.toString()).isEqualTo("{\"code\": \"SAM\", \"name\": \"South America\", \"type\": \"Region\", \"id\": 12, \"effectiveCode\": \"SAM\"}");
    }

    @Test
    public void testEquals() throws Exception {
        Region region = new Region("South America", "SAM", "Region", 12);
        assertThat(region).isNotEqualTo("ARRGH");
    }

}