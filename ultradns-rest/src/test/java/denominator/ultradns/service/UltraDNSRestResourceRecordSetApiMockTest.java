package denominator.ultradns.service;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.AData;
import denominator.ultradns.MockUltraDNSRestServer;
import denominator.ultradns.UltraDNSMockResponse;
import denominator.ultradns.exception.UltraDNSRestException;
import org.junit.Rule;
import org.junit.Test;

import denominator.ResourceRecordSetApi;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

import denominator.ResourceTypeToValue.ResourceTypes;

import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.ultradns.UltraDNSMockResponse.GET_RESOURCE_RECORDS_PRESENT;
import static denominator.ultradns.UltraDNSMockResponse.STATUS_SUCCESS;
import static denominator.ultradns.UltraDNSMockResponse.POOL_WITH_ONE_RESOURCE_RECORDS;
import static denominator.ultradns.UltraDNSMockResponse.RR_SET_LIST_WITH_ONE_NS_RECORD;
import static denominator.ultradns.UltraDNSMockResponse.TTL_86400;
import static denominator.ultradns.UltraDNSMockResponse.TTL_3600;
import static denominator.ultradns.UltraDNSMockResponse.TTL_2400;
import static org.assertj.core.api.Assertions.assertThat;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

public class UltraDNSRestResourceRecordSetApiMockTest {

  @Rule
  public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void listWhenNoneMatch() throws Exception {
    thrown.expect(UltraDNSRestException.class);
    thrown.expectMessage("Zone does not exist in the system.");

    server.enqueueSessionResponse();
    server.enqueue(new MockResponse()
            .setResponseCode(SC_NOT_FOUND)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.ZONE_NOT_FOUND,
                    "Zone does not exist in the system.")));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("non-existent-zone.io.");
    // This calls the UltraDNS#getResourceRecordsOfZone().
    api.iterator();
  }

  @Test
  public void listWhenThereAreMatches() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(GET_RESOURCE_RECORDS_PRESENT));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("denominator.io.");
    api.iterator();

    server.assertSessionRequest();
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets");
  }

  @Test
  public void iterateByNameWhenNoneMatch() throws Exception {
    thrown.expect(UltraDNSRestException.class);
    thrown.expectMessage("Data not found.");

    server.enqueueSessionResponse();
    server.enqueue(new MockResponse()
            .setResponseCode(SC_NOT_FOUND)
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
            "pool_2.denominator.io.", TTL_86400,
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
            .setResponseCode(SC_NOT_FOUND)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.DATA_NOT_FOUND,
                    "Data not found.")));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("denominator.io.");
    // This calls the UltraDNS#getResourceRecordsOfDNameByType().
    api.getByNameAndType("www.denominator.io.", ResourceTypes.A.name());
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
            "pool_2.denominator.io.", TTL_86400,
            Arrays.asList("1.1.1.1", "2.2.2.2", "3.3.3.3", "4.4.4.4",
                    "5.5.5.5", "6.6.6.6", "7.7.7.7"));
    assertThat(api.getByNameAndType("pool_2.denominator.io.", ResourceTypes.A.name()))
            .isEqualTo(aDataResourceRecordSet);
    server.assertSessionRequest();
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/1/pool_2.denominator.io.");
  }

  @Test
  public void getByNameAndTypeWhenZoneAbsent() throws Exception {
    thrown.expect(UltraDNSRestException.class);
    thrown.expectMessage("Zone does not exist in the system.");

    server.enqueueSessionResponse();
    // Response to the request to get the RR Sets in the pool.
    server.enqueue(new MockResponse()
            .setResponseCode(SC_NOT_FOUND)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.ZONE_NOT_FOUND,
                    "Zone does not exist in the system.")));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("denominator.io.");
    api.getByNameAndType("www.denominator.io.", ResourceTypes.A.name());
  }

  @Test
  public void putFirstAAAACreatesRoundRobinPoolThenAddsRecordToIt() throws Exception {
    server.enqueueSessionResponse();
    // Response to the request to get the RR Sets in the pool.
    server.enqueue(new MockResponse()
            .setResponseCode(SC_NOT_FOUND)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.DATA_NOT_FOUND,
                    "Data not found.")));
    // Response to the request to create the pool.
    server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));

    ResourceRecordSetApi api = server.connect().api()
            .basicRecordSetsInZone("denominator.io.");
    api.put(aaaa("www.denominator.io.", TTL_3600, "3FFE:0B80:0447:0001:0000:0000:0000:0001"));

    server.assertSessionRequest();

    // Assert request to get the RR Sets in the pool.
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/28/www.denominator.io.");

    // Assert request to create the pool addRRLBPoolRequestBody.
    server.assertRequest()
            .hasMethod("POST")
            .hasPath("/zones/denominator.io./rrsets/28/www.denominator.io.")
            .hasBody("{\n" +
                    "  \"ownerName\": \"www.denominator.io.\",\n" +
                    "  \"rrtype\": \"AAAA\",\n" +
                    "  \"ttl\": 3600,\n" +
                    "  \"rdata\": [\n" +
                    "    \"3FFE:0B80:0447:0001:0000:0000:0000:0001\"\n" +
                    "  ],\n" +
                    "  \"profile\": {\n" +
                    "    \"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\",\n" +
                    "    \"order\": \"ROUND_ROBIN\"\n" +
                    "  }\n" +
                    "}");
  }

  @Test
  public void deleteNonExistentPool() throws Exception {
    server.enqueueSessionResponse();
    // Response to UltraDNSRestResourceRecordSetApi#recordsByNameAndType(name, type)
    server.enqueue(new MockResponse()
            .setResponseCode(SC_NOT_FOUND)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.RESOURCE_RECORD_POOL_NOT_FOUND,
                    "Cannot find resource record data for the input zone, " +
                            "record type and owner combination.")));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    api.deleteByNameAndType("pool_2.denominator.io.", ResourceTypes.A.name());

    server.assertSessionRequest();

    // Assert request to get the RR Sets in the pool.
    server.assertRequest()
            .hasMethod("DELETE")
            .hasPath("/zones/denominator.io./rrsets/1/pool_2.denominator.io.");
  }

  @Test
  public void deleteAnAlreadyDeletedPool01() throws Exception {
    server.enqueueSessionResponse();
    // Response to UltraDNSRestResourceRecordSetApi#recordsByNameAndType(name, type)
    server.enqueue(new MockResponse().setBody(POOL_WITH_ONE_RESOURCE_RECORDS));
    // Response to the request to delete a resource record.
    server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));
    // Response to UltraDNSRest#deleteLBPool(zoneName, hostName, typeCode)
    server.enqueue(new MockResponse()
            .setResponseCode(SC_NOT_FOUND)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.RESOURCE_RECORD_POOL_NOT_FOUND,
                    "Cannot find resource record data for the input zone, " +
                            "record type and owner combination.")));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    api.deleteByNameAndType("pool_2.denominator.io.", ResourceTypes.A.name());

    server.assertSessionRequest();

    // Assert request to get the RR Sets in the pool.
    server.assertRequest()
            .hasMethod("DELETE")
            .hasPath("/zones/denominator.io./rrsets/1/pool_2.denominator.io.");
  }

  @Test
  public void deleteAnAlreadyDeletedPool02() throws Exception {
    server.enqueueSessionResponse();
    // Response to UltraDNSRestResourceRecordSetApi#recordsByNameAndType(name, type)
    server.enqueue(new MockResponse().setBody(POOL_WITH_ONE_RESOURCE_RECORDS));
    // Response to UltraDNSRestResourceRecordSetApi#getResourceRecordsOfDNameByType(zoneName, name, intType)
    server.enqueue(new MockResponse()
            .setResponseCode(SC_NOT_FOUND)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.RESOURCE_RECORD_POOL_NOT_FOUND,
                    "Cannot find resource record data for the input zone, " +
                            "record type and owner combination.")));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    api.deleteByNameAndType("pool_2.denominator.io.", ResourceTypes.A.name());

    server.assertSessionRequest();

    // Assert request to get the RR Sets in the pool.
    server.assertRequest()
            .hasMethod("DELETE")
            .hasPath("/zones/denominator.io./rrsets/1/pool_2.denominator.io.");
  }

  @Test
  public void putFirstNsRecordAddsIt() throws Exception {
    server.enqueueSessionResponse();
    // Response to the request to get the RR Sets in the pool.
    server.enqueue(new MockResponse()
            .setResponseCode(SC_NOT_FOUND)
            .setBody(UltraDNSMockResponse.getMockErrorResponse(
                    UltraDNSRestException.DATA_NOT_FOUND,
                    "Data not found.")));
    // Response to the request to create the pool.
    server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    api.put(ns("www.denominator.io.", TTL_3600, "ns1.denominator.io."));

    server.assertSessionRequest();

    // Assert request to get the NS records
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/2/www.denominator.io.");

    // Assert request to create the NS record
    server.assertRequest()
            .hasMethod("POST")
            .hasPath("/zones/denominator.io./rrsets/2/www.denominator.io.")
            .hasBody("{\n" +
                    "  \"ownerName\": \"www.denominator.io.\",\n" +
                    "  \"rrtype\": \"NS\",\n" +
                    "  \"ttl\": 3600,\n" +
                    "  \"rdata\": [\n" +
                    "    \"ns1.denominator.io.\"\n" +
                    "  ]\n" +
                    "}");
  }

  @Test
  public void putSecondNsRecordAddsIt() throws Exception {
    server.enqueueSessionResponse();
    // Response to the request to get the RR Sets for the owner name.
    server.enqueue(new MockResponse().setBody(RR_SET_LIST_WITH_ONE_NS_RECORD));
    // Response to the request to update the pool.
    server.enqueue(new MockResponse().setBody(STATUS_SUCCESS));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("denominator.io.");
    api.put(ns("www.denominator.io.", TTL_2400, Arrays.asList("ns1.denominator.io.", "ns2.denominator.io.")));

    server.assertSessionRequest();

    // Assert request to get the NS records
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/2/www.denominator.io.");

    // Assert request to update the NS record
    server.assertRequest()
            .hasMethod("PUT")
            .hasPath("/zones/denominator.io./rrsets/2/www.denominator.io.")
            .hasBody("{\n" +
                    "  \"ownerName\": \"www.denominator.io.\",\n" +
                    "  \"rrtype\": \"NS (2)\",\n" +
                    "  \"ttl\": 2400,\n" +
                    "  \"rdata\": [\n" +
                    "    \"ns1.denominator.io.\",\n" +
                    "    \"ns2.denominator.io.\"\n" +
                    "  ]\n" +
                    "}");
  }

}
