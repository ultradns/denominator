package denominator.ultradns;

import denominator.ultradns.model.Status;
import denominator.ultradns.model.AccountList;
import denominator.ultradns.model.ZoneList;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.RRSetList;
import denominator.ultradns.model.Region;

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
   * Delete a zone given the zone name.
   *
   * @param zoneName The zone that has to be deleted.
   *
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
   * Delete a pool given the zone name, type code and hostname.
   *
   * @param zoneName zone in which the pool is present in.
   * @param typeCode integer value of type of the pool.
   * @param hostName hostname in which the pool is present in.
   *
   * @throws UltraDNSRestException with code {@link UltraDNSRestException#POOL_NOT_FOUND} and {@link
   *                           UltraDNSRestException#RESOURCE_RECORD_NOT_FOUND}.
   */
  @RequestLine("DELETE /zones/{zoneName}/rrsets/{typeCode}/{hostName}")
  void deleteLBPool(@Param("zoneName") String zoneName,
                    @Param("typeCode") int typeCode,
                    @Param("hostName") String hostName);

  /**
   * Get the child regions given an optional comma-separated region codes.
   *
   * @param codes Can be an empty string. Can be a comma-separated list of
   *              region codes. If the codes is empty, the top level
   *              regions are returned.
   * @return A list of list of region objects.
   */
  @RequestLine("GET /geoip/territories?codes={codes}")
  Collection<Collection<Region>> getAvailableRegions(@Param("codes") String codes);

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


  @RequestLine("PUT /zones/{zoneName}/rrsets/{poolRecordType}/{hostName}")
  Status updateDirectionalPool(@Param("zoneName") String zoneName,
                               @Param("hostName") String name,
                               @Param("poolRecordType") String type,
                               RRSet rrSet);

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
