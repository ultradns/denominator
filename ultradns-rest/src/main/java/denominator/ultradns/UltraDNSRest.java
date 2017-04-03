package denominator.ultradns;

import denominator.ultradns.model.Status;
import denominator.ultradns.model.AccountList;
import denominator.ultradns.model.ZoneList;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.RRSetList;
import denominator.ultradns.model.Region;
import denominator.ultradns.model.DirectionalGroup;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.Collection;

@Headers("Content-Type: application/json")
interface UltraDNSRest {

  @RequestLine("GET /status")
  Status getNeustarNetworkStatus();

  @RequestLine("GET /accounts")
  AccountList getAccountsListOfUser();

  @RequestLine("GET /accounts/{accountName}/zones")
  ZoneList getZonesOfAccount(@Param("accountName") String accountName);

  @RequestLine("POST /zones")
  @Body("%7B" +
          "\"properties\": %7B" +
            "\"name\": \"{name}\", " +
            "\"accountName\": \"{accountName}\", " +
            "\"type\": \"{type}\"" +
          "%7D, " +
            "\"primaryCreateInfo\": %7B" +
            "\"forceImport\": {forceImport}, " +
            "\"createType\": \"{createType}\"" +
          "%7D" +
        "%7D")
  void createPrimaryZone(@Param("name") final String name,
                         @Param("accountName") final String accountName,
                         @Param("type") final String type,
                         @Param("forceImport") final boolean forceImport,
                         @Param("createType") final String createType);

  /**
   * @throws UltraDNSRestException with code {@link UltraDNSRestException#ZONE_NOT_FOUND}.
   */
  @RequestLine("DELETE /zones/{zoneName}")
  void deleteZone(@Param("zoneName") final String zoneName);

  @RequestLine("GET /zones/{zoneName}/rrsets")
  RRSetList getResourceRecordsOfZone(@Param("zoneName") String zoneName);

  @RequestLine("GET /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  RRSetList getResourceRecordsOfDNameByType(@Param("zoneName") String zoneName,
                                            @Param("hostName") String hostName,
                                            @Param("rrType") int rrType);

  @RequestLine("POST /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  void createResourceRecord(@Param("zoneName") String zoneName,
                            @Param("rrType") int rrType,
                            @Param("hostName") String hostName,
                            RRSet rrSet);

  @RequestLine("PUT /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  void updateResourceRecord(@Param("zoneName") String zoneName,
                            @Param("rrType") int rrType,
                            @Param("hostName") String hostName,
                            RRSet rrSet);

  @RequestLine("PATCH /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  void partialUpdateResourceRecord(@Param("zoneName") String zoneName,
                                   @Param("rrType") int rrType,
                                   @Param("hostName") String hostName,
                                   RRSet rrSet);

  @RequestLine("DELETE /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  Status deleteResourceRecordByNameType(@Param("zoneName") String zoneName,
                                        @Param("rrType") int rrType,
                                        @Param("hostName") String hostName);

  @Headers("Content-Type: application/json-patch+json")
  @RequestLine("PATCH /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  @Body("%5B" +
          "%7B" +
            "\"op\": \"remove\", " +
            "\"path\": \"/rdata/{index}\"" +
          "%7D" +
        "%5D")
  Status deleteResourceRecord(@Param("zoneName") String zoneName,
                              @Param("rrType") int rrType,
                              @Param("hostName") String hostName,
                              @Param("index") int index);

  /**
   * Returns the blah blah FIXME
   *
   * @param zoneName Typically ends with a period character (i.e, `.`).
   * @param typeCode Should be an integer between 1 and 257.
   * @return
   */
  @RequestLine("GET /zones/{zoneName}/rrsets/{typeCode}?q=kind:RD_POOLS")
  RRSetList getLoadBalancingPoolsByZone(@Param("zoneName") String zoneName, @Param("typeCode") int typeCode);

  @RequestLine("POST /zones/{zoneName}/rrsets/{typeCode}/{hostName}")
  @Body("{requestBody}")
  void addRRLBPool(@Param("zoneName") String zoneName,
                   @Param("hostName") String name,
                   @Param("typeCode") int typeCode,
                   @Param("requestBody") String requestBody);

  @RequestLine("PATCH /zones/{zoneName}/rrsets/{typeCode}/{hostName}")
  @Body("%7B" +
          "\"ttl\": {ttl}, " +
          "\"rdata\": [\"{address}\"]" +
        "%7D")
  void addRecordToRRPool(@Param("typeCode") int typeCode,
                         @Param("ttl") int ttl,
                         @Param("address") String address,
                         @Param("hostName") String hostName,
                         @Param("zoneName") String zoneName);

  @RequestLine("PATCH /zones/{zoneName}/rrsets/{typeCode}/{hostName}")
  @Body("%7B" +
          "\"ttl\": {ttlToApply}, " +
          "\"rdata\": {rdata}, " +
          "\"profile\": {profile}" +
        "%7D")
  void updateRecordOfRRPool(@Param("zoneName") String zoneName,
                            @Param("typeCode") int typeCode,
                            @Param("hostName") String hostName,
                            @Param("ttlToApply") int ttlToApply,
                            @Param("rdata") String rdataJson,
                            @Param("profile") String profileJson);

  /**
   * @throws UltraDNSRestException with code {@link UltraDNSRestException#POOL_NOT_FOUND} and {@link
   *                           UltraDNSRestException#RESOURCE_RECORD_NOT_FOUND}.
   */
  @RequestLine("DELETE /zones/{zoneName}/rrsets/{typeCode}/{hostName}")
  void deleteLBPool(@Param("zoneName") String zoneName,
                    @Param("typeCode") int typeCode,
                    @Param("hostName") String hostName);

  /**
   *
   * @param codes Can be an empty string. Can be a comma-separated list of region codes.
   * @return
   */
  @RequestLine("GET /geoip/territories?codes={codes}")
  Collection<Collection<Region>> getAvailableRegions(@Param("codes") String codes);

  @RequestLine("POST")
  @Body("<v01:getDirectionalDNSGroupDetails><GroupId>{GroupId}</GroupId></v01:getDirectionalDNSGroupDetails>")
  DirectionalGroup getDirectionalDNSGroupDetails(@Param("GroupId") String groupId);

  /**
   * @throws UltraDNSRestException with code {@link UltraDNSRestException#POOL_RECORD_ALREADY_EXISTS}.
   */
  @RequestLine("PATCH /zones/{zoneName}/rrsets/{poolRecordType}/{hostName}")
  Status addDirectionalPoolRecord(@Param("zoneName") String zoneName,
                                  @Param("hostName") String hostName,
                                  @Param("poolRecordType") String type,
                                  RRSet rrSet);

  @Headers("Content-Type: application/json-patch+json")
  @RequestLine("PATCH /zones/{zoneName}/rrsets/{poolRecordType}/{hostName}")
  @Body("%5B" +
          "%7B" +
            "\"op\": \"replace\", " +
            "\"path\": \"/profile/rdataInfo/{index}\", " +
            "\"value\": {rDataInfo}" +
          "%7D" +
        "%5D")
  void updateDirectionalPoolRecord(@Param("zoneName") String zoneName,
                                   @Param("hostName") String name,
                                   @Param("poolRecordType") String type,
                                   @Param("rDataInfo") String rDataInfo,
                                   @Param("index") int index);

  @RequestLine("GET /zones/{zoneName}/rrsets/?q=kind:DIR_POOLS")
  RRSetList getDirectionalPoolsOfZone(@Param("zoneName") String zoneName);

  @RequestLine("GET /zones/{zoneName}/rrsets/{poolRecordType}/{hostName}?q=kind:DIR_POOLS")
  RRSetList getDirectionalDNSRecordsForHost(@Param("zoneName") String zoneName,
                                            @Param("hostName") String name,
                                            @Param("poolRecordType") int rrType);

  @RequestLine("POST /zones/{zoneName}/rrsets/{poolRecordType}/{hostName}")
  @Body("%7B" +
            "\"profile\": %7B" +
            "\"@context\": \"http://schemas.ultradns.com/DirPool.jsonschema\", " +
             "\"description\": \"{poolRecordType}\"" +
          "%7D" +
        "%7D")
  Status addDirectionalPool(@Param("zoneName") String zoneName,
                            @Param("hostName") String name,
                            @Param("poolRecordType") String type);

  @Headers("Content-Type: application/json-patch+json")
  @RequestLine("PATCH /zones/{zoneName}/rrsets/{poolRecordType}/{hostName}")
  @Body("%5B" +
          "%7B" +
            "\"op\": \"remove\", " +
            "\"path\": \"/rdata/{index}\"" +
          "%7D, " +
          "%7B" +
            "\"op\": \"remove\", " +
            "\"path\": \"/profile/rdataInfo/{index}\"" +
          "%7D" +
        "%5D")
  void deleteDirectionalPoolRecord(@Param("zoneName") String zoneName,
                                   @Param("hostName") String name,
                                   @Param("poolRecordType") String type,
                                   @Param("index") int index);

  @Headers("Content-Type: application/json-patch+json")
  @RequestLine("PATCH /zones/{zoneName}/rrsets/{poolRecordType}/{hostName}")
  @Body("[" +
          "{" +
            "\"op\": \"remove\", " +
            "\"path\": \"/profile/noResponse\"" +
          "}" +
        "]")
  void deleteDirectionalNoResponseRecord(@Param("zoneName") String zoneName,
                                         @Param("hostName") String name,
                                         @Param("poolRecordType") String type);
}
