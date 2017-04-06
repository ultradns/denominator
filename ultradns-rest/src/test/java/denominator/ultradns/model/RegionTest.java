package denominator.ultradns.model;

import org.junit.Test;

import java.util.Arrays;

import static denominator.assertj.ModelAssertions.assertThat;

public class RegionTest {

    private static final int SAM_ID = 12;
    private static final int SAM_WRONG_ID = 1200;
    private static final int NAM_ID = 13;
    private static final int USA_ID = 14;
    private static final int MX_ID = 15;
    private static final int CA_ID = 16;
    private static final int VA_ID = 101;

    @Test
    public void testSetNameAndGetName() throws Exception {
        Region region = new Region("North America", "SAM", "Region", SAM_ID);
        region.setName("South America");
        assertThat(region.getName()).isEqualTo("South America");
    }

    @Test
    public void testSetCodeAndGetCode() throws Exception {
        Region region = new Region("South America", "NAM", "Region", SAM_ID);
        region.setCode("SAM");
        assertThat(region.getCode()).isEqualTo("SAM");
    }

    @Test
    public void testSetTypeAndGetType() throws Exception {
        Region region = new Region("South America", "SAM", "State", SAM_ID);
        region.setType("Region");
        assertThat(region.getType()).isEqualTo("Region");
    }

    @Test
    public void testSetIdAndGetId() throws Exception {
        Region region = new Region("South America", "SAM", "Region", SAM_WRONG_ID);
        region.setId(SAM_ID);
        assertThat(region.getId()).isEqualTo(SAM_ID);
    }

    @Test
    public void testGetEffectiveCodesOfChildRegions() throws Exception {
        Region nam = new Region("North America", "NAM", "Region", NAM_ID);
        nam.setEffectiveCode();
        Region usa = new Region("United States of America", "US", "Country", USA_ID);
        Region mexico = new Region("Mexico", "MX", "Country", MX_ID);
        Region canada = new Region("Canada", "CA", "Country", CA_ID);
        nam.setChildRegions(Arrays.asList(usa, mexico, canada));

        // Check if effective codes are set properly for the child regions.
        assertThat(usa.getEffectiveCode()).isEqualTo("NAM-US");
        assertThat(mexico.getEffectiveCode()).isEqualTo("NAM-MX");
        assertThat(canada.getEffectiveCode()).isEqualTo("NAM-CA");

        // Check if effective codes for geo are set properly for a region,
        // its children and its grandchildren.
        assertThat(nam.getEffectiveCodeForGeo()).isEqualTo("NAM");
        assertThat(usa.getEffectiveCodeForGeo()).isEqualTo("US");
        Region virginiaInUsa = new Region("Virginia", "VA", "State", VA_ID);
        usa.setChildRegions(Arrays.asList(virginiaInUsa));
        assertThat(virginiaInUsa.getEffectiveCodeForGeo()).isEqualTo("US-VA");
    }

    @Test
    public void testToString() throws Exception {
        Region region = new Region("South America", "SAM", "Region", SAM_ID);
        region.setEffectiveCode();
        assertThat(region.toString()).isEqualTo("{\"code\": \"SAM\", " +
                "\"name\": \"South America\", " +
                "\"type\": \"Region\", " +
                "\"id\": " + SAM_ID + ", " +
                "\"effectiveCode\": \"SAM\", " +
                "\"effectiveCodeForGeo\": \"SAM\"}");
    }

    @Test
    public void testEquals() throws Exception {
        Region region = new Region("South America", "SAM", "Region", SAM_ID);
        assertThat(region).isNotEqualTo("ARRGH");
    }
}
