package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import denominator.Credentials;
import denominator.ultradns.model.DirectionalRecord;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.RRSetList;
import feign.Feign;
import org.junit.Rule;
import org.junit.Test;

import denominator.ultradns.InvalidatableTokenProvider.Session;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static denominator.ultradns.UltraDNSMockResponse.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class UltraDNSRestTest {

    @Rule
    public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    /**
     * Mock API to call UltraDNSRest Endpoint
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
        tokenProvider.lastCredentialsHashCode = credentials.get().hashCode();
        tokenProvider.token = "token";
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

        assertThat(rrSetList.getRrSets().get(0).getRdata().size()).isEqualTo(7);
        assertThat(rrSetList.getRrSets()).extracting("ownerName", "rrtype", "ttl")
                .containsExactly(
                        tuple("pool_2.denominator.io.", "A (1)", 86400)
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

        assertThat(rrSetList.getRrSets().get(0).getRdata().size()).isEqualTo(7);
        assertThat(rrSetList.getRrSets()).extracting("ownerName", "rrtype", "ttl")
                .containsExactly(
                        tuple("pool_2.denominator.io.", "A (1)", 86400)
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

    private RRSet getSampleRRSet(String ownerName, String rrtype, List<String> rdata){
        RRSet rrSet= new RRSet(86400, rdata);
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

        List<DirectionalRecord> records = mockApi().getDirectionalDNSRecordsForHost(
                "test-zone-1.com.", "dir_pool_1.test-zone-1.com.", 1).buildDirectionalRecords();

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
                        tuple("A", 86400, asList("1.1.1.1")),
                        tuple("A", 50, asList("2.2.2.2")),
                        tuple("A", 100, asList("3.3.3.3")),
                        tuple("A", 122, asList("6.6.6.6")),
                        tuple("A", 0, asList("No Data Response"))
                );

        server.assertSessionRequest();
        server.assertRequest("GET", "/zones/test-zone-1.com./rrsets/1/dir_pool_1.test-zone-1.com.?q=kind%3ADIR_POOLS", "");
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
        server.assertRequest("GET", "/zones/denominator.io./rrsets/1/dir_pool_1_www.denominator.io.?q=kind%3ADIR_POOLS", "");
    }

    @Test
    public void createDirectionalPoolInZoneForNameAndType() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));
        server.enqueue(new MockResponse());

        assertThat(mockApi().addDirectionalPool("test-zone-1.com.", "dir_pool_1.test-zone-1.com.", "A")
                .getMessage()).isEqualTo("Successful");

        server.assertSessionRequest();
        server.assertRequest("POST",
                "/zones/test-zone-1.com./rrsets/A/dir_pool_1.test-zone-1.com.",
                "{\"profile\": {\"@context\": \"http://schemas.ultradns.com/DirPool.jsonschema\",\"description\": \"A\"}}");
    }

    @Test
    public void createDirectionalPoolInZoneForNameAndTypeWhenAlreadyExists() throws Exception {
        thrown.expect(UltraDNSRestException.class);

        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setResponseCode(400).setBody(UltraDNSMockResponse.
                getMockErrorResponse("2912", "Pool already created for this host name  :  dir_pool_2.test-zone-1.com.")));

        mockApi().addDirectionalPool("test-zone-1.com.", "dir_pool_1.test-zone-1.com.", "A");
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
                "[{\"op\": \"remove\",\"path\": \"/rdata/0\"}]");
    }
}