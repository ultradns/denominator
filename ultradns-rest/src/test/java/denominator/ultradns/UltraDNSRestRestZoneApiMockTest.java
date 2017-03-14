package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.ultradns.UltraDNSMockResponse.*;
import static denominator.ultradns.UltraDNSRestException.INVALID_ZONE_NAME;

public class UltraDNSRestRestZoneApiMockTest {

  @Rule
  public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(GET_ACCOUNTS_LIST_OF_USER_RESPONSE));
    server.enqueue(new MockResponse().setBody(GET_ZONES_OF_ACCOUNT_PRESENT));
    server.enqueue(new MockResponse().setBody(GET_SOA_RESOURCE_RECORDS));
    server.enqueue(new MockResponse().setBody(GET_SOA_RESOURCE_RECORDS));
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterator()).containsExactly(
            Zone.create("www.test-zone-1.com.", "www.test-zone-1.com.", 86400, "arghya\\.b.neustar.biz."),
            Zone.create("www.test-zone-2.com.", "www.test-zone-2.com.", 86400, "arghya\\.b.neustar.biz.")
    );

    server.assertSessionRequest();
    server.assertRequest("GET", "/accounts", "");
    server.assertRequest("GET", "/accounts/npp-rest-test1/zones", "");
    server.assertRequest("GET", "/zones/www.test-zone-1.com./rrsets/6/www.test-zone-1.com.", "");
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(GET_ACCOUNTS_LIST_OF_USER_RESPONSE));
    server.enqueue(new MockResponse().setBody(GET_ZONES_OF_ACCOUNT_ABSENT));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterator()).isEmpty();

    server.assertSessionRequest();
    server.assertRequest("GET", "/accounts", "");
    server.assertRequest("GET", "/accounts/npp-rest-test1/zones", "");
  }

  @Test
  public void iteratorByName() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(GET_SOA_RESOURCE_RECORDS));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io.")).containsExactly(
            Zone.create("denominator.io.", "denominator.io.", 86400, "arghya\\.b.neustar.biz.")
    );

    server.assertSessionRequest();
    server.assertRequest("GET", "/zones/denominator.io./rrsets/6/denominator.io.", "");
  }

  @Test
  public void iteratorByNameWhenNotFound() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(500)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(INVALID_ZONE_NAME, "Invalid zone name.")));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io.")).isEmpty();

    server.assertSessionRequest();
    server.assertRequest("GET", "/zones/denominator.io./rrsets/6/denominator.io.", "");
  }


  @Test
  public void putWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(GET_ACCOUNTS_LIST_OF_USER_RESPONSE));
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse().setBody(GET_SOA_RESOURCE_RECORDS));
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());

    server.assertSessionRequest();
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/accounts");
    server.assertRequest()
            .hasMethod("POST")
            .hasPath("/zones")
            .hasBody("{\"properties\": {\"name\": \"denominator.io.\", \"accountName\": \"npp-rest-test1\", \"type\": \"PRIMARY\"}, " +
                    "\"primaryCreateInfo\": {\"forceImport\": false, \"createType\": \"NEW\"}}");
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/6/denominator.io.");
  }

  @Test
  public void putWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(GET_ACCOUNTS_LIST_OF_USER_RESPONSE));
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse().setBody(GET_SOA_RESOURCE_RECORDS));
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());

    server.enqueue(new MockResponse().setResponseCode(400).setBody(UltraDNSMockResponse
            .getMockErrorResponse(UltraDNSRestException.ZONE_ALREADY_EXISTS,
                    "Zone already exists in the system.")));

    server.assertSessionRequest();
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/accounts");
    server.assertRequest()
            .hasMethod("POST")
            .hasPath("/zones")
            .hasBody("{\"properties\": {\"name\": \"denominator.io.\", \"accountName\": \"npp-rest-test1\", \"type\": \"PRIMARY\"}, " +
                    "\"primaryCreateInfo\": {\"forceImport\": false, \"createType\": \"NEW\"}}");
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/6/denominator.io.");
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");

    server.assertSessionRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/zones/denominator.io.");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueueError(404, UltraDNSRestException.ZONE_NOT_FOUND, "Zone does not exist in the system.");

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");

    server.assertSessionRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/zones/denominator.io.");
  }
}