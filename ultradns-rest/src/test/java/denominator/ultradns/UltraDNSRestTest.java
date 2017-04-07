package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import denominator.Credentials;
import denominator.ultradns.model.DirectionalRecord;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.RRSetList;
import denominator.ultradns.model.Region;
import denominator.ultradns.util.RRSetUtil;
import feign.Feign;
import org.junit.Rule;
import org.junit.Test;

import denominator.ultradns.InvalidatableTokenProvider.Session;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import denominator.ResourceTypeToValue.ResourceTypes;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import static denominator.ultradns.UltraDNSMockResponse.GET_ACCOUNTS_LIST_OF_USER;
import static denominator.ultradns.UltraDNSMockResponse.GET_ZONES_OF_ACCOUNT_PRESENT;
import static denominator.ultradns.UltraDNSMockResponse.GET_ZONES_OF_ACCOUNT_ABSENT;
import static denominator.ultradns.UltraDNSMockResponse.GET_RESOURCE_RECORDS_PRESENT;
import static denominator.ultradns.UltraDNSMockResponse.RR_SET_ABSENT;
import static denominator.ultradns.UltraDNSMockResponse.STATUS_SUCCESS;
import static denominator.ultradns.UltraDNSMockResponse.GET_DIRECTIONAL_POOLS_OF_ZONE;
import static denominator.ultradns.UltraDNSMockResponse.RESOURCE_RECORDS_COUNT;
import static denominator.ultradns.UltraDNSMockResponse.RESOURCE_RECORD_TYPE;
import static denominator.ultradns.UltraDNSMockResponse.TTL_86400;
import static denominator.ultradns.UltraDNSMockResponse.TTL_50;
import static denominator.ultradns.UltraDNSMockResponse.TTL_100;
import static denominator.ultradns.UltraDNSMockResponse.TTL_122;
import static denominator.ultradns.UltraDNSMockResponse.REGION_CODE_A1;
import static denominator.ultradns.UltraDNSMockResponse.REGION_CODE_A2;
import static denominator.ultradns.UltraDNSMockResponse.REGION_CODE_A3;
import static denominator.ultradns.UltraDNSMockResponse.REGION_CODE_NAM;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;

public class UltraDNSRestTest {

    @Rule
    public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    /**
     * Mock API to call UltraDNSRest Endpoint.
     * @return
     */
    UltraDNSRest mockApi() {
        UltraDNSRestProvider.FeignModule module = new UltraDNSRestProvider.FeignModule();
        UltraDNSRestProvider provider = new UltraDNSRestProvider() {
            @Override
            public String url() {
                return server.url();
            }
        };
        javax.inject.Provider<Credentials> credentials = new javax.inject.Provider<Credentials>() {
            @Override
            public Credentials get() {
                return server.credentials();
            }

        };
        AtomicReference<Boolean> sessionValid = module.sessionValid();
        UltraDNSRestErrorDecoder errorDecoder = new UltraDNSRestErrorDecoder(sessionValid);
        Feign feign = module.feign(module.logger(), module.logLevel(), errorDecoder);
        Session session = feign.newInstance(new SessionTarget(provider));

        InvalidatableTokenProvider tokenProvider = new InvalidatableTokenProvider(provider,
                session, credentials, sessionValid);
        tokenProvider.setLastCredentialsHashCode(credentials.get().hashCode());
        tokenProvider.setToken("token");
        sessionValid.set(true);

        return feign.newInstance(new UltraDNSRestTarget(new UltraDNSRestProvider() {
            @Override
            public String url() {
                return server.url();
            }
        }, tokenProvider));
    }

    @Test
    public void networkGood() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody("{ \"message\": \"Good\" }"));
        server.enqueue(new MockResponse());

        assertThat(mockApi().getNeustarNetworkStatus().getMessage()).isEqualTo("Good");

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/status");
    }

    @Test
    public void networkFailed() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody("{ \"message\": \"Failed\" }"));
        server.enqueue(new MockResponse());

        assertThat(mockApi().getNeustarNetworkStatus().getMessage()).isEqualTo("Failed");

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/status");
    }

    @Test
    public void retryOnSystemError() throws Exception {
        thrown.expect(UltraDNSRestException.class);
        thrown.expectMessage("System Error");

        server.enqueueSessionResponse();
        // First time, send a "System Error" response.
        server.enqueue(new MockResponse()
                .setResponseCode(SC_INTERNAL_SERVER_ERROR)
                .setBody(UltraDNSMockResponse.getMockErrorResponse(
                        UltraDNSRestException.SYSTEM_ERROR,
                        "System Error")));
        // Second time, send the network status good response.
        server.enqueue(new MockResponse().setBody("{ \"message\": \"Good\" }"));

        assertThat(mockApi().getNeustarNetworkStatus().getMessage()).isEqualTo("Good");

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/status");
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/status");
    }

    @Test
    public void noRetryOnInvalidUser() throws Exception {
        thrown.expect(UltraDNSRestException.class);
        thrown.expectMessage(
                "invalid_grant:Token not found, expired or invalid.");

        server.enqueueSessionResponse();
        server.enqueue(new MockResponse()
                .setResponseCode(SC_UNAUTHORIZED)
                .setBody(UltraDNSMockResponse.getMockErrorResponse(
                        UltraDNSRestException.INVALID_GRANT,
                        "invalid_grant:Token not found, expired or invalid.")));

        mockApi().getNeustarNetworkStatus();
    }

    @Test
    public void testAccountsListOfUser() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(GET_ACCOUNTS_LIST_OF_USER));
        server.enqueue(new MockResponse());

        assertThat(mockApi().getAccountsListOfUser().getAccounts().size()).isEqualTo(2);

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/accounts");
    }

    @Test
    public void zonesOfAccountPresent() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(GET_ZONES_OF_ACCOUNT_PRESENT));
        server.enqueue(new MockResponse());

        assertThat(mockApi().getZonesOfAccount("npp-rest-test1").getZones().size()).isEqualTo(2);

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/accounts/npp-rest-test1/zones");
    }

    @Test
    public void zonesOfAccountAbsent() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(GET_ZONES_OF_ACCOUNT_ABSENT));
        server.enqueue(new MockResponse());

        assertThat(mockApi().getZonesOfAccount("npp-rest-test1").getZones().size()).isEqualTo(0);

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/accounts/npp-rest-test1/zones");
    }

    @Test
    public void recordsInZonePresent() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(GET_RESOURCE_RECORDS_PRESENT));
        server.enqueue(new MockResponse());

        RRSetList rrSetList = mockApi().getResourceRecordsOfZone("denominator.io.");

        assertThat(rrSetList.getRrSets().get(0).getRdata().size()).isEqualTo(RESOURCE_RECORDS_COUNT);
        assertThat(rrSetList.getRrSets()).extracting("ownerName", "rrtype", "ttl")
                .containsExactly(
                        tuple("pool_2.denominator.io.", "A (1)", TTL_86400)
                );

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/zones/denominator.io./rrsets");
    }

    @Test
    public void recordsInZoneAbsent() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(RR_SET_ABSENT));
        server.enqueue(new MockResponse());

        RRSetList rrSetList = mockApi().getResourceRecordsOfZone("denominator.io.");
        assertThat(rrSetList.getRrSets().size()).isEqualTo(0);

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/zones/denominator.io./rrsets");
    }

    @Test
    public void recordsInZoneByNameAndTypePresent() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(GET_RESOURCE_RECORDS_PRESENT));
        server.enqueue(new MockResponse());

        RRSetList rrSetList = mockApi().getResourceRecordsOfDNameByType("denominator.io.", "pool_2.denominator.io.", 1);

        assertThat(rrSetList.getRrSets().get(0).getRdata().size()).isEqualTo(RESOURCE_RECORDS_COUNT);
        assertThat(rrSetList.getRrSets()).extracting("ownerName", "rrtype", "ttl")
                .containsExactly(
                        tuple("pool_2.denominator.io.", "A (1)", TTL_86400)
                );

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/zones/denominator.io./rrsets/1/pool_2.denominator.io.");
    }

    @Test
    public void recordsInZoneByNameAndTypeAbsent() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(RR_SET_ABSENT));
        server.enqueue(new MockResponse());

        RRSetList rrSetList = mockApi().getResourceRecordsOfDNameByType("denominator.io.", "pool_2.denominator.io.", 1);
        assertThat(rrSetList.getRrSets().size()).isEqualTo(0);

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/zones/denominator.io./rrsets/1/pool_2.denominator.io.");
    }

    @Test
    public void recordsInZoneByNameAndTypeInvalidZone() throws Exception {
        thrown.expect(UltraDNSRestException.class);
        thrown.expectMessage("Zone does not exist in the system.");

        // Response to the request to get the RR Sets in the pool.
        server.enqueue(new MockResponse()
                .setResponseCode(SC_NOT_FOUND)
                .setBody(UltraDNSMockResponse.getMockErrorResponse(
                        UltraDNSRestException.ZONE_NOT_FOUND,
                        "Zone does not exist in the system.")));

        mockApi().getResourceRecordsOfDNameByType("ARGHH", "ARGHH", RESOURCE_RECORD_TYPE);
    }

    @Test
    public void createRecordInZone() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse());

        List<String> rdata = new ArrayList<String>();
        rdata.add("1.1.1.1");
        rdata.add("2.2.2.2");

        mockApi().createResourceRecord("denominator.io.", 1, "denominator.io.",
                getSampleRRSet("denominator.io.", "A (1)", rdata));

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("POST")
                .hasPath("/zones/denominator.io./rrsets/1/denominator.io.");
    }

    @Test
    public void updateRecordInZone() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse());

        List<String> rdata = new ArrayList<String>();
        rdata.add("1.1.1.1");
        rdata.add("2.2.2.2");

        mockApi().updateResourceRecord("denominator.io.", 1, "denominator.io.",
                getSampleRRSet("denominator.io.", "A (1)", rdata));

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("PUT")
                .hasPath("/zones/denominator.io./rrsets/1/denominator.io.");
    }

    @Test
    public void deleteRecord() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));

        mockApi().deleteResourceRecord("denominator.io.", 1, "www.denominator.io.", 0);

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("PATCH")
                .hasPath("/zones/denominator.io./rrsets/1/www.denominator.io.");
    }

    @Test
    public void createRRPoolInZoneForNameAndType() throws Exception {
        final String zoneName = "denominator.io.";
        final String hostName = "h1.denominator.io.";
        final int typeCode = 1;
        final String expectedPath = "/zones/" + zoneName + "/rrsets/" + typeCode + "/" + hostName;
        final String expectedBody =
                "{" +
                    "\"ttl\": 300, " +
                    "\"rdata\": [], " +
                    "\"profile\": {" +
                        "\"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\", " +
                        "\"order\": \"ROUND_ROBIN\", " +
                        "\"description\": \"This is a great RD Pool\"" +
                    "}" +
                "}";
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));
        mockApi().addRRLBPool(zoneName, hostName, typeCode, expectedBody);
        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("POST")
                .hasPath(expectedPath)
                .hasBody(expectedBody);
    }

    @Test
    public void testAddRRLBPoolWithNonFQDNAsHostName() throws Exception {
        final String zoneName = "denominator.io.";
        final String hostName = "h2";
        final int typeCode = 1;
        final String expectedPath = "/zones/" + zoneName + "/rrsets/" + typeCode + "/" + hostName;
        final String expectedBody =
                "{" +
                    "\"ttl\": 300, " +
                    "\"rdata\": [], " +
                    "\"profile\": {" +
                        "\"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\", " +
                        "\"order\": \"ROUND_ROBIN\", " +
                        "\"description\": \"This is a great RD Pool\"" +
                    "}" +
                "}";
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));
        mockApi().addRRLBPool(zoneName, hostName, typeCode, expectedBody);
        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("POST")
                .hasPath(expectedPath)
                .hasBody(expectedBody);
    }

    @Test
    public void createRRPoolInZoneForNameAndTypeWhenAlreadyExists() throws Exception {
        thrown.expect(UltraDNSRestException.class);
        thrown.expectMessage("Pool already created for this host name : h2.denominator.io.");

        final String zoneName = "denominator.io.";
        final String hostName = "h2";
        final int typeCode = 1;
        final String expectedBody =
                "{" +
                    "\"ttl\": 300, " +
                    "\"rdata\": [], " +
                    "\"profile\": {" +
                    "\"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\", " +
                        "\"order\": \"ROUND_ROBIN\", " +
                        "\"description\": \"This is a great RD Pool\"" +
                    "}" +
                "}";
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setResponseCode(SC_BAD_REQUEST).setBody(UltraDNSMockResponse
                .getMockErrorResponse(UltraDNSRestException.POOL_ALREADY_EXISTS,
                        "Pool already created for this host name : h2.denominator.io.")));
        mockApi().addRRLBPool(zoneName, hostName, typeCode, expectedBody);
    }

    @Test
    public void getRegionsByIdAndName() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));

        Region anonymousProxy = new Region("Anonymous Proxy", "A1", "Country", REGION_CODE_A1);
        Region satelliteProvider = new Region("Satellite Provider", "A2", "Country", REGION_CODE_A2);
        Region unknownOrUncategorizedIPs = new Region("Unknown / Uncategorized IPs", "A3", "Country", REGION_CODE_A3);
        Region northAmerica = new Region("North America", "NAM", "Region", REGION_CODE_NAM);

        Collection<Collection<Region>> group = mockApi().getAvailableRegions("");
        Collection<Region> topLevelRegions = group.iterator().next();
        Iterator<Region> topLevelRegionIterator = topLevelRegions.iterator();

        Region firstRegion = topLevelRegionIterator.next();
        assertThat(firstRegion).isEqualTo(anonymousProxy);
        Region secondRegion = topLevelRegionIterator.next();
        assertThat(secondRegion).isEqualTo(satelliteProvider);
        Region thirdRegion = topLevelRegionIterator.next();
        assertThat(thirdRegion).isEqualTo(unknownOrUncategorizedIPs);
        Region fourthRegion = topLevelRegionIterator.next();
        assertThat(fourthRegion).isEqualTo(northAmerica);
    }

    @Test
    public void testAddRecordIntoRRPoolInZoneWhenRecordIsProper() throws Exception {
        final String zoneName = "denominator.io.";
        final String hostName = "h2";
        final String address = "1.1.1.1";
        final int ttl = 300;
        final int typeCode = 1;

        final String expectedPath = "/zones/" + zoneName + "/rrsets/" + typeCode + "/" + hostName;
        final String expectedBody =
                "{" +
                    "\"ttl\": " + ttl + ", " +
                    "\"rdata\": [\"" + address + "\"]" +
                "}";

        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));

        mockApi().addRecordToRRPool(typeCode, ttl, address, hostName, zoneName);

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("PATCH")
                .hasPath(expectedPath)
                .hasBody(expectedBody);
    }

    @Test
    public void testAddRecordIntoRRPoolInZoneWhenRecordIsNotProper() throws Exception {
        final String address = "blah";
        final String expectedErrorMessage = String.format("Invalid input: record data - Invalid address: %s", address);
        thrown.expect(UltraDNSRestException.class);
        thrown.expectMessage(expectedErrorMessage);

        final String zoneName = "denominator.io.";
        final String hostName = "h2";
        final int ttl = 300;
        final int typeCode = 1;

        server.enqueueSessionResponse();
        final String actualErrorMessage = String.format("Invalid input: record data - Invalid address: %s", address);
        server.enqueue(new MockResponse().setResponseCode(SC_BAD_REQUEST).setBody(UltraDNSMockResponse
                .getMockErrorResponse(UltraDNSRestException.INVALID_ADDRESS_IN_RECORD_DATA, actualErrorMessage)));

        mockApi().addRecordToRRPool(typeCode, ttl, address, hostName, zoneName);
    }

    @Test
    public void deleteRRPool() throws Exception {
        final String zoneName = "denominator.io.";
        final String hostName = "h2";
        final int typeCode = 1;

        final String expectedPath = "/zones/" + zoneName + "/rrsets/" + typeCode + "/" + hostName;

        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setResponseCode(SC_NO_CONTENT));
        mockApi().deleteLBPool(zoneName, typeCode, hostName);
        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("DELETE")
                .hasPath(expectedPath);
    }

    @Test
    public void deleteRRPoolWhenPoolNotFound() throws Exception {
        thrown.expect(UltraDNSRestException.class);
        thrown.expectMessage("Cannot find resource record data for the input zone, record type and owner combination.");

        final String zoneName = "denominator.io.";
        final String hostName = "h2";
        final int typeCode = 1;
        server.enqueue(new MockResponse().setResponseCode(SC_NOT_FOUND).setBody(UltraDNSMockResponse
                .getMockErrorResponse(UltraDNSRestException.RESOURCE_RECORD_POOL_NOT_FOUND,
                        "Cannot find resource record data for the input zone, record type and owner combination.")));
        mockApi().deleteLBPool(zoneName, typeCode, hostName);
    }

    private RRSet getSampleRRSet(String ownerName, String rrtype, List<String> rdata) {
        RRSet rrSet = new RRSet(TTL_86400, rdata);
        rrSet.setOwnerName(ownerName);
        rrSet.setRrtype(rrtype);
        return rrSet;
    }

    @Test
    public void dirPoolInZonePresent() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(GET_DIRECTIONAL_POOLS_OF_ZONE));
        server.enqueue(new MockResponse());

        assertThat(mockApi().getDirectionalPoolsOfZone("test-zone-1.com.").getRrSets()
                .get(0).getOwnerName())
                .isEqualTo("dir_pool_1.test-zone-1.com.");

        server.assertSessionRequest();
        server.assertRequest("GET", "/zones/test-zone-1.com./rrsets/?q=kind%3ADIR_POOLS", "");
    }

    @Test
    public void dirPoolInZoneAbsent() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(RR_SET_ABSENT));
        server.enqueue(new MockResponse());

        assertThat(mockApi().getDirectionalPoolsOfZone("denominator.io.").getRrSets()).isEmpty();

        server.assertSessionRequest();
        server.assertRequest("GET", "/zones/denominator.io./rrsets/?q=kind%3ADIR_POOLS", "");
    }

    @Test
    public void directionalRecordsByNameAndTypePresent() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(GET_DIRECTIONAL_POOLS_OF_ZONE));
        server.enqueue(new MockResponse());

        List<DirectionalRecord> records = RRSetUtil.buildDirectionalRecords(mockApi().getDirectionalDNSRecordsForHost(
                "test-zone-1.com.", "dir_pool_1.test-zone-1.com.", 1).rrSets());

        assertThat(records).extracting("name", "geoGroupName", "ipGroupName", "noResponseRecord")
                .containsExactly(
                        tuple("dir_pool_1.test-zone-1.com.", "GroupB", "GroupA", false),
                        tuple("dir_pool_1.test-zone-1.com.", null, null, false),
                        tuple("dir_pool_1.test-zone-1.com.", "GroupC", "GroupD", false),
                        tuple("dir_pool_1.test-zone-1.com.", "GroupE", "GroupF", false),
                        tuple("dir_pool_1.test-zone-1.com.", "GroupN", null, true)
                );
        assertThat(records).extracting("type", "ttl", "rdata")
                .containsExactly(
                        tuple(ResourceTypes.A.name(), TTL_86400, asList("1.1.1.1")),
                        tuple(ResourceTypes.A.name(), TTL_50, asList("2.2.2.2")),
                        tuple(ResourceTypes.A.name(), TTL_100, asList("3.3.3.3")),
                        tuple(ResourceTypes.A.name(), TTL_122, asList("6.6.6.6")),
                        tuple(ResourceTypes.A.name(), 0, asList("No Data Response"))
                );

        server.assertSessionRequest();
        server.assertRequest("GET", "/zones/test-zone-1.com./rrsets/1/dir_pool_1.test-zone-1.com.?q=kind%3ADIR_POOLS",
                "");
    }

    @Test
    public void directionalRecordsByNameAndTypeAbsent() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(RR_SET_ABSENT));
        server.enqueue(new MockResponse());

        assertThat(mockApi().getDirectionalDNSRecordsForHost("denominator.io.",
                "dir_pool_1_www.denominator.io.", 1)
                .getRrSets()).isEmpty();

        server.assertSessionRequest();
        server.assertRequest("GET", "/zones/denominator.io./rrsets/1/dir_pool_1_www.denominator.io.?q=kind%3ADIR_POOLS"
                , "");
    }

    @Test
    public void createDirectionalPoolInZoneForNameAndType() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));
        server.enqueue(new MockResponse());

        assertThat(mockApi().addDirectionalPool("test-zone-1.com.", "dir_pool_1.test-zone-1.com.", ResourceTypes.A.name())
                .getMessage()).isEqualTo("Successful");

        server.assertSessionRequest();
        server.assertRequest("POST",
                "/zones/test-zone-1.com./rrsets/A/dir_pool_1.test-zone-1.com.",
                "{\"profile\": {\"@context\": \"http://schemas.ultradns.com/DirPool.jsonschema\"," +
                        " \"description\": \"A\"}}");
    }

    @Test
    public void createDirectionalPoolInZoneForNameAndTypeWhenAlreadyExists() throws Exception {
        thrown.expect(UltraDNSRestException.class);

        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setResponseCode(SC_BAD_REQUEST).setBody(UltraDNSMockResponse.
                getMockErrorResponse(UltraDNSRestException.POOL_ALREADY_EXISTS,
                        "Pool already created for this host name  :  dir_pool_2.test-zone-1.com.")));

        mockApi().addDirectionalPool("test-zone-1.com.", "dir_pool_1.test-zone-1.com.", ResourceTypes.A.name());
    }


    @Test
    public void testDeleteResourceRecordByNameType() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));
        server.enqueue(new MockResponse());

        assertThat(mockApi().deleteResourceRecordByNameType("test-zone-1.com.", 1, "pool_1.test-zone-1.com.")
                .getMessage()).isEqualTo("Successful");

        server.assertSessionRequest();
        server.assertRequest("DELETE",
                "/zones/test-zone-1.com./rrsets/1/pool_1.test-zone-1.com.", "");
    }

    @Test
    public void testDeleteResourceRecord() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));
        server.enqueue(new MockResponse());

        assertThat(mockApi().deleteResourceRecord("test-zone-1.com.", 1, "pool_1.test-zone-1.com.", 0)
                .getMessage()).isEqualTo("Successful");

        server.assertSessionRequest();
        server.assertRequest("PATCH",
                "/zones/test-zone-1.com./rrsets/1/pool_1.test-zone-1.com.",
                "[{\"op\": \"remove\", \"path\": \"/rdata/0\"}]");
    }

    static String getAvailableRegionsResponse =
            "[\n" +
                    "  [\n" +
                    "    {\n" +
                    "      \"name\": \"Anonymous Proxy\",\n" +
                    "      \"code\": \"A1\",\n" +
                    "      \"type\": \"Country\",\n" +
                    "      \"id\": 315\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"Satellite Provider\",\n" +
                    "      \"code\": \"A2\",\n" +
                    "      \"type\": \"Country\",\n" +
                    "      \"id\": 316\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"Unknown / Uncategorized IPs\",\n" +
                    "      \"code\": \"A3\",\n" +
                    "      \"type\": \"Country\",\n" +
                    "      \"id\": 331\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"North America\",\n" +
                    "      \"code\": \"NAM\",\n" +
                    "      \"type\": \"Region\",\n" +
                    "      \"id\": 338\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"South America\",\n" +
                    "      \"code\": \"SAM\",\n" +
                    "      \"type\": \"Region\",\n" +
                    "      \"id\": 337\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"Europe\",\n" +
                    "      \"code\": \"EUR\",\n" +
                    "      \"type\": \"Region\",\n" +
                    "      \"id\": 336\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"Africa\",\n" +
                    "      \"code\": \"AFR\",\n" +
                    "      \"type\": \"Region\",\n" +
                    "      \"id\": 332\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"Asia\",\n" +
                    "      \"code\": \"ASI\",\n" +
                    "      \"type\": \"Region\",\n" +
                    "      \"id\": 334\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"Australia / Oceania\",\n" +
                    "      \"code\": \"OCN\",\n" +
                    "      \"type\": \"Region\",\n" +
                    "      \"id\": 335\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"Antarctica\",\n" +
                    "      \"code\": \"ANT\",\n" +
                    "      \"type\": \"Region\",\n" +
                    "      \"id\": 333\n" +
                    "    }\n" +
                    "  ]\n" +
                    "]";

}
