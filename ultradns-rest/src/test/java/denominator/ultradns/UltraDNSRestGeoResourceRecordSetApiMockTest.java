package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Iterator;

import static denominator.ultradns.UltraDNSRestException.DIRECTIONAL_NOT_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class UltraDNSRestGeoResourceRecordSetApiMockTest {

    @Rule
    public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void apiWhenUnsupported() throws Exception {
        server.enqueueSessionResponse();
        server.enqueueError(500, DIRECTIONAL_NOT_ENABLED,
                "Directional feature not Enabled or Directional migration is not done.");

        assertThat(server.connect().api().geoRecordSetsInZone("denominator.io.")).isNull();

        server.assertSessionRequest();
        server.assertRequest("GET", "/geoip/territories?codes=", "");

    }

    @Test
    public void supportedRegionsCache() throws Exception {
        server.enqueueSessionResponse();
        setAvailableRegions();

        GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
        assertThat(api.supportedRegions()).hasSize(15);

        server.assertSessionRequest();
        assertAvailableRegions();
    }

    /**
     * WIP - Need to add more when 3 API will get clarified
     * @throws Exception
     */
    @Test
    public void listWhenPresent() throws Exception {
        server.enqueueSessionResponse();
        setAvailableRegions();
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));
        server.enqueue(new MockResponse().setBody(DIRECTIONAL_POOLS_RESPONSE));

        GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io.");
        Iterator<ResourceRecordSet<?>> iterator = api.iterator();

        /*while (iterator.hasNext()) {
            System.out.printf(iterator.next().name());
        }*/

        server.assertSessionRequest();
        assertAvailableRegions();
        server.assertRequest("GET", "/zones/denominator.io./rrsets/?q=kind%3ADIR_POOLS", "");
        // server.assertRequest("GET", "/zones/denominator.io./rrsets/A/dir_pool_1.denominator.io.?q=kind:DIR_POOLS", "");
    }

    private void setAvailableRegions() {
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_CONTINENTS_RESPONSE));
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_COUNTRIES_RESPONSE));
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_STATES_RESPONSE));
    }

    private void assertAvailableRegions() throws InterruptedException {
        server.assertRequest("GET", "/geoip/territories?codes=", "");
        server.assertRequest("GET", "/geoip/territories?codes=A1%2CA2%2CA3%2CASI%2CEUR%2CNAM", "");
        server.assertRequest("GET", "/geoip/territories?codes=ASI-IN%2CASI-JP%2CASI-MY%2CEUR-ES%2CEUR-GB%2CEUR-SE%2CNAM-U3%2CNAM-US", "");
    }

    private String DIRECTIONAL_POOLS_RESPONSE = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"dir_pool_1.denominator.io.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"rdata\": [\n" +
            "                \"1.1.1.1\",\n" +
            "                \"1.1.2.2\"              \n" +
            "            ],\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/DirPool.jsonschema\",\n" +
            "                \"description\": \"A\",\n" +
            "                \"conflictResolve\": \"GEO\",\n" +
            "                \"rdataInfo\": [\n" +
            "                    {\n" +
            "                        \"geoInfo\": {\n" +
            "                            \"name\": \"GroupB\",\n" +
            "                            \"codes\": [\n" +
            "                                \"NAM\"\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ipInfo\": {\n" +
            "                            \"name\": \"GroupA\",\n" +
            "                            \"ips\": [\n" +
            "                                {\n" +
            "                                    \"start\": \"10.1.1.1\",\n" +
            "                                    \"end\": \"10.1.1.5\"\n" +
            "                                }\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ttl\": 86400,\n" +
            "                        \"type\": \"A\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"geoInfo\": {\n" +
            "                            \"name\": \"Group1-A\",\n" +
            "                            \"codes\": [\n" +
            "                                \"AE\",\n" +
            "                                \"AL\",\n" +
            "                                \"BV\",\n" +
            "                                \"U8\",\n" +
            "                                \"UZ\",\n" +
            "                                \"VA\",\n" +
            "                                \"YE\"\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ttl\": 111,\n" +
            "                        \"type\": \"A\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"noResponse\": {\n" +
            "                    \"geoInfo\": {\n" +
            "                        \"name\": \"GroupX\",\n" +
            "                        \"codes\": [\n" +
            "                            \"EUR\"\n" +
            "                        ]\n" +
            "                    },\n" +
            "                    \"ttl\": null\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"q\": \"kind:DIR_POOLS\",\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}";

    private String GET_AVAILABLE_CONTINENTS_RESPONSE = "[\n" +
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

    private String GET_AVAILABLE_COUNTRIES_RESPONSE = "[\n" +
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

    private String GET_AVAILABLE_STATES_RESPONSE = "[\n" +
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
