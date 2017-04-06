package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import static denominator.assertj.ModelAssertions.assertThat;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.AData;
import denominator.profile.GeoResourceRecordSetApi;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import static denominator.ultradns.UltraDNSRestException.DIRECTIONAL_NOT_ENABLED;
import static denominator.ultradns.UltraDNSMockResponse.GEO_SUPPORTED_REGIONS_SIZE;
import static denominator.ultradns.UltraDNSMockResponse.TTL_200;
import static denominator.ultradns.UltraDNSMockResponse.TTL_500;
import static denominator.ultradns.UltraDNSMockResponse.TTL_300;
import static denominator.ultradns.UltraDNSMockResponse.TTL_100;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;

public class UltraDNSRestGeoResourceRecordSetApiMockTest {

    @Rule
    public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void apiWhenUnsupported() throws Exception {
        server.enqueueSessionResponse();
        server.enqueueError(SC_INTERNAL_SERVER_ERROR, DIRECTIONAL_NOT_ENABLED,
                "Directional feature not Enabled or Directional migration is not done.");

        assertThat(server.connect().api().geoRecordSetsInZone("denominator.io.")).isNull();

        server.assertSessionRequest();
        server.assertRequest("GET", "/geoip/territories?codes=", "");

    }

    @Test
    public void supportedRegionsCache() throws Exception {
        server.enqueueSessionResponse();
        enqueueAvailableRegionsResponse();

        GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
        assertThat(api.supportedRegions()).hasSize(GEO_SUPPORTED_REGIONS_SIZE);

        server.assertSessionRequest();
        assertAvailableRegionsRequest();
    }

    @Test
    public void listWhenPresent() throws Exception {
        server.enqueueSessionResponse();
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        enqueueDirectionalDNSGroupByName();
        enqueueDirectionalDNSGroupByName();
        enqueueDirectionalDNSGroupByName();

        GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
        Iterator<ResourceRecordSet<?>> iterator = api.iterator();

        assertNorthAmerica(iterator.next());
        assertEurope(iterator.next());
        assertAsia(iterator.next());
        assertThat(iterator).isEmpty();

        server.assertSessionRequest();
        assertAvailableRegionsRequest();
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/?q=kind%3ADIR_POOLS",
                "");
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        assertDirectionalDNSGroupByName();
        assertDirectionalDNSGroupByName();
        assertDirectionalDNSGroupByName();
    }

    @Test
    public void iterateByNameWhenPresent() throws Exception {
        server.enqueueSessionResponse();
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        enqueueDirectionalDNSGroupByName();
        enqueueDirectionalDNSGroupByName();
        enqueueDirectionalDNSGroupByName();

        GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
        Iterator<ResourceRecordSet<?>> iterator = api.iterateByName("test_directional_pool.denominator.io.");

        assertNorthAmerica(iterator.next());
        assertEurope(iterator.next());
        assertAsia(iterator.next());
        assertThat(iterator).isEmpty();

        server.assertSessionRequest();
        assertAvailableRegionsRequest();
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/255/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        assertDirectionalDNSGroupByName();
        assertDirectionalDNSGroupByName();
        assertDirectionalDNSGroupByName();

    }

    @Test
    public void iterateByNameAndTypeWhenPresent() throws Exception {
        server.enqueueSessionResponse();
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        enqueueDirectionalDNSGroupByName();
        enqueueDirectionalDNSGroupByName();
        enqueueDirectionalDNSGroupByName();

        GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
        Iterator<ResourceRecordSet<?>> iterator = api.iterateByNameAndType("test_directional_pool.denominator.io.", "A");

        assertNorthAmerica(iterator.next());
        assertEurope(iterator.next());
        assertAsia(iterator.next());
        assertThat(iterator).isEmpty();

        server.assertSessionRequest();
        assertAvailableRegionsRequest();
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        assertDirectionalDNSGroupByName();
        assertDirectionalDNSGroupByName();
        assertDirectionalDNSGroupByName();
    }

    @Test
    public void putWhenRegionsMatches() throws Exception {
        server.enqueueSessionResponse();
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        enqueueAvailableRegionsResponse();

        ResourceRecordSet<AData> europe = ResourceRecordSet
                .<AData>builder()
                .name("test_directional_pool.denominator.io.")
                .type("A")
                .qualifier("Europe")
                .ttl(TTL_200)
                .add(AData.create("2.2.2.2"))
                .geo(Geo.create(new LinkedHashMap<String, Collection<String>>() {
                    {
                        put("Europe", Arrays.asList("Spain", "United Kingdom - England, Northern Ireland, Scotland, Wales", "Sweden"));
                    }
                })).build();

        GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
        api.put(europe);

        server.assertSessionRequest();
        assertAvailableRegionsRequest();
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        assertAvailableRegionsRequest();
    }

    @Test
    public void putWhenRegionsDiffer() throws Exception {
        server.enqueueSessionResponse();
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(
                "{" +
                    "\"message\": \"Successful" +
                "\"}"
        ));

        ResourceRecordSet<AData> europe = ResourceRecordSet
                .<AData>builder()
                .name("test_directional_pool.denominator.io.")
                .type("A")
                .qualifier("Europe")
                .ttl(TTL_200)
                .add(AData.create("2.2.2.2"))
                .geo(Geo.create(new LinkedHashMap<String, Collection<String>>() {
                    {
                        put("Europe", Arrays.asList("Spain"));
                    }
                })).build();

        GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
        api.put(europe);

        server.assertSessionRequest();
        assertAvailableRegionsRequest();
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        assertAvailableRegionsRequest();
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        assertAvailableRegionsRequest();
        server.assertRequest("PATCH",
                "/zones/denominator.io./rrsets/A/test_directional_pool.denominator.io.",
                "[{\"op\": \"replace\", \"path\": \"/profile/rdataInfo/1\", " +
                        "\"value\": {\"geoInfo\":{\"name\":\"Europe\",\"codes\":[\"ES\"]},\"ttl\":200}}]");
    }

    @Test
    public void putWhenTTLDiffers() throws Exception {
        server.enqueueSessionResponse();
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(
                "{" +
                    "\"message\": \"Successful" +
                "\"}"
        ));

        ResourceRecordSet<AData> europe = ResourceRecordSet
                .<AData>builder()
                .name("test_directional_pool.denominator.io.")
                .type("A")
                .qualifier("Europe")
                .ttl(TTL_500)
                .add(AData.create("2.2.2.2"))
                .geo(Geo.create(new LinkedHashMap<String, Collection<String>>() {
                    {
                        put("Europe", Arrays.asList("Spain", "Sweden"));
                    }
                })).build();

        GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
        api.put(europe);

        server.assertSessionRequest();
        assertAvailableRegionsRequest();
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        assertAvailableRegionsRequest();
        server.assertRequest("PATCH",
                "/zones/denominator.io./rrsets/A/test_directional_pool.denominator.io.",
                "[{\"op\": \"replace\", \"path\": \"/profile/rdataInfo/1\", " +
                        "\"value\": {\"geoInfo\":{\"name\":\"Europe\",\"codes\":[\"ES\",\"SE\"]},\"ttl\":500}}]");
    }

    @Test
    public void putAsNewDirectionalRecord() throws Exception {
        server.enqueueSessionResponse();
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        server.enqueueError(SC_BAD_REQUEST, UltraDNSRestException.POOL_ALREADY_EXISTS,
                "Pool already created for this host name : dir_pool_1.test-zone-1.com.");
        enqueueAvailableRegionsResponse();
        server.enqueue(new MockResponse().setBody(
                "{" +
                    "\"message\": \"Successful" +
                "\"}"
        ));

        ResourceRecordSet<AData> europe = ResourceRecordSet
                .<AData>builder()
                .name("test_directional_pool.denominator.io.")
                .type("A")
                .qualifier("Europe")
                .ttl(TTL_500)
                .add(AData.create("7.7.7.7"))
                .geo(Geo.create(new LinkedHashMap<String, Collection<String>>() {
                    {
                        put("Europe", Arrays.asList("Spain", "Sweden"));
                    }
                })).build();

        GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
        api.put(europe);

        server.assertSessionRequest();
        assertAvailableRegionsRequest();
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        server.assertRequest("PATCH",
                "/zones/denominator.io./rrsets/A/test_directional_pool.denominator.io.",
                "[{\"op\": \"remove\", \"path\": \"/rdata/1\"}, " +
                        "{\"op\": \"remove\", \"path\": \"/profile/rdataInfo/1\"}]");
        server.assertRequest("POST",
                "/zones/denominator.io./rrsets/A/test_directional_pool.denominator.io.",
                    "{\"profile\": {\"@context\": \"http://schemas.ultradns.com/DirPool.jsonschema\", " +
                        "\"description\": \"A\"}}");
        assertAvailableRegionsRequest();
        server.assertRequest("PATCH",
                "/zones/denominator.io./rrsets/A/test_directional_pool.denominator.io.",
                "{\n" +
                    "  \"rdata\": [\n" +
                    "    \"7.7.7.7\"\n" +
                    "  ],\n" +
                    "  \"profile\": {\n" +
                    "    \"rdataInfo\": [\n" +
                    "      {\n" +
                    "        \"geoInfo\": {\n" +
                    "          \"name\": \"Europe\",\n" +
                    "          \"codes\": [\n" +
                    "            \"ES\",\n" +
                    "            \"SE\"\n" +
                    "          ]\n" +
                    "        }\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }\n" +
                "}");
    }

    private void enqueueDirectionalDNSGroupByName() {
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        enqueueAvailableRegionsResponse();
    }

    private void assertDirectionalDNSGroupByName() throws InterruptedException {
        server.assertRequest("GET",
                "/zones/denominator.io./rrsets/1/test_directional_pool.denominator.io.?q=kind%3ADIR_POOLS",
                "");
        assertAvailableRegionsRequest();
    }

    private void enqueueAvailableRegionsResponse() {
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_CONTINENTS_RESPONSE));
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_COUNTRIES_RESPONSE));
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_STATES_RESPONSE));
    }

    private void assertAvailableRegionsRequest() throws InterruptedException {
        server.assertRequest("GET", "/geoip/territories?codes=", "");
        server.assertRequest("GET", "/geoip/territories?codes=A1%2CA2%2CA3%2CASI%2CEUR%2CNAM", "");
        server.assertRequest("GET", "/geoip/territories?codes=ASI-IN%2CASI-JP%2CASI-MY%2CEUR-ES%2CEUR-GB%2CEUR-SE%2CNAM-U3%2CNAM-US", "");
    }

    private void assertNorthAmerica(ResourceRecordSet<?> actual) {
        assertThat(actual)
                .hasName("test_directional_pool.denominator.io.")
                .hasType("A")
                .hasQualifier("NorthAmerica")
                .hasTtl(TTL_100)
                .containsExactlyRecords(AData.create("1.1.1.1"))
                .containsRegion("North America", "United States", "U.S. Virgin Islands");
    }

    private void assertEurope(ResourceRecordSet<?> actual) {
        assertThat(actual)
                .hasName("test_directional_pool.denominator.io.")
                .hasType("A")
                .hasQualifier("Europe")
                .hasTtl(TTL_200)
                .containsExactlyRecords(AData.create("2.2.2.2"))
                .containsRegion("Europe", "Spain", "United Kingdom - England, Northern Ireland, Scotland, Wales", "Sweden");
    }

    private void assertAsia(ResourceRecordSet<?> actual) {
        assertThat(actual)
                .hasName("test_directional_pool.denominator.io.")
                .hasType("A")
                .hasQualifier("Asia")
                .hasTtl(TTL_300)
                .containsExactlyRecords(AData.create("3.3.3.3"))
                .containsRegion("Asia", "India", "Japan")
                .containsRegion("India", "West Bengal");
    }

    private static final String DIRECTIONAL_POOLS_RESPONSE = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"test_directional_pool.denominator.io.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"rdata\": [\n" +
            "                \"1.1.1.1\",\n" +
            "                \"2.2.2.2\",\n" +
            "                \"3.3.3.3\"\n" +
            "            ],\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/DirPool.jsonschema\",\n" +
            "                \"description\": \"Nice Pool\",\n" +
            "                \"conflictResolve\": null,\n" +
            "                \"rdataInfo\": [\n" +
            "                    {\n" +
            "                        \"geoInfo\": {\n" +
            "                            \"name\": \"NorthAmerica\",\n" +
            "                            \"codes\": [\n" +
            "                                \"CA-AB\",\n" +
            "                                \"CA-BC\",\n" +
            "                                \"MX\",\n" +
            "                                \"US\",\n" +
            "                                \"VI\"\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ttl\": 100,\n" +
            "                        \"type\": \"A\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"geoInfo\": {\n" +
            "                            \"name\": \"Europe\",\n" +
            "                            \"codes\": [\n" +
            "                                \"ES\",\n" +
            "                                \"GB\",\n" +
            "                                \"SE\"\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ttl\": 200,\n" +
            "                        \"type\": \"A\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"geoInfo\": {\n" +
            "                            \"name\": \"Asia\",\n" +
            "                            \"codes\": [\n" +
            "                                \"BD\",\n" +
            "                                \"IN\",\n" +
            "                                \"IN-UT\",\n" +
            "                                \"IN-WB\",\n" +
            "                                \"JP\"\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ttl\": 300,\n" +
            "                        \"type\": \"A\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"noResponse\": {\n" +
            "                    \"geoInfo\": {\n" +
            "                        \"name\": \"Group-NoResPonse\",\n" +
            "                        \"codes\": [\n" +
            "                            \"ANT\"\n" +
            "                        ]\n" +
            "                    },\n" +
            "                    \"ttl\": null\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    private static final String GET_AVAILABLE_CONTINENTS_RESPONSE = "[\n" +
            "    [\n" +
            "        {\n" +
            "            \"name\": \"Anonymous Proxy\",\n" +
            "            \"code\": \"A1\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 315\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Satellite Provider\",\n" +
            "            \"code\": \"A2\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 316\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Unknown / Uncategorized IPs\",\n" +
            "            \"code\": \"A3\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 331\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"North America\",\n" +
            "            \"code\": \"NAM\",\n" +
            "            \"type\": \"Region\",\n" +
            "            \"id\": 338\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Europe\",\n" +
            "            \"code\": \"EUR\",\n" +
            "            \"type\": \"Region\",\n" +
            "            \"id\": 336\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Asia\",\n" +
            "            \"code\": \"ASI\",\n" +
            "            \"type\": \"Region\",\n" +
            "            \"id\": 334\n" +
            "        }\n" +
            "    ]\n" +
            "]";

    private static final String GET_AVAILABLE_COUNTRIES_RESPONSE = "[\n" +
            "    [\n" +
            "    ],\n" +
            "    [\n" +
            "    ],\n" +
            "    [\n" +
            "    ],  \n" +
            "    [\n" +
            "        {\n" +
            "            \"name\": \"India\",\n" +
            "            \"code\": \"IN\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 244\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Japan\",\n" +
            "            \"code\": \"JP\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 246\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Malaysia\",\n" +
            "            \"code\": \"MY\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 253\n" +
            "        }\n" +
            "    ],\n" +
            "    [\n" +
            "        {\n" +
            "            \"name\": \"Spain\",\n" +
            "            \"code\": \"ES\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 151\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Sweden\",\n" +
            "            \"code\": \"SE\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 152\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"United Kingdom - England, Northern Ireland, Scotland, Wales\",\n" +
            "            \"code\": \"GB\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 155\n" +
            "        }\n" +
            "    ],\n" +
            "    [\n" +
            "        {\n" +
            "            \"name\": \"U.S. Virgin Islands\",\n" +
            "            \"code\": \"VI\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 306\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Undefined Central America\",\n" +
            "            \"code\": \"U3\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 319\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"United States\",\n" +
            "            \"code\": \"US\",\n" +
            "            \"type\": \"Country\",\n" +
            "            \"id\": 340\n" +
            "        }\n" +
            "    ]\n" +
            "]";

    private static final String GET_AVAILABLE_STATES_RESPONSE = "[\n" +
            "    [\n" +
            "        {\n" +
            "            \"name\": \"Delhi\",\n" +
            "            \"code\": \"DL\",\n" +
            "            \"type\": \"State\",\n" +
            "            \"id\": 1972\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Tamil Nadu\",\n" +
            "            \"code\": \"TN\",\n" +
            "            \"type\": \"State\",\n" +
            "            \"id\": 1995\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"West Bengal\",\n" +
            "            \"code\": \"WB\",\n" +
            "            \"type\": \"State\",\n" +
            "            \"id\": 1999\n" +
            "        }       \n" +
            "    ],\n" +
            "    [\n" +
            "    ],\n" +
            "    [\n" +
            "    ],\n" +
            "    [\n" +
            "        {\n" +
            "            \"name\": \"Caceres\",\n" +
            "            \"code\": \"CC\",\n" +
            "            \"type\": \"State\",\n" +
            "            \"id\": 1276\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Cadiz\",\n" +
            "            \"code\": \"CA\",\n" +
            "            \"type\": \"State\",\n" +
            "            \"id\": 1275\n" +
            "        }\n" +
            "    ],\n" +
            "    [\n" +
            "    ],\n" +
            "    [\n" +
            "    ],\n" +
            "    [\n" +
            "    ],\n" +
            "    [\n" +
            "        {\n" +
            "            \"name\": \"Alabama\",\n" +
            "            \"code\": \"AL\",\n" +
            "            \"type\": \"State\",\n" +
            "            \"id\": 46\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Alaska\",\n" +
            "            \"code\": \"AK\",\n" +
            "            \"type\": \"State\",\n" +
            "            \"id\": 6\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"Arizona\",\n" +
            "            \"code\": \"AZ\",\n" +
            "            \"type\": \"State\",\n" +
            "            \"id\": 36\n" +
            "        }\n" +
            "    ]\n" +
            "]\n";
}
