package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import denominator.Credentials;
import denominator.ultradns.model.RRSetList;
import feign.Feign;
import org.junit.Rule;
import org.junit.Test;

import denominator.ultradns.InvalidatableTokenProvider.Session;

import java.util.concurrent.atomic.AtomicReference;

import static denominator.ultradns.UltraDNSMockResponse.GET_RESOURCE_RECORDS_ABSENT;
import static denominator.ultradns.UltraDNSMockResponse.GET_RESOURCE_RECORDS_PRESENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class UltraDNSRestTest {

    @Rule
    public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

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

    /**
     * getNeustarNetworkStatus
     * When NetworkStatus is Good
     * @throws Exception
     */
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

    /**
     * getNeustarNetworkStatus - (-)scenario
     * When NetworkStatus is Failed
     * @throws Exception
     */
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
        server.enqueue(new MockResponse().setBody(GET_RESOURCE_RECORDS_ABSENT));
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
        server.enqueue(new MockResponse().setBody(GET_RESOURCE_RECORDS_ABSENT));
        server.enqueue(new MockResponse());

        RRSetList rrSetList = mockApi().getResourceRecordsOfDNameByType("denominator.io.", "pool_2.denominator.io.", 1);
        assertThat(rrSetList.getRrSets().size()).isEqualTo(0);

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/zones/denominator.io./rrsets/1/pool_2.denominator.io.");
    }
}