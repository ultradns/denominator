package denominator.ultradns;

import denominator.Credentials;
import denominator.ultradns.service.auth.InvalidatableTokenProvider;
import denominator.ultradns.service.auth.SessionTarget;
import denominator.ultradns.service.auth.UltraDNSRestTarget;
import denominator.ultradns.service.decoder.UltraDNSRestErrorDecoder;
import denominator.ultradns.service.integration.UltraDNSRest;
import feign.Feign;

import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;

public final class UltraDNSMockResponse {

    private UltraDNSMockResponse() { }

    public static final int TTL_100 = 100;

    public static final int TTL_200 = 200;

    public static final int TTL_300 = 300;

    public static final int TTL_500 = 500;

    public static final int TTL_86400 = 86400;

    public static final int TTL_3600 = 3600;

    public static final int TTL_2400 = 2400;

    public static final int TTL_4800 = 4800;

    public static final int TTL_122 = 122;

    public static final int TTL_50 = 50;

    public static final int TTL_3601 = 3601;

    public static final int GEO_SUPPORTED_REGIONS_SIZE = 15;

    public static final int RESOURCE_RECORDS_COUNT = 7;

    public static final int RESOURCE_RECORD_TYPE = 7;

    public static final int REGION_CODE_A1 = 315;

    public static final int REGION_CODE_A2 = 316;

    public static final int REGION_CODE_A3 = 331;

    public static final int REGION_CODE_NAM = 338;

    /**
     * Mock API to call UltraDNSRest Endpoint.
     * @return
     */
    public static UltraDNSRest getUltraApi(final MockUltraDNSRestServer server) {
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
        InvalidatableTokenProvider.Session session = feign.newInstance(new SessionTarget(provider));

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

    public static String getMockErrorResponse(int errorCode, String errorMessage) {
        return "[\n" +
                    "{\n" +
                        format("\"errorCode\": %s,\n", errorCode) +
                        format("\"errorMessage\": \"%s\"\n", errorMessage) +
                    "}\n" +
                "]\n";
    }

    public static final String STATUS_SUCCESS = "{\n" +
            "    \"message\": \"Successful\"\n" +
            "}\n";

    public static final String GET_ACCOUNTS_LIST_OF_USER = "{\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 2,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 2\n" +
            "    },\n" +
            "    \"accounts\": [\n" +
            "        {\n" +
            "            \"accountName\": \"npp-rest-test2\",\n" +
            "            \"accountHolderUserName\": \"neustarnpptest2\",\n" +
            "            \"ownerUserName\": \"nppresttest2\",\n" +
            "            \"numberOfUsers\": 1,\n" +
            "            \"numberOfGroups\": 3,\n" +
            "            \"accountType\": \"ORGANIZATION\",\n" +
            "            \"features\": [\n" +
            "                \"ADVDIRECTIONAL\",\n" +
            "                \"DNSSEC\",\n" +
            "                \"MAILFORWARD\",\n" +
            "                \"RECURSIVE\",\n" +
            "                \"REPORTING\",\n" +
            "                \"WEBFORWARD\"\n" +
            "            ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"accountName\": \"npp-rest-test2a\",\n" +
            "            \"accountHolderUserName\": \"neustar eng - npp\",\n" +
            "            \"ownerUserName\": \"npp-rest-test2a\",\n" +
            "            \"numberOfUsers\": 2,\n" +
            "            \"numberOfGroups\": 3,\n" +
            "            \"accountType\": \"ORGANIZATION\",\n" +
            "            \"features\": [\n" +
            "                \"ADVDIRECTIONAL\",\n" +
            "                \"DNSSEC\",\n" +
            "                \"MAILFORWARD\",\n" +
            "                \"WEBFORWARD\"\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    public static final String GET_ACCOUNTS_LIST_OF_USER_RESPONSE = "{\n" +
            "    \"resultInfo\": {\n"
            + "        \"totalCount\": 1,\n"
            + "        \"offset\": 0,\n"
            + "        \"returnedCount\": 1\n"
            + "    },\n"
            + "    \"accounts\": [\n"
            + "        {\n"
            + "            \"accountName\": \"npp-rest-test1\",\n"
            + "            \"accountHolderUserName\": \"neustarnpptest1\",\n"
            + "            \"ownerUserName\": \"nppresttest1\",\n"
            + "            \"numberOfUsers\": 1,\n"
            + "            \"numberOfGroups\": 3,\n"
            + "            \"accountType\": \"ORGANIZATION\",\n"
            + "            \"features\": [\n"
            + "                \"ADVDIRECTIONAL\",\n"
            + "                \"DNSSEC\",\n"
            + "                \"MAILFORWARD\",\n"
            + "                \"MDDI\",\n"
            + "                \"RECURSIVE\",\n"
            + "                \"WEBFORWARD\"\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}";

    public static final String RR_SET_WITH_NO_RECORDS = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"www.denominator.io.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"ttl\": 86400,\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\",\n" +
            "                \"order\": \"ROUND_ROBIN\",\n" +
            "                \"description\": \"1\"\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    public static final String GET_RESOURCE_RECORDS_PRESENT = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"pool_2.denominator.io.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"ttl\": 86400,\n" +
            "            \"rdata\": [\n" +
            "                \"1.1.1.1\",\n" +
            "                \"2.2.2.2\",\n" +
            "                \"3.3.3.3\",\n" +
            "                \"4.4.4.4\",\n" +
            "                \"5.5.5.5\",\n" +
            "                \"6.6.6.6\",\n" +
            "                \"7.7.7.7\"\n" +
            "            ],\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\",\n" +
            "                \"order\": \"ROUND_ROBIN\",\n" +
            "                \"description\": \"1\"\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    public static final String POOL_WITH_FOUR_RESOURCE_RECORDS = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"pool_2.denominator.io.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"ttl\": 86400,\n" +
            "            \"rdata\": [\n" +
            "                \"1.1.1.1\",\n" +
            "                \"2.2.2.2\",\n" +
            "                \"3.3.3.3\",\n" +
            "                \"4.4.4.4\"\n" +
            "            ],\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\",\n" +
            "                \"order\": \"ROUND_ROBIN\",\n" +
            "                \"description\": \"1\"\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    public static final String POOL_WITH_THREE_RESOURCE_RECORDS = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"pool_2.denominator.io.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"ttl\": 86400,\n" +
            "            \"rdata\": [\n" +
            "                \"2.2.2.2\",\n" +
            "                \"3.3.3.3\",\n" +
            "                \"4.4.4.4\"\n" +
            "            ],\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\",\n" +
            "                \"order\": \"ROUND_ROBIN\",\n" +
            "                \"description\": \"1\"\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    public static final String POOL_WITH_TWO_RESOURCE_RECORDS = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"pool_2.denominator.io.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"ttl\": 86400,\n" +
            "            \"rdata\": [\n" +
            "                \"3.3.3.3\",\n" +
            "                \"4.4.4.4\"\n" +
            "            ],\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\",\n" +
            "                \"order\": \"ROUND_ROBIN\",\n" +
            "                \"description\": \"1\"\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    public static final String POOL_WITH_ONE_RESOURCE_RECORDS = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"pool_2.denominator.io.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"ttl\": 86400,\n" +
            "            \"rdata\": [\n" +
            "                \"4.4.4.4\"\n" +
            "            ],\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\",\n" +
            "                \"order\": \"ROUND_ROBIN\",\n" +
            "                \"description\": \"1\"\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    public static final String RR_SET_WITH_ONE_RECORD = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"www.denominator.io.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"ttl\": 3600,\n" +
            "            \"rdata\": [\n" +
            "                \"192.0.2.1\"\n" +
            "            ],\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\",\n" +
            "                \"order\": \"ROUND_ROBIN\",\n" +
            "                \"description\": \"1\"\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    public static final String RR_SET_WITH_NO_AAAA_RECORDS = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"www.denominator.io.\",\n" +
            "            \"rrtype\": \"AAAA (28)\",\n" +
            "            \"ttl\": 3600,\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\",\n" +
            "                \"order\": \"ROUND_ROBIN\",\n" +
            "                \"description\": \"1\"\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    public static final String RR_SET_ABSENT = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 0,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 0\n" +
            "    }\n" +
            "}\n";

    public static final String RR_SET_LIST_WITH_ONE_NS_RECORD = "{\n" +
            "  \"zoneName\": \"denominator.io.\",\n" +
            "  \"rrSets\": [\n" +
            "    {\n" +
            "      \"ownerName\": \"www.denominator.io.\",\n" +
            "      \"rrtype\": \"NS (2)\",\n" +
            "      \"ttl\": 3600,\n" +
            "      \"rdata\": [\n" +
            "        \"ns1.denominator.io.\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"queryInfo\": {\n" +
            "    \"sort\": \"OWNER\",\n" +
            "    \"reverse\": false,\n" +
            "    \"limit\": 100\n" +
            "  },\n" +
            "  \"resultInfo\": {\n" +
            "    \"totalCount\": 1,\n" +
            "    \"offset\": 0,\n" +
            "    \"returnedCount\": 1\n" +
            "  }\n" +
            "}";

    public static final String RR_SET_LIST_WITH_TWO_NS_RECORDS = "{\n" +
            "  \"zoneName\": \"denominator.io.\",\n" +
            "  \"rrSets\": [\n" +
            "    {\n" +
            "      \"ownerName\": \"www.denominator.io.\",\n" +
            "      \"rrtype\": \"NS (2)\",\n" +
            "      \"ttl\": 3600,\n" +
            "      \"rdata\": [\n" +
            "        \"ns1.denominator.io.\",\n" +
            "        \"ns2.denominator.io.\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"queryInfo\": {\n" +
            "    \"sort\": \"OWNER\",\n" +
            "    \"reverse\": false,\n" +
            "    \"limit\": 100\n" +
            "  },\n" +
            "  \"resultInfo\": {\n" +
            "    \"totalCount\": 1,\n" +
            "    \"offset\": 0,\n" +
            "    \"returnedCount\": 1\n" +
            "  }\n" +
            "}";

    public static final String GET_SOA_RESOURCE_RECORDS = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"denominator.io.\",\n" +
            "            \"rrtype\": \"SOA (6)\",\n" +
            "            \"ttl\": 86400,\n" +
            "            \"rdata\": [\n" +
            "                \"pdns1.ultradns.net. arghya\\\\.b.neustar.biz. 2017012518 86400 86400 86400 86400\"\n" +
            "            ]\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    public static final String GET_ZONES_OF_USER_PRESENT = "{\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"NAME\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 2,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 2\n" +
            "    },\n" +
            "    \"zones\": [\n" +
            "        {\n" +
            "            \"properties\": {\n" +
            "                \"name\": \"www.test-zone-1.com.\",\n" +
            "                \"accountName\": \"npp-rest-test1\",\n" +
            "                \"type\": \"PRIMARY\",\n" +
            "                \"dnssecStatus\": \"UNSIGNED\",\n" +
            "                \"status\": \"ACTIVE\",\n" +
            "                \"owner\": \"nppresttest1\",\n" +
            "                \"resourceRecordCount\": 3,\n" +
            "                \"lastModifiedDateTime\": \"2016-12-23T10:45Z\"\n" +
            "            },\n" +
            "            \"registrarInfo\": {\n" +
            "                \"nameServers\": {\n" +
            "                    \"missing\": [\n" +
            "                        \"udns1.ultradns.net.\",\n" +
            "                        \"udns2.ultradns.net.\"\n" +
            "                    ]\n" +
            "                }\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"properties\": {\n" +
            "                \"name\": \"www.test-zone-2.com.\",\n" +
            "                \"accountName\": \"npp-rest-test1\",\n" +
            "                \"type\": \"PRIMARY\",\n" +
            "                \"dnssecStatus\": \"UNSIGNED\",\n" +
            "                \"status\": \"ACTIVE\",\n" +
            "                \"owner\": \"nppresttest1\",\n" +
            "                \"resourceRecordCount\": 3,\n" +
            "                \"lastModifiedDateTime\": \"2017-01-12T11:01Z\"\n" +
            "            },\n" +
            "            \"registrarInfo\": {\n" +
            "                \"nameServers\": {\n" +
            "                    \"missing\": [\n" +
            "                        \"udns1.ultradns.net.\",\n" +
            "                        \"udns2.ultradns.net.\"\n" +
            "                    ]\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}\n";

    public static final String GET_ZONES_OF_USER_ABSENT = "{\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"NAME\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 0,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 0\n" +
            "    },\n" +
            "    \"zones\": [ ]\n" +
            "}";

    public static final String GET_ZONE_BY_NAME = "{\n" +
            "    \"properties\": {\n" +
            "        \"name\": \"denominator.io.\",\n" +
            "        \"accountName\": \"npp-rest-test1\",\n" +
            "        \"type\": \"PRIMARY\",\n" +
            "        \"dnssecStatus\": \"UNSIGNED\",\n" +
            "        \"status\": \"ACTIVE\",\n" +
            "        \"owner\": \"npp-rest-test3a\",\n" +
            "        \"resourceRecordCount\": 7,\n" +
            "        \"lastModifiedDateTime\": \"2017-05-10T18:43Z\"\n" +
            "    },\n" +
            "    \"registrarInfo\": {\n" +
            "        \"nameServers\": {\n" +
            "            \"missing\": [\n" +
            "                \"pdns1.ultradns.net.\",\n" +
            "                \"pdns2.ultradns.net.\",\n" +
            "                \"pdns3.ultradns.org.\",\n" +
            "                \"pdns4.ultradns.org.\",\n" +
            "                \"pdns5.ultradns.info.\",\n" +
            "                \"pdns6.ultradns.co.uk.\"\n" +
            "            ]\n" +
            "        }\n" +
            "    },\n" +
            "    \"inherit\": \"ALL\"\n" +
            "}";

    public static final String GET_DIRECTIONAL_POOLS_OF_ZONE = "{\n" +
            "    \"zoneName\": \"test-zone-1.com.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"dir_pool_1.test-zone-1.com.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"rdata\": [\n" +
            "                \"1.1.1.1\",\n" +
            "                \"2.2.2.2\",\n" +
            "                \"3.3.3.3\",\n" +
            "                \"6.6.6.6\"\n" +
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
            "                                    \"end\": \"10.1.1.10\"\n" +
            "                                }\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ttl\": 86400,\n" +
            "                        \"type\": \"A\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"allNonConfigured\": true,\n" +
            "                        \"ttl\": 50,\n" +
            "                        \"type\": \"A\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"geoInfo\": {\n" +
            "                            \"name\": \"GroupC\",\n" +
            "                            \"codes\": [\n" +
            "                                \"SAM\"\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ipInfo\": {\n" +
            "                            \"name\": \"GroupD\",\n" +
            "                            \"ips\": [\n" +
            "                                {\n" +
            "                                    \"start\": \"20.1.1.1\",\n" +
            "                                    \"end\": \"20.1.1.20\"\n" +
            "                                }\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ttl\": 100,\n" +
            "                        \"type\": \"A\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"geoInfo\": {\n" +
            "                            \"name\": \"GroupE\",\n" +
            "                            \"codes\": [\n" +
            "                                \"EUR\"\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ipInfo\": {\n" +
            "                            \"name\": \"GroupF\",\n" +
            "                            \"ips\": [\n" +
            "                                {\n" +
            "                                    \"start\": \"30.1.1.1\",\n" +
            "                                    \"end\": \"30.1.1.30\"\n" +
            "                                }\n" +
            "                            ]\n" +
            "                        },\n" +
            "                        \"ttl\": 122,\n" +
            "                        \"type\": \"A\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"noResponse\": {\n" +
            "                    \"geoInfo\": {\n" +
            "                        \"name\": \"GroupN\",\n" +
            "                        \"codes\": [\n" +
            "                            \"AFR\"\n" +
            "                        ]\n" +
            "                    },\n" +
            "                    \"ttl\": null\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

}
