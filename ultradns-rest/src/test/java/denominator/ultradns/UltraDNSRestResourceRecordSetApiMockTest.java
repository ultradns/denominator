package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import denominator.ResourceRecordSetApi;
import org.junit.rules.ExpectedException;
import denominator.Credentials;
import feign.Feign;

import denominator.ultradns.InvalidatableTokenProvider.Session;

import java.util.concurrent.atomic.AtomicReference;

public class UltraDNSRestResourceRecordSetApiMockTest {

  @Rule
  public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  /**
   * Mock API to call UltraDNSRest Endpoint
   * @return UltraDNSRest object
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
                    "" + UltraDNSRestException.ZONE_NOT_FOUND,
                    "Zone does not exist in the system.")));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("non-existent-zone.io.");
    // This calls the UltraDNS#getResourceRecordsOfZone().
    api.iterator();
  }

}
