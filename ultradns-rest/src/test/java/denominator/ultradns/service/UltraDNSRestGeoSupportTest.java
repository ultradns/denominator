package denominator.ultradns.service;

import com.squareup.okhttp.mockwebserver.MockResponse;
import denominator.ultradns.MockUltraDNSRestServer;
import denominator.ultradns.UltraDNSMockResponse;
import denominator.ultradns.model.Region;
import denominator.ultradns.service.integration.UltraDNSRest;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;

public class UltraDNSRestGeoSupportTest {

    @Rule
    public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

    /**
     * Mock API to call UltraDNSRest Endpoint.
     * @return
     */
    UltraDNSRest mockApi() {
        return UltraDNSMockResponse.getUltraApi(server);
    }

    @Test
    public void regions() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_REGIONS_RESPONSE_TOP_LEVEL));
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_REGIONS_RESPONSE_SECOND_LEVEL));
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_REGIONS_RESPONSE_THIRD_LEVEL_PART_1));

        UltraDNSRest api = mockApi();
        UltraDNSRestGeoSupport ultraDNSRestGeoSupport =  new UltraDNSRestGeoSupport();
        Map<String, Collection<String>> availableRegions = ultraDNSRestGeoSupport.regions(api);

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/geoip/territories?codes=");
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/geoip/territories?codes=A1%2CA2%2CA3%2CANT%2COCN%2CSAM");
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/geoip/territories?codes=ANT-AQ%2CANT-BV%2CANT-TF%2COCN-AS%2COCN-AU%2COCN-CC%2COCN-CK%2COCN" +
                        "-CX%2COCN-FJ%2COCN-FM%2COCN-GU%2COCN-HM%2COCN-KI%2COCN-MH%2COCN-MP%2COCN-NC%2COCN-NF%2COCN" +
                        "-NR%2COCN-NU%2COCN-NZ%2COCN-PF%2COCN-PG%2COCN-PN%2COCN-PW%2COCN-SB%2COCN-TK%2COCN-TO%2COCN" +
                        "-TV%2COCN-U9%2COCN-UM%2COCN-VU%2COCN-WF%2COCN-WS%2CSAM-AR%2CSAM-BO%2CSAM-BR%2CSAM-CL%2CSAM" +
                       "-CO%2CSAM-EC%2CSAM-FK%2CSAM-GF%2CSAM-GS%2CSAM-GY%2CSAM-PE%2CSAM-PY%2CSAM-SR%2CSAM-U4%2CSAM-UY");
    }

    @Test
    public void regionsAsRegions() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_REGIONS_RESPONSE_TOP_LEVEL));
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_REGIONS_RESPONSE_SECOND_LEVEL));
        server.enqueue(new MockResponse().setBody(GET_AVAILABLE_REGIONS_RESPONSE_THIRD_LEVEL_PART_1));

        UltraDNSRest api = mockApi();
        UltraDNSRestGeoSupport ultraDNSRestGeoSupport =  new UltraDNSRestGeoSupport(api);
        Map<Region, Collection<Region>> availableRegions = ultraDNSRestGeoSupport.regionsAsRegions();

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/geoip/territories?codes=");
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/geoip/territories?codes=A1%2CA2%2CA3%2CANT%2COCN%2CSAM");
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/geoip/territories?codes=ANT-AQ%2CANT-BV%2CANT-TF%2COCN-AS%2COCN-AU%2COCN-CC%2COCN-CK%2COC" +
                        "N-CX%2COCN-FJ%2COCN-FM%2COCN-GU%2COCN-HM%2COCN-KI%2COCN-MH%2COCN-MP%2COCN-NC%2COCN-NF%2COCN" +
                        "-NR%2COCN-NU%2COCN-NZ%2COCN-PF%2COCN-PG%2COCN-PN%2COCN-PW%2COCN-SB%2COCN-TK%2COCN-TO%2COCN" +
                        "-TV%2COCN-U9%2COCN-UM%2COCN-VU%2COCN-WF%2COCN-WS%2CSAM-AR%2CSAM-BO%2CSAM-BR%2CSAM-CL%2CSAM" +
                       "-CO%2CSAM-EC%2CSAM-FK%2CSAM-GF%2CSAM-GS%2CSAM-GY%2CSAM-PE%2CSAM-PY%2CSAM-SR%2CSAM-U4%2CSAM-UY");
    }

    private static final String GET_AVAILABLE_REGIONS_RESPONSE_TOP_LEVEL =
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
                    "      \"name\": \"South America\",\n" +
                    "      \"code\": \"SAM\",\n" +
                    "      \"type\": \"Region\",\n" +
                    "      \"id\": 337\n" +
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

    private static final String GET_AVAILABLE_REGIONS_RESPONSE_SECOND_LEVEL = "[\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Antarctica\",\n" +
            "      \"code\": \"AQ\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 330\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bouvet Island\",\n" +
            "      \"code\": \"BV\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 297\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"French Southern Territories\",\n" +
            "      \"code\": \"TF\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 298\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"American Samoa\",\n" +
            "      \"code\": \"AS\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 284\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Australia\",\n" +
            "      \"code\": \"AU\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 270\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Christmas Island\",\n" +
            "      \"code\": \"CX\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 285\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cocos (Keeling) Islands\",\n" +
            "      \"code\": \"CC\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 286\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cook Islands\",\n" +
            "      \"code\": \"CK\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 287\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Fiji\",\n" +
            "      \"code\": \"FJ\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 272\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"French Polynesia\",\n" +
            "      \"code\": \"PF\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 288\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Guam\",\n" +
            "      \"code\": \"GU\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 289\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Heard Island and McDonald Islands\",\n" +
            "      \"code\": \"HM\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 290\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Kiribati\",\n" +
            "      \"code\": \"KI\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 273\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Marshall Islands\",\n" +
            "      \"code\": \"MH\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 274\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Micronesia , Federated States of\",\n" +
            "      \"code\": \"FM\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 275\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nauru\",\n" +
            "      \"code\": \"NR\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 276\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"New Caledonia\",\n" +
            "      \"code\": \"NC\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 292\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"New Zealand\",\n" +
            "      \"code\": \"NZ\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 271\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Niue\",\n" +
            "      \"code\": \"NU\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 293\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Norfolk Island\",\n" +
            "      \"code\": \"NF\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 303\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Northern Mariana Islands, Commonwealth of\",\n" +
            "      \"code\": \"MP\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 291\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Palau\",\n" +
            "      \"code\": \"PW\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 277\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Papua New Guinea\",\n" +
            "      \"code\": \"PG\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 278\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Pitcairn\",\n" +
            "      \"code\": \"PN\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 294\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Samoa\",\n" +
            "      \"code\": \"WS\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 279\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Solomon Islands\",\n" +
            "      \"code\": \"SB\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 280\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tokelau\",\n" +
            "      \"code\": \"TK\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 295\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tonga\",\n" +
            "      \"code\": \"TO\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 281\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tuvalu\",\n" +
            "      \"code\": \"TV\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 282\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Undefined Australia / Oceania\",\n" +
            "      \"code\": \"U9\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 325\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"United States Minor Outlying Islands\",\n" +
            "      \"code\": \"UM\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 345\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Vanuatu\",\n" +
            "      \"code\": \"VU\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 283\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Wallis and Futuna\",\n" +
            "      \"code\": \"WF\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 296\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Argentina\",\n" +
            "      \"code\": \"AR\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 96\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bolivia\",\n" +
            "      \"code\": \"BO\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 97\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Brazil\",\n" +
            "      \"code\": \"BR\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 98\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Chile\",\n" +
            "      \"code\": \"CL\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 99\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Colombia\",\n" +
            "      \"code\": \"CO\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 100\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ecuador\",\n" +
            "      \"code\": \"EC\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 101\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Falkland Islands\",\n" +
            "      \"code\": \"FK\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 108\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"French Guiana\",\n" +
            "      \"code\": \"GF\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 109\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Guyana\",\n" +
            "      \"code\": \"GY\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 102\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Paraguay\",\n" +
            "      \"code\": \"PY\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 103\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Peru\",\n" +
            "      \"code\": \"PE\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 104\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"South Georgia and the South Sandwich Islands\",\n" +
            "      \"code\": \"GS\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 110\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Suriname\",\n" +
            "      \"code\": \"SR\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 105\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Undefined South America\",\n" +
            "      \"code\": \"U4\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 320\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Uruguay\",\n" +
            "      \"code\": \"UY\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 106\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Venezuela, Bolivarian Republic of\",\n" +
            "      \"code\": \"VE\",\n" +
            "      \"type\": \"Country\",\n" +
            "      \"id\": 107\n" +
            "    }\n" +
            "  ]\n" +
            "]";

    private static final String GET_AVAILABLE_REGIONS_RESPONSE_THIRD_LEVEL_PART_1 = "[\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Eastern\",\n" +
            "      \"code\": \"ET\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 467\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Manu'a\",\n" +
            "      \"code\": \"MA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 468\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Western\",\n" +
            "      \"code\": \"WT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 469\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Australian Capital Territory\",\n" +
            "      \"code\": \"ACT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 479\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"New South Wales\",\n" +
            "      \"code\": \"NSW\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 480\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Northern Territory\",\n" +
            "      \"code\": \"NT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 481\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Queensland\",\n" +
            "      \"code\": \"QLD\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 482\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"South Australia\",\n" +
            "      \"code\": \"SA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 483\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tasmania\",\n" +
            "      \"code\": \"TAS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 484\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Victoria\",\n" +
            "      \"code\": \"VIC\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 485\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Western Australia\",\n" +
            "      \"code\": \"WA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 486\n" +
            "    }\n" +
            "  ],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Central\",\n" +
            "      \"code\": \"C\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1346\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Eastern\",\n" +
            "      \"code\": \"E\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1347\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Northern\",\n" +
            "      \"code\": \"N\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1348\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rotuma\",\n" +
            "      \"code\": \"R\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1349\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Western\",\n" +
            "      \"code\": \"W\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1350\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Chuuk\",\n" +
            "      \"code\": \"TRK\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1353\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Kosrae\",\n" +
            "      \"code\": \"KSA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1351\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Pohnpei\",\n" +
            "      \"code\": \"PNI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1352\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Yap\",\n" +
            "      \"code\": \"YAP\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1354\n" +
            "    }\n" +
            "  ],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Gilbert Islands\",\n" +
            "      \"code\": \"G\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2282\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Line Islands\",\n" +
            "      \"code\": \"L\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2283\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Phoenix Islands\",\n" +
            "      \"code\": \"P\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2284\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Ailinglaplap\",\n" +
            "      \"code\": \"ALL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2706\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ailuk\",\n" +
            "      \"code\": \"ALK\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2705\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Arno\",\n" +
            "      \"code\": \"ARN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2707\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Aur\",\n" +
            "      \"code\": \"AUR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2708\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ebon\",\n" +
            "      \"code\": \"EBO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2709\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Enewetak\",\n" +
            "      \"code\": \"ENI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2710\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Jabat\",\n" +
            "      \"code\": \"JAB\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2711\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Jaluit\",\n" +
            "      \"code\": \"JAL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2712\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Kili\",\n" +
            "      \"code\": \"KIL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2713\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Kwajalein\",\n" +
            "      \"code\": \"KWA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2714\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Lae\",\n" +
            "      \"code\": \"LAE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2715\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Lib\",\n" +
            "      \"code\": \"LIB\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2716\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Likiep\",\n" +
            "      \"code\": \"LIK\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2717\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Majuro\",\n" +
            "      \"code\": \"MAJ\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2718\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Maloelap\",\n" +
            "      \"code\": \"MAL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2719\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Mejit\",\n" +
            "      \"code\": \"MEJ\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2720\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Mili\",\n" +
            "      \"code\": \"MIL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2721\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Namorik\",\n" +
            "      \"code\": \"NMK\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2722\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Namu\",\n" +
            "      \"code\": \"NMU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2723\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rongelap\",\n" +
            "      \"code\": \"RON\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2724\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ujae\",\n" +
            "      \"code\": \"UJA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2725\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Utirik\",\n" +
            "      \"code\": \"UTI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2726\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Wotho\",\n" +
            "      \"code\": \"WTH\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2727\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Wotje\",\n" +
            "      \"code\": \"WTJ\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2728\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Northern Islands\",\n" +
            "      \"code\": \"NI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2860\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rota\",\n" +
            "      \"code\": \"RO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2861\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Saipan\",\n" +
            "      \"code\": \"SA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2862\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tinian\",\n" +
            "      \"code\": \"TI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 2863\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Iles Loyaute\",\n" +
            "      \"code\": \"IL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3006\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nord\",\n" +
            "      \"code\": \"NO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3007\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Sud\",\n" +
            "      \"code\": \"SU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3008\n" +
            "    }\n" +
            "  ],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Aiwo\",\n" +
            "      \"code\": \"01\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3107\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Anabar\",\n" +
            "      \"code\": \"02\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3108\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Anetan\",\n" +
            "      \"code\": \"03\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3109\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Anibare\",\n" +
            "      \"code\": \"04\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3110\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Baiti\",\n" +
            "      \"code\": \"05\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3111\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Boe\",\n" +
            "      \"code\": \"06\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3112\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Buada\",\n" +
            "      \"code\": \"07\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3113\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Denigomodu\",\n" +
            "      \"code\": \"08\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3114\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ewa\",\n" +
            "      \"code\": \"09\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3115\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ijuw\",\n" +
            "      \"code\": \"10\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3116\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Meneng\",\n" +
            "      \"code\": \"11\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3117\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nibok\",\n" +
            "      \"code\": \"12\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3118\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Uaboe\",\n" +
            "      \"code\": \"13\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3119\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Yaren\",\n" +
            "      \"code\": \"14\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3120\n" +
            "    }\n" +
            "  ],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Auckland\",\n" +
            "      \"code\": \"AUK\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3121\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bay Of Plenty\",\n" +
            "      \"code\": \"BOP\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3122\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Canterbury\",\n" +
            "      \"code\": \"CAN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3123\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Chatham Islands Territory\",\n" +
            "      \"code\": \"CIT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3124\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Gisborne District\",\n" +
            "      \"code\": \"GIS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3125\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Hawke's Bay\",\n" +
            "      \"code\": \"HKB\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3126\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Manawatu-Wanganui\",\n" +
            "      \"code\": \"MWT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3128\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Marlborough District\",\n" +
            "      \"code\": \"MBH\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3127\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nelson City\",\n" +
            "      \"code\": \"NSN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3129\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Northland\",\n" +
            "      \"code\": \"NTL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3130\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Otago\",\n" +
            "      \"code\": \"OTA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3131\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Southland\",\n" +
            "      \"code\": \"STL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3132\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Taranaki\",\n" +
            "      \"code\": \"TKI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3134\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tasman District\",\n" +
            "      \"code\": \"TAS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3133\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Waikato\",\n" +
            "      \"code\": \"WKO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3136\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Wellington\",\n" +
            "      \"code\": \"WGN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3135\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"West Coast\",\n" +
            "      \"code\": \"WTC\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3137\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Clipperton Island\",\n" +
            "      \"code\": \"CI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3189\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Iles Du Vent\",\n" +
            "      \"code\": \"WI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3194\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Iles Sous Le Vent\",\n" +
            "      \"code\": \"LI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3190\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Marquises\",\n" +
            "      \"code\": \"MI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3191\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tuamotu-Gambier\",\n" +
            "      \"code\": \"TG\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3192\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tubuai\",\n" +
            "      \"code\": \"TI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3193\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Bougainville\",\n" +
            "      \"code\": \"NSB\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3211\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Central\",\n" +
            "      \"code\": \"CPM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3196\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Chimbu\",\n" +
            "      \"code\": \"CPK\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3195\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"East New Britain\",\n" +
            "      \"code\": \"EBR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3197\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"East Sepik\",\n" +
            "      \"code\": \"ESW\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3200\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Eastern Highlands\",\n" +
            "      \"code\": \"EHG\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3198\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Enga\",\n" +
            "      \"code\": \"EPW\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3199\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Gulf\",\n" +
            "      \"code\": \"GPK\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3201\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Hela\",\n" +
            "      \"code\": \"HE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3202\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Jiwaka\",\n" +
            "      \"code\": \"JI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3203\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Madang\",\n" +
            "      \"code\": \"MPM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3206\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Manus\",\n" +
            "      \"code\": \"MRL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3207\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Milne Bay\",\n" +
            "      \"code\": \"MBA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3204\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Morobe\",\n" +
            "      \"code\": \"MPL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3205\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"National Capital District\",\n" +
            "      \"code\": \"NCD\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3208\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"New Ireland\",\n" +
            "      \"code\": \"NIK\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3209\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Northern\",\n" +
            "      \"code\": \"NPP\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3210\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Sandaun\",\n" +
            "      \"code\": \"SAN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3212\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Southern Highlands\",\n" +
            "      \"code\": \"SHM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3213\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"West New Britain\",\n" +
            "      \"code\": \"WBK\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3214\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Western\",\n" +
            "      \"code\": \"WPD\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3216\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Western Highlands\",\n" +
            "      \"code\": \"WHM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3215\n" +
            "    }\n" +
            "  ],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Aimelik\",\n" +
            "      \"code\": \"002\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3360\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Airai\",\n" +
            "      \"code\": \"004\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3361\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Angaur\",\n" +
            "      \"code\": \"010\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3362\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Hatohobei\",\n" +
            "      \"code\": \"050\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3363\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Kayangel\",\n" +
            "      \"code\": \"100\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3364\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Koror\",\n" +
            "      \"code\": \"150\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3365\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Melekeok\",\n" +
            "      \"code\": \"212\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3366\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ngaraard\",\n" +
            "      \"code\": \"214\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3367\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ngarchelong\",\n" +
            "      \"code\": \"218\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3368\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ngardmau\",\n" +
            "      \"code\": \"222\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3369\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ngatpang\",\n" +
            "      \"code\": \"224\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3370\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ngchesar\",\n" +
            "      \"code\": \"226\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3371\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ngeremlengui\",\n" +
            "      \"code\": \"227\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3372\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ngiwal\",\n" +
            "      \"code\": \"228\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3373\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Peleliu\",\n" +
            "      \"code\": \"350\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3374\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Sonsoro\",\n" +
            "      \"code\": \"370\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3375\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Capital Territory\",\n" +
            "      \"code\": \"CT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3576\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Central\",\n" +
            "      \"code\": \"CE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3574\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Choiseul\",\n" +
            "      \"code\": \"CH\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3575\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Guadalcanal\",\n" +
            "      \"code\": \"GU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3577\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Isabel\",\n" +
            "      \"code\": \"IS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3578\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Makira\",\n" +
            "      \"code\": \"MK\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3579\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Malaita\",\n" +
            "      \"code\": \"ML\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3580\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rennell And Bellona\",\n" +
            "      \"code\": \"RB\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3581\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Temotu\",\n" +
            "      \"code\": \"TE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3582\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Western\",\n" +
            "      \"code\": \"WE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3583\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Atafu\",\n" +
            "      \"code\": \"AT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4079\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Fakaofo\",\n" +
            "      \"code\": \"FA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4080\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nukunonu\",\n" +
            "      \"code\": \"NU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4081\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"'eua\",\n" +
            "      \"code\": \"01\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4125\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ha'apai\",\n" +
            "      \"code\": \"02\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4126\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Niuas\",\n" +
            "      \"code\": \"03\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4127\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tongatapu\",\n" +
            "      \"code\": \"04\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4128\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Vava'u\",\n" +
            "      \"code\": \"05\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4129\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Funafuti\",\n" +
            "      \"code\": \"FUN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4227\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nanumanga\",\n" +
            "      \"code\": \"NMG\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4233\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nanumea\",\n" +
            "      \"code\": \"NMA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4232\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Niutao\",\n" +
            "      \"code\": \"NIT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4228\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nui\",\n" +
            "      \"code\": \"NIU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4229\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nukufetau\",\n" +
            "      \"code\": \"NKF\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4230\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nukulaelae\",\n" +
            "      \"code\": \"NKL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4231\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Vaitupu\",\n" +
            "      \"code\": \"VAI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4234\n" +
            "    }\n" +
            "  ],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Baker Island\",\n" +
            "      \"code\": \"81\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4415\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Howland Island\",\n" +
            "      \"code\": \"84\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4416\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Jarvis Island\",\n" +
            "      \"code\": \"86\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4417\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Johnston Atoll\",\n" +
            "      \"code\": \"67\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4411\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Kingman Reef\",\n" +
            "      \"code\": \"89\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4418\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Midway Islands\",\n" +
            "      \"code\": \"71\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4412\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Navassa Island\",\n" +
            "      \"code\": \"76\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4413\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Palmyra Atoll\",\n" +
            "      \"code\": \"95\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4419\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Wake Island\",\n" +
            "      \"code\": \"79\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4414\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Malampa\",\n" +
            "      \"code\": \"MAP\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4547\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Penama\",\n" +
            "      \"code\": \"PAM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4548\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Sanma\",\n" +
            "      \"code\": \"SAM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4549\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Shefa\",\n" +
            "      \"code\": \"SEE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4550\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tafea\",\n" +
            "      \"code\": \"TAE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4551\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Torba\",\n" +
            "      \"code\": \"TOB\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4552\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Alo\",\n" +
            "      \"code\": \"AL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4553\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Singave\",\n" +
            "      \"code\": \"SI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4554\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Uvea\",\n" +
            "      \"code\": \"UV\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4555\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"A`Ana\",\n" +
            "      \"code\": \"AA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4556\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Aiga-I-Le-Tai\",\n" +
            "      \"code\": \"AL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4557\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Atua\",\n" +
            "      \"code\": \"AT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4558\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Fa`Asaleleaga\",\n" +
            "      \"code\": \"FA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4559\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Gaga`Emauga\",\n" +
            "      \"code\": \"GE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4560\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Gagaifomauga\",\n" +
            "      \"code\": \"GI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4561\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Palauli\",\n" +
            "      \"code\": \"PA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4562\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Satupa`Itea\",\n" +
            "      \"code\": \"SA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4563\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tuamasaga\",\n" +
            "      \"code\": \"TU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4564\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Va`A-O-Fonoti\",\n" +
            "      \"code\": \"VF\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4565\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Vaisigano\",\n" +
            "      \"code\": \"VS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4566\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Buenos Aires\",\n" +
            "      \"code\": \"B\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 444\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Catamarca\",\n" +
            "      \"code\": \"K\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 452\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Chaco\",\n" +
            "      \"code\": \"H\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 450\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Chubut\",\n" +
            "      \"code\": \"U\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 461\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ciudad De Buenos Aires\",\n" +
            "      \"code\": \"C\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 445\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cordoba\",\n" +
            "      \"code\": \"X\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 464\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Corrientes\",\n" +
            "      \"code\": \"W\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 463\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Entre Rios\",\n" +
            "      \"code\": \"E\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 447\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Formosa\",\n" +
            "      \"code\": \"P\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 456\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Jujuy\",\n" +
            "      \"code\": \"Y\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 465\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"La Pampa\",\n" +
            "      \"code\": \"L\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 453\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"La Rioja\",\n" +
            "      \"code\": \"F\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 448\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Mendoza\",\n" +
            "      \"code\": \"M\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 454\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Misiones\",\n" +
            "      \"code\": \"N\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 455\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Neuquen\",\n" +
            "      \"code\": \"Q\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 457\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rio Negro\",\n" +
            "      \"code\": \"R\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 458\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Salta\",\n" +
            "      \"code\": \"A\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 443\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"San Juan\",\n" +
            "      \"code\": \"J\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 451\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"San Luis\",\n" +
            "      \"code\": \"D\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 446\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Santa Cruz\",\n" +
            "      \"code\": \"Z\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 466\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Santa Fe\",\n" +
            "      \"code\": \"S\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 459\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Santiago Del Estero\",\n" +
            "      \"code\": \"G\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 449\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tierra Del Fuego Antartida E Islas Del Atlantico Sur\",\n" +
            "      \"code\": \"V\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 462\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tucuman\",\n" +
            "      \"code\": \"T\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 460\n" +
            "    }\n" +
            "  ],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  [],\n" +
            "  []\n" +
            "]";

}
