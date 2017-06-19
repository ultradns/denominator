package denominator.ultradns.service;

import denominator.model.Zone;
import denominator.ultradns.service.integration.UltraDNSRest;
import denominator.ultradns.exception.UltraDNSRestException;
import denominator.ultradns.model.AccountList;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.Record;
import denominator.ultradns.util.RRSetUtil;
import denominator.ultradns.util.ZoneUtil;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

import static denominator.common.Preconditions.checkState;
import static denominator.common.Util.singletonIterator;
import static denominator.ultradns.exception.UltraDNSRestException.processUltraDnsException;

import denominator.ResourceTypeToValue.ResourceTypes;
import org.apache.log4j.Logger;

public final class UltraDNSRestZoneApi implements denominator.ZoneApi {

  private final UltraDNSRest api;

  @Inject
  UltraDNSRestZoneApi(UltraDNSRest api) {
    this.api = api;
  }

  private static final Logger LOGGER = Logger.getLogger(UltraDNSRestZoneApi.class);

  /**
   * in UltraDNSRest, zones are scoped to an account.
   */
  @Override
  public Iterator<Zone> iterator() {
    List<Map<String, String>> zoneAccountList = new ArrayList<Map<String, String>>();
    try {
      zoneAccountList = ZoneUtil.getZoneAccountList(api.getZonesOfUser().getZones());
    } catch (UltraDNSRestException e) {
      processUltraDnsException(e, UltraDNSRestException.DATA_NOT_FOUND);
    }

    final Iterator<Map<String, String>> delegate = zoneAccountList.iterator();
    return new Iterator<Zone>() {
      @Override
      public boolean hasNext() {
        return delegate.hasNext();
      }

      @Override
      public Zone next() {
        return fromSOA(delegate.next());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Get Zone details with zone name.
   * @param name name of the zone
   * @return zone An iterator to iterate through Zones
   */
  @Override
  public Iterator<Zone> iterateByName(String name) {
    Zone zone = null;
    try {
      final Map<String, String> zoneAccount = ZoneUtil.getZoneAccount(api.getZoneByName(name));
      zone = fromSOA(zoneAccount);
    } catch (UltraDNSRestException e) {
      processUltraDnsException(e,
              new HashSet<Integer>(Arrays.asList(
                      UltraDNSRestException.ZONE_NOT_FOUND,
                      UltraDNSRestException.INVALID_ZONE_NAME
              )));
    }
    return singletonIterator(zone);
  }

  /**
   * Add or update a zone with email and ttl.
   * @param zone Zone Object
   * @return zone name of the Zone
   */
  @Override
  public String put(Zone zone) {
    try {
      final String accountName = StringUtils.isEmpty(zone.accountName()) ? getCurrentAccountName() : zone.accountName();
      LOGGER.debug("Creating Zone with zone name: " + zone.name() + " and account name: " + accountName);
      api.createPrimaryZone(zone.name(), accountName, "PRIMARY", false, "NEW");
    } catch (UltraDNSRestException e) {
        processUltraDnsException(e, UltraDNSRestException.ZONE_ALREADY_EXISTS);
    }

    RRSet soa = api.getResourceRecordsOfDNameByType(zone.name(), zone.name(),
            ResourceTypes.SOA.code()).getRrSets().get(0);
    soa.setTtl(zone.ttl());
    List<String> rDataList = Arrays.asList(soa.getRdata().get(0).split("\\s"));
    final int emailIndex = 1;
    final int ttlIndex = 6;
    rDataList.set(emailIndex, formatEmail(zone.email()));
    rDataList.set(ttlIndex, String.valueOf(zone.ttl()));
    List<String> newRDataList = new ArrayList<String>();
    newRDataList.add(StringUtils.join(rDataList, " "));
    soa.setRdata(newRDataList);
    LOGGER.debug("Updating records with email: " + rDataList.get(emailIndex) + ", ttl: " + rDataList.get(ttlIndex));
    api.updateResourceRecord(zone.name(), ResourceTypes.SOA.code(), zone.name(), soa);

    return zone.name();
  }

  /**
   * Delete zone with name.
   * @param name name of the Zone
   */
  @Override
  public void delete(String name) {
    try {
      LOGGER.debug("Deleting zone with zone name: " + name);
      api.deleteZone(name);
    } catch (UltraDNSRestException e) {
      processUltraDnsException(e, UltraDNSRestException.ZONE_NOT_FOUND);
    }
  }

  /**
   * Get Zone with SOA records.
   *
   * @param zoneAccount map with account name as key, zone name as value
   * @return zone Zone Object
   */
  private Zone fromSOA(Map<String, String> zoneAccount) {
    final String name = zoneAccount.keySet().iterator().next();
    final String accountName = zoneAccount.get(name);

    List<Record> soaRecords;
    try {
      soaRecords = RRSetUtil.buildRecords(api
              .getResourceRecordsOfDNameByType(name, name, ResourceTypes.SOA.code())
              .rrSets());
    } catch (UltraDNSRestException e) {
      throw e;
    }
    checkState(!soaRecords.isEmpty(), "SOA record for zone %s was not present", name);
    Record soa = soaRecords.get(0);

    Zone zone = Zone.create(name, name, soa.getTtl(), soa.getRdata().get(1));
    zone.setAccountName(accountName);
    return zone;
  }

  /**
   * Return the most recently added account name associated with logged user.
   * This method will be invoked only if client does't pass account name while performing operation zone.
   *
   * @return account name
   */
  private String getCurrentAccountName() {
    AccountList accountList;
    try {
      LOGGER.debug("Retrieving list of accounts for currently logged in user ... ");
      accountList = api.getAccountsListOfUser();
    } catch (UltraDNSRestException e) {
      throw e;
    }
    if (accountList.getAccounts() != null && !accountList.getAccounts().isEmpty()) {
      return accountList.getAccounts()
              .get(accountList.getAccounts().size() - 1)
              .getAccountName();
    }
    return null;
  }

  /**
   * Format the email address like below to save it in rdata.
   * test.email@neustar.biz --> test\.email.neustar.biz.
   * If email is already formatted the it will return as it is.
   *
   * @param email email id
   * @return formatted email
   */
  private String formatEmail(String email) {
    String[] values = email.split("@");
    if (values.length != 1) {
      return values[0].replace(".", "\\.") + "." + values[1] + ".";
    } else {
      return email;
    }
  }
}
