package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import denominator.ResourceRecordSetApi;
import org.junit.rules.ExpectedException;
import denominator.Credentials;
import feign.Feign;

import denominator.ultradns.InvalidatableTokenProvider.Session;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static denominator.model.ResourceRecordSets.a;
import static denominator.ultradns.UltraDNSMockResponse.*;
import static org.assertj.core.api.Assertions.assertThat;

public class UltraDNSRestResourceRecordSetApiMockTest {

  @Rule
  public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  /**
   * Mock API to call UltraDNSRest Endpoint
   */
  @Before
  public void mockServer() {
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
  }

  @Test
  public void listWhenNoneMatch() throws Exception {
    thrown.expect(UltraDNSRestException.class);
    thrown.expectMessage("Zone does not exist in the system.");

    server.enqueueSessionResponse();
    server.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.ZONE_NOT_FOUND,
                    "Zone does not exist in the system.")));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("non-existent-zone.io.");
    // This calls the UltraDNS#getResourceRecordsOfZone().
    api.iterator();
  }

  @Test
  public void iterateByNameWhenNoneMatch() throws Exception {
    thrown.expect(UltraDNSRestException.class);
    thrown.expectMessage("Data not found.");

    server.enqueueSessionResponse();
    server.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.DATA_NOT_FOUND,
                    "Data not found.")));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("denominator.io.");
    // This calls the UltraDNS#getResourceRecordsOfDNameByType().
    api.iterateByName("non-existent-subdomain.denominator.io.");
  }

  @Test
  public void iterateByNameWhenMatch() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(GET_RESOURCE_RECORDS_PRESENT));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("denominator.io.");
    ResourceRecordSet<AData> aDataResourceRecordSet = a(
            "pool_2.denominator.io.", 86400,
            Arrays.asList("1.1.1.1", "2.2.2.2", "3.3.3.3", "4.4.4.4",
                    "5.5.5.5", "6.6.6.6", "7.7.7.7"));
    assertThat(api.iterateByName("pool_2.denominator.io."))
            .containsExactly(aDataResourceRecordSet);

    server.assertSessionRequest();
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/255/pool_2.denominator.io.");
  }

  @Test
  public void getByNameAndTypeWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.DATA_NOT_FOUND,
                    "Data not found.")));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("denominator.io.");
    // This calls the UltraDNS#getResourceRecordsOfDNameByType().
    api.getByNameAndType("www.denominator.io.", "A");
    server.assertSessionRequest();
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/1/www.denominator.io.");
  }

  @Test
  public void getByNameAndTypeWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(GET_RESOURCE_RECORDS_PRESENT));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("denominator.io.");
    ResourceRecordSet<AData> aDataResourceRecordSet = a(
            "pool_2.denominator.io.", 86400,
            Arrays.asList("1.1.1.1", "2.2.2.2", "3.3.3.3", "4.4.4.4",
                    "5.5.5.5", "6.6.6.6", "7.7.7.7"));
    assertThat(api.getByNameAndType("pool_2.denominator.io.", "A"))
            .isEqualTo(aDataResourceRecordSet);
    server.assertSessionRequest();
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/1/pool_2.denominator.io.");
  }

  @Test
  public void putFirstACreatesRoundRobinPoolThenAddsRecordToIt() throws Exception {
    server.enqueueSessionResponse();
    // Response to the request to get the RR Sets in the pool.
    server.enqueue(new MockResponse()
            .setResponseCode(404)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.DATA_NOT_FOUND,
                    "Data not found.")));
    // Response to the request to create the pool.
    server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));
    // Response to the request to add a record to the pool.
    server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

    server.assertSessionRequest();

    // Assert request to get the RR Sets in the pool.
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/1/www.denominator.io.");

    // Assert request to create the pool.
    String addRRLBPoolRequestBody = "{" +
              "\"ttl\": 300, " +
              "\"rdata\": [], " +
              "\"profile\": {" +
                "\"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\", " +
                "\"order\": \"ROUND_ROBIN\", " +
                "\"description\": \"This is a great RD Pool\"" +
              "}" +
            "}";
    server.assertRequest()
            .hasMethod("POST")
            .hasPath("/zones/denominator.io./rrsets/1/www.denominator.io.")
            .hasBody(addRRLBPoolRequestBody);

    // Assert request to add a record to the pool.
    String addRecordToRRPoolRequestBody = "{" +
              "\"ttl\": 3600, " +
              "\"rdata\": [\"192.0.2.1\"]" +
            "}";
    server.assertRequest()
            .hasMethod("PATCH")
            .hasPath("/zones/denominator.io./rrsets/1/www.denominator.io.")
            .hasBody(addRecordToRRPoolRequestBody);
  }

  @Test
  public void putFirstAReusesExistingEmptyRoundRobinPool() throws Exception {
    server.enqueueSessionResponse();
    // Response to the request to get the RR Sets in the pool.
    server.enqueue(new MockResponse().setBody(RR_SET_WITH_NO_RECORDS));
    // Response to the request to create the pool. It will be a 400 bad
    // request since the pool is already created.
    server.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.POOL_ALREADY_EXISTS,
                    "Pool already created for this host name : " +
                            "www.denominator.io.")));
    // Response to the request to add a record to the pool.
    server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("denominator.io.");
    api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

    server.assertSessionRequest();

    // Assert request to get the RR Sets in the pool.
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/1/www.denominator.io.");

    // Assert request to create the pool.
    String addRRLBPoolRequestBody = "{" +
              "\"ttl\": 300, " +
              "\"rdata\": [], " +
              "\"profile\": {" +
                "\"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\", " +
                "\"order\": \"ROUND_ROBIN\", " +
                "\"description\": \"This is a great RD Pool\"" +
              "}" +
            "}";
    server.assertRequest()
            .hasMethod("POST")
            .hasPath("/zones/denominator.io./rrsets/1/www.denominator.io.")
            .hasBody(addRRLBPoolRequestBody);

    // Assert request to add a record to the pool.
    String addRecordToRRPoolRequestBody = "{" +
              "\"ttl\": 3600, " +
              "\"rdata\": [\"192.0.2.1\"]" +
            "}";
    server.assertRequest()
            .hasMethod("PATCH")
            .hasPath("/zones/denominator.io./rrsets/1/www.denominator.io.")
            .hasBody(addRecordToRRPoolRequestBody);
  }

}
