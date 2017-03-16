package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import denominator.ultradns.model.Region;
import org.junit.Rule;
import org.junit.Test;

import denominator.Credentials;
import feign.Feign;
import denominator.ultradns.InvalidatableTokenProvider.Session;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static denominator.assertj.ModelAssertions.assertThat;

public class UltraDNSRestGeoSupportTest {

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

    @Test
    public void regions() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(getAvailableRegionsResponseTopLevel));
        server.enqueue(new MockResponse().setBody(getAvailableRegionsResponseSecondLevel));
        server.enqueue(new MockResponse().setBody(getAvailableRegionsResponseThirdLevelPart01));

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
                .hasPath("/geoip/territories?codes=ANT-AQ%2CANT-BV%2CANT-TF%2COCN-AS%2COCN-AU%2COCN-CC%2COCN-CK%2COCN-CX%2COCN-FJ%2COCN-FM%2COCN-GU%2COCN-HM%2COCN-KI%2COCN-MH%2COCN-MP%2COCN-NC%2COCN-NF%2COCN-NR%2COCN-NU%2COCN-NZ%2COCN-PF%2COCN-PG%2COCN-PN%2COCN-PW%2COCN-SB%2COCN-TK%2COCN-TO%2COCN-TV%2COCN-U9%2COCN-UM%2COCN-VU%2COCN-WF%2COCN-WS%2CSAM-AR%2CSAM-BO%2CSAM-BR%2CSAM-CL%2CSAM-CO%2CSAM-EC%2CSAM-FK%2CSAM-GF%2CSAM-GS%2CSAM-GY%2CSAM-PE%2CSAM-PY%2CSAM-SR%2CSAM-U4%2CSAM-UY");
    }

    @Test
    public void regionsAsRegions() throws Exception {
        server.enqueueSessionResponse();
        server.enqueue(new MockResponse().setBody(getAvailableRegionsResponseTopLevel));
        server.enqueue(new MockResponse().setBody(getAvailableRegionsResponseSecondLevel));
        server.enqueue(new MockResponse().setBody(getAvailableRegionsResponseThirdLevelPart01));

        UltraDNSRest api = mockApi();
        UltraDNSRestGeoSupport ultraDNSRestGeoSupport =  new UltraDNSRestGeoSupport();
        Map<Region, Collection<Region>> availableRegions = ultraDNSRestGeoSupport.regionsAsRegions(api);

        server.assertSessionRequest();
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/geoip/territories?codes=");
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/geoip/territories?codes=A1%2CA2%2CA3%2CANT%2COCN%2CSAM");
        server.assertRequest()
                .hasMethod("GET")
                .hasPath("/geoip/territories?codes=ANT-AQ%2CANT-BV%2CANT-TF%2COCN-AS%2COCN-AU%2COCN-CC%2COCN-CK%2COCN-CX%2COCN-FJ%2COCN-FM%2COCN-GU%2COCN-HM%2COCN-KI%2COCN-MH%2COCN-MP%2COCN-NC%2COCN-NF%2COCN-NR%2COCN-NU%2COCN-NZ%2COCN-PF%2COCN-PG%2COCN-PN%2COCN-PW%2COCN-SB%2COCN-TK%2COCN-TO%2COCN-TV%2COCN-U9%2COCN-UM%2COCN-VU%2COCN-WF%2COCN-WS%2CSAM-AR%2CSAM-BO%2CSAM-BR%2CSAM-CL%2CSAM-CO%2CSAM-EC%2CSAM-FK%2CSAM-GF%2CSAM-GS%2CSAM-GY%2CSAM-PE%2CSAM-PY%2CSAM-SR%2CSAM-U4%2CSAM-UY");
    }

    static String getAvailableRegionsResponseTopLevel =
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

    static String getAvailableRegionsResponseSecondLevel = "[\n" +
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

    static String getAvailableRegionsResponseThirdLevelPart01 = "[\n" +
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
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Chuquisaca\",\n" +
            "      \"code\": \"H\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 721\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cochabamba\",\n" +
            "      \"code\": \"C\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 720\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"El Beni\",\n" +
            "      \"code\": \"B\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 719\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"La Paz\",\n" +
            "      \"code\": \"L\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 722\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Oruro\",\n" +
            "      \"code\": \"O\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 724\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Pando\",\n" +
            "      \"code\": \"N\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 723\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Potosi\",\n" +
            "      \"code\": \"P\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 725\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Santa Cruz\",\n" +
            "      \"code\": \"S\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 726\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tarija\",\n" +
            "      \"code\": \"T\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 727\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Acre\",\n" +
            "      \"code\": \"AC\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 731\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Alagoas\",\n" +
            "      \"code\": \"AL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 732\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Amapa\",\n" +
            "      \"code\": \"AP\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 734\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Amazonas\",\n" +
            "      \"code\": \"AM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 733\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bahia\",\n" +
            "      \"code\": \"BA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 735\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ceara\",\n" +
            "      \"code\": \"CE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 736\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Distrito Federal\",\n" +
            "      \"code\": \"DF\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 737\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Espirito Santo\",\n" +
            "      \"code\": \"ES\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 738\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Goias\",\n" +
            "      \"code\": \"GO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 739\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Maranhao\",\n" +
            "      \"code\": \"MA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 740\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Mato Grosso\",\n" +
            "      \"code\": \"MT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 743\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Mato Grosso Do Sul\",\n" +
            "      \"code\": \"MS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 742\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Minas Gerais\",\n" +
            "      \"code\": \"MG\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 741\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Para\",\n" +
            "      \"code\": \"PA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 744\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Paraiba\",\n" +
            "      \"code\": \"PB\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 745\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Parana\",\n" +
            "      \"code\": \"PR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 748\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Pernambuco\",\n" +
            "      \"code\": \"PE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 746\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Piaui\",\n" +
            "      \"code\": \"PI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 747\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rio De Janeiro\",\n" +
            "      \"code\": \"RJ\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 749\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rio Grande Do Norte\",\n" +
            "      \"code\": \"RN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 750\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rio Grande Do Sul\",\n" +
            "      \"code\": \"RS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 753\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rondonia\",\n" +
            "      \"code\": \"RO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 751\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Roraima\",\n" +
            "      \"code\": \"RR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 752\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Santa Catarina\",\n" +
            "      \"code\": \"SC\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 754\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Sao Paulo\",\n" +
            "      \"code\": \"SP\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 756\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Sergipe\",\n" +
            "      \"code\": \"SE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 755\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tocantins\",\n" +
            "      \"code\": \"TO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 757\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Aisen Del General Carlos Ibanez Del Campo\",\n" +
            "      \"code\": \"AI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 918\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Antofagasta\",\n" +
            "      \"code\": \"AN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 919\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Araucania\",\n" +
            "      \"code\": \"AR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 921\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Arica Y Parinacota\",\n" +
            "      \"code\": \"AP\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 920\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Atacama\",\n" +
            "      \"code\": \"AT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 922\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bio-Bio\",\n" +
            "      \"code\": \"BI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 923\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Coquimbo\",\n" +
            "      \"code\": \"CO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 924\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Libertador General Bernardo O'higgins\",\n" +
            "      \"code\": \"LI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 925\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Los Lagos\",\n" +
            "      \"code\": \"LL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 926\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Los Rios\",\n" +
            "      \"code\": \"LR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 927\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Magallanes Y De La Antartica Chilena\",\n" +
            "      \"code\": \"MA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 928\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Maule\",\n" +
            "      \"code\": \"ML\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 929\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Region Metropolitana\",\n" +
            "      \"code\": \"RM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 930\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tarapaca\",\n" +
            "      \"code\": \"TA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 931\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Valparaiso\",\n" +
            "      \"code\": \"VS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 932\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Amazonas\",\n" +
            "      \"code\": \"AMA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 974\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Antioquia\",\n" +
            "      \"code\": \"ANT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 975\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Arauca\",\n" +
            "      \"code\": \"ARA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 976\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Atlantico\",\n" +
            "      \"code\": \"ATL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 977\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bolivar\",\n" +
            "      \"code\": \"BOL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 978\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Boyaca\",\n" +
            "      \"code\": \"BOY\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 979\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Caldas\",\n" +
            "      \"code\": \"CAL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 980\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Caqueta\",\n" +
            "      \"code\": \"CAQ\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 981\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Casanare\",\n" +
            "      \"code\": \"CAS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 982\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cauca\",\n" +
            "      \"code\": \"CAU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 983\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cesar\",\n" +
            "      \"code\": \"CES\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 984\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Choco\",\n" +
            "      \"code\": \"CHO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 985\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cordoba\",\n" +
            "      \"code\": \"COR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 986\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cundinamarca\",\n" +
            "      \"code\": \"CUN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 987\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Distrito Capital\",\n" +
            "      \"code\": \"DC\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 988\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Guainia\",\n" +
            "      \"code\": \"GUA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 989\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Guaviare\",\n" +
            "      \"code\": \"GUV\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 990\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Huila\",\n" +
            "      \"code\": \"HUI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 991\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"La Guajira\",\n" +
            "      \"code\": \"LAG\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 992\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Magdalena\",\n" +
            "      \"code\": \"MAG\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 993\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Meta\",\n" +
            "      \"code\": \"MET\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 994\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Narino\",\n" +
            "      \"code\": \"NAR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 995\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Norte De Santander\",\n" +
            "      \"code\": \"NSA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 996\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Putumayo\",\n" +
            "      \"code\": \"PUT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 997\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Quindio\",\n" +
            "      \"code\": \"QUI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 998\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Risaralda\",\n" +
            "      \"code\": \"RIS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 999\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"San Andres Y Providencia\",\n" +
            "      \"code\": \"SAP\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1001\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Santander\",\n" +
            "      \"code\": \"SAN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1000\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Sucre\",\n" +
            "      \"code\": \"SUC\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1002\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tolima\",\n" +
            "      \"code\": \"TOL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1003\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Valle Del Cauca\",\n" +
            "      \"code\": \"VAC\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1004\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Vaupes\",\n" +
            "      \"code\": \"VAU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1005\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Vichada\",\n" +
            "      \"code\": \"VID\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1006\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Azuay\",\n" +
            "      \"code\": \"A\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1189\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bolivar\",\n" +
            "      \"code\": \"B\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1190\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Canar\",\n" +
            "      \"code\": \"F\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1194\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Carchi\",\n" +
            "      \"code\": \"C\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1191\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Chimborazo\",\n" +
            "      \"code\": \"H\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1196\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cotopaxi\",\n" +
            "      \"code\": \"X\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1210\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"El Oro\",\n" +
            "      \"code\": \"O\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1201\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Esmeraldas\",\n" +
            "      \"code\": \"E\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1193\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Galapagos\",\n" +
            "      \"code\": \"W\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1209\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Guayas\",\n" +
            "      \"code\": \"G\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1195\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Imbabura\",\n" +
            "      \"code\": \"I\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1197\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Loja\",\n" +
            "      \"code\": \"L\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1198\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Los Rios\",\n" +
            "      \"code\": \"R\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1203\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Manabi\",\n" +
            "      \"code\": \"M\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1199\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Morona-Santiago\",\n" +
            "      \"code\": \"S\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1204\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Napo\",\n" +
            "      \"code\": \"N\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1200\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Orellana\",\n" +
            "      \"code\": \"D\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1192\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Pastaza\",\n" +
            "      \"code\": \"Y\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1211\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Pichincha\",\n" +
            "      \"code\": \"P\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1202\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Santa Elena\",\n" +
            "      \"code\": \"SE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1206\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Santo Domingo De Los Tsachilas\",\n" +
            "      \"code\": \"SD\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1205\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Sucumbios\",\n" +
            "      \"code\": \"U\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1208\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tungurahua\",\n" +
            "      \"code\": \"T\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1207\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Zamora-Chinchipe\",\n" +
            "      \"code\": \"Z\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1212\n" +
            "    }\n" +
            "  ],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Cayenne\",\n" +
            "      \"code\": \"CY\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1685\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Saint-Laurent-Du-Maroni\",\n" +
            "      \"code\": \"SL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1686\n" +
            "    }\n" +
            "  ],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Barima-Waini\",\n" +
            "      \"code\": \"BA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1796\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cuyuni-Mazaruni\",\n" +
            "      \"code\": \"CU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1797\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Demerara-Mahaica\",\n" +
            "      \"code\": \"DE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1798\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"East Berbice-Corentyne\",\n" +
            "      \"code\": \"EB\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1799\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Essequibo Islands-West Demerara\",\n" +
            "      \"code\": \"ES\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1800\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Mahaica-Berbice\",\n" +
            "      \"code\": \"MA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1801\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Pomeroon-Supenaam\",\n" +
            "      \"code\": \"PM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1802\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Potaro-Siparuni\",\n" +
            "      \"code\": \"PT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1803\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Upper Demerara-Berbice\",\n" +
            "      \"code\": \"UD\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1804\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Upper Takutu-Upper Essequibo\",\n" +
            "      \"code\": \"UT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 1805\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Amazonas\",\n" +
            "      \"code\": \"AMA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3163\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ancash\",\n" +
            "      \"code\": \"ANC\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3164\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Apurimac\",\n" +
            "      \"code\": \"APU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3165\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Arequipa\",\n" +
            "      \"code\": \"ARE\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3166\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ayacucho\",\n" +
            "      \"code\": \"AYA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3167\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cajamarca\",\n" +
            "      \"code\": \"CAJ\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3168\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Callao\",\n" +
            "      \"code\": \"CAL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3169\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cusco\",\n" +
            "      \"code\": \"CUS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3170\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Huancavelica\",\n" +
            "      \"code\": \"HUV\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3172\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Huanuco\",\n" +
            "      \"code\": \"HUC\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3171\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ica\",\n" +
            "      \"code\": \"ICA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3173\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Junin\",\n" +
            "      \"code\": \"JUN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3174\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"La Libertad\",\n" +
            "      \"code\": \"LAL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3175\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Lambayeque\",\n" +
            "      \"code\": \"LAM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3176\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Lima\",\n" +
            "      \"code\": \"LIM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3177\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Lima Province\",\n" +
            "      \"code\": \"LMA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3178\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Loreto\",\n" +
            "      \"code\": \"LOR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3179\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Madre De Dios\",\n" +
            "      \"code\": \"MDD\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3180\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Moquegua\",\n" +
            "      \"code\": \"MOQ\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3181\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Pasco\",\n" +
            "      \"code\": \"PAS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3182\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Piura\",\n" +
            "      \"code\": \"PIU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3183\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Puno\",\n" +
            "      \"code\": \"PUN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3184\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"San Martin\",\n" +
            "      \"code\": \"SAM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3185\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tacna\",\n" +
            "      \"code\": \"TAC\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3186\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tumbes\",\n" +
            "      \"code\": \"TUM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3187\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Ucayali\",\n" +
            "      \"code\": \"UCA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3188\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Alto Paraguay\",\n" +
            "      \"code\": \"16\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3383\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Alto Parana\",\n" +
            "      \"code\": \"10\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3377\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Amambay\",\n" +
            "      \"code\": \"13\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3380\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Asuncion\",\n" +
            "      \"code\": \"ASU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3393\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Boqueron\",\n" +
            "      \"code\": \"19\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3384\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Caaguazu\",\n" +
            "      \"code\": \"5\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3388\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Caazapa\",\n" +
            "      \"code\": \"6\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3389\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Canindeyu\",\n" +
            "      \"code\": \"14\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3381\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Central\",\n" +
            "      \"code\": \"11\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3378\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Concepcion\",\n" +
            "      \"code\": \"1\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3376\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cordillera\",\n" +
            "      \"code\": \"3\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3386\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Guaira\",\n" +
            "      \"code\": \"4\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3387\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Itapua\",\n" +
            "      \"code\": \"7\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3390\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Misiones\",\n" +
            "      \"code\": \"8\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3391\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Neembucu\",\n" +
            "      \"code\": \"12\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3379\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Paraguari\",\n" +
            "      \"code\": \"9\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3392\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Presidente Hayes\",\n" +
            "      \"code\": \"15\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3382\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"San Pedro\",\n" +
            "      \"code\": \"2\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3385\n" +
            "    }\n" +
            "  ],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Brokopondo\",\n" +
            "      \"code\": \"BR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3915\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Commewijne\",\n" +
            "      \"code\": \"CM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3916\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Coronie\",\n" +
            "      \"code\": \"CR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3917\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Marowijne\",\n" +
            "      \"code\": \"MA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3918\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Nickerie\",\n" +
            "      \"code\": \"NI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3919\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Para\",\n" +
            "      \"code\": \"PR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3921\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Paramaribo\",\n" +
            "      \"code\": \"PM\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3920\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Saramacca\",\n" +
            "      \"code\": \"SA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3922\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Sipaliwini\",\n" +
            "      \"code\": \"SI\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3923\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Wanica\",\n" +
            "      \"code\": \"WA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 3924\n" +
            "    }\n" +
            "  ],\n" +
            "  [],\n" +
            "  [\n" +
            "    {\n" +
            "      \"name\": \"Artigas\",\n" +
            "      \"code\": \"AR\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4420\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Canelones\",\n" +
            "      \"code\": \"CA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4421\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Cerro Largo\",\n" +
            "      \"code\": \"CL\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4422\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Colonia\",\n" +
            "      \"code\": \"CO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4423\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Durazno\",\n" +
            "      \"code\": \"DU\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4424\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Flores\",\n" +
            "      \"code\": \"FS\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4426\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Florida\",\n" +
            "      \"code\": \"FD\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4425\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Lavalleja\",\n" +
            "      \"code\": \"LA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4427\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Maldonado\",\n" +
            "      \"code\": \"MA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4428\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Montevideo\",\n" +
            "      \"code\": \"MO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4429\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Paysandu\",\n" +
            "      \"code\": \"PA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4430\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rio Negro\",\n" +
            "      \"code\": \"RN\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4431\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rivera\",\n" +
            "      \"code\": \"RV\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4433\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Rocha\",\n" +
            "      \"code\": \"RO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4432\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Salto\",\n" +
            "      \"code\": \"SA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4434\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"San Jose\",\n" +
            "      \"code\": \"SJ\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4435\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Soriano\",\n" +
            "      \"code\": \"SO\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4436\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Tacuarembo\",\n" +
            "      \"code\": \"TA\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4437\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Treinta Y Tres\",\n" +
            "      \"code\": \"TT\",\n" +
            "      \"type\": \"State\",\n" +
            "      \"id\": 4438\n" +
            "    }\n" +
            "  ]\n" +
            "]";

}