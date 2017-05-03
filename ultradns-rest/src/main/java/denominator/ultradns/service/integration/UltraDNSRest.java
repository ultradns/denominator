package denominator.ultradns.service.integration;

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

@Headers({ "Content-Type: application/json", "UltraClient: denominator" })
public interface UltraDNSRest {

  /**
   * Gets neustar network status.
   *
   * @return status
   */
  @RequestLine("GET /status")
  Status getNeustarNetworkStatus();

  /**
   * Gets accounts list of the user.
   *
   * @return account list
   */
  @RequestLine("GET /accounts")
  AccountList getAccountsListOfUser();

  /**
   * Gets zones of the account.
   *
   * @param accountName
   * @return zone list
   */
  @RequestLine("GET /accounts/{accountName}/zones")
  ZoneList getZonesOfAccount(@Param("accountName") String accountName);

  /**
   * Creates primary zone.
   *
   * @param name
   * @param accountName
   * @param type
   * @param forceImport
   * @param createType
   */
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
   * @param zoneName The zone that has to be deleted.
   */
  @RequestLine("DELETE /zones/{zoneName}")
  void deleteZone(@Param("zoneName") final String zoneName);

  /**
   * Get the resource records given the zone name.
   *
   * @param zoneName
   * @return RRSetList object.
   */
  @RequestLine("GET /zones/{zoneName}/rrsets")
  RRSetList getResourceRecordsOfZone(@Param("zoneName") String zoneName);

  /**
   * Get the resource records given the zone name, hostName and rrtype.
   *
   * @param zoneName
   * @param hostName
   * @param rrType
   * @return RRSetList object.
   */
  @RequestLine("GET /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  RRSetList getResourceRecordsOfDNameByType(@Param("zoneName") String zoneName,
                                            @Param("hostName") String hostName,
                                            @Param("rrType") int rrType);

  /**
   * Creates resource record given the zone name, rrtype, hostName and rrset.
   *
   * @param zoneName
   * @param rrType
   * @param hostName
   * @param rrSet
   */
  @RequestLine("POST /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  void createResourceRecord(@Param("zoneName") String zoneName,
                            @Param("rrType") int rrType,
                            @Param("hostName") String hostName,
                            RRSet rrSet);
  /**
   * Updates resource record using PUT given the zone name, rrtype, hostName and rrset .
   *
   * @param zoneName
   * @param rrType
   * @param hostName
   * @param rrSet
   */
  @RequestLine("PUT /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  void updateResourceRecord(@Param("zoneName") String zoneName,
                            @Param("rrType") int rrType,
                            @Param("hostName") String hostName,
                            RRSet rrSet);

  /**
   * Updates resource record using PATCH given the zone name, rrtype, hostName and rrset.
   *
   * @param zoneName
   * @param rrType
   * @param hostName
   * @param rrSet
   */
  @RequestLine("PATCH /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  void partialUpdateResourceRecord(@Param("zoneName") String zoneName,
                                   @Param("rrType") int rrType,
                                   @Param("hostName") String hostName,
                                   RRSet rrSet);
  /**
   * Delete the resource record given the zone name, rrtype and hostName.
   *
   * @param zoneName
   * @param rrType
   * @param hostName
   * @return status.
   */
  @RequestLine("DELETE /zones/{zoneName}/rrsets/{rrType}/{hostName}")
  Status deleteResourceRecordByNameType(@Param("zoneName") String zoneName,
                                        @Param("rrType") int rrType,
                                        @Param("hostName") String hostName);

  /**
   * Delete the resource record given the zone name, rrtype and hostName and index.
   *
   * @param zoneName
   * @param rrType
   * @param hostName
   * @param index
   * @return status.
   */
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

  /**
   * Get the directional pools given the zone name.
   *
   * @param zoneName Can be an empty string. Can be a comma-separated list of
   *              region codes. If the codes is empty, the top level
   *              regions are returned.
   * @return RRSetList object.
   */
  @RequestLine("GET /zones/{zoneName}/rrsets/?q=kind:DIR_POOLS")
  RRSetList getDirectionalPoolsOfZone(@Param("zoneName") String zoneName);

  @RequestLine("GET /zones/{zoneName}/rrsets/{poolRecordType}/{hostName}?q=kind:DIR_POOLS")
  RRSetList getDirectionalDNSRecordsForHost(@Param("zoneName") String zoneName,
                                            @Param("hostName") String name,
                                            @Param("poolRecordType") int rrType);

  /**
   * Adds the directional pool given the zone name,owner name and type.
   *
   * @param zoneName
   * @param name hostName
   * @param type poolRecordType
   * @return status
   */
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

  /**
   * Update the directional pool given the zone name,owner name and type.
   *
   * @param zoneName
   * @param name hostName
   * @param type poolRecordType
   * @return status
   */
  @RequestLine("PUT /zones/{zoneName}/rrsets/{poolRecordType}/{hostName}")
  Status updateDirectionalPool(@Param("zoneName") String zoneName,
                               @Param("hostName") String name,
                               @Param("poolRecordType") String type,
                               RRSet rrSet);

  /**
   * Delete the directional pool record given the zone name,owner name and type.
   *
   * @param zoneName
   * @param name hostName
   * @param type poolRecordType
   * @param index
   */
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

  /**
   * Delete  no response directional pool record given the zone name,owner name and type.
   *
   * @param zoneName
   * @param name hostName
   * @param type poolRecordType
   */
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
