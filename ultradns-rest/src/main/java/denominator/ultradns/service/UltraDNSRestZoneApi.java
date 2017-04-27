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

import static denominator.common.Preconditions.checkState;
import static denominator.common.Util.singletonIterator;
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
    final String accountName = getCurrentAccountName();
    List<String> zoneNames = new ArrayList<String>();
    if (accountName != null) {
      zoneNames = ZoneUtil.getZoneNames(api.getZonesOfAccount(accountName).getZones());
    }
    final Iterator<String> delegate = zoneNames.iterator();
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

  @Override
  public Iterator<Zone> iterateByName(String name) {
    Zone zone = null;
    try {
      zone = fromSOA(name);
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.ZONE_NOT_FOUND
          && e.code() != UltraDNSRestException.INVALID_ZONE_NAME) {
        throw e;
      }
    }
    return singletonIterator(zone);
  }

  /**
   * Add or update a zone with email & ttl.
   * @param zone
   * @return zone name
   */
  @Override
  public String put(Zone zone) {
    try {
      String accountName = getCurrentAccountName();
      LOGGER.debug("Creating Zone with zone name: " + zone.name() + " and account name: " + accountName);
      api.createPrimaryZone(zone.name(), accountName, "PRIMARY", false, "NEW");
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.ZONE_ALREADY_EXISTS) {
        throw e;
      }
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
   * @param name
   */
  @Override
  public void delete(String name) {
    try {
      LOGGER.debug("Deleting zone with zone name: " + name);
      api.deleteZone(name);
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.ZONE_NOT_FOUND) {
        throw e;
      }
    }
  }

  /**
   * Add or update a zone with email & ttl.
   * @param name
   * @return Zone
   */
  private Zone fromSOA(String name) {
    List<Record> soas = RRSetUtil.buildRecords(api.getResourceRecordsOfDNameByType(name, name,
            ResourceTypes.SOA.code()).rrSets());
    checkState(!soas.isEmpty(), "SOA record for zone %s was not present", name);
    Record soa = soas.get(0);
    return Zone.create(name, name, soa.getTtl(), soa.getRdata().get(1));
  }

  private String getCurrentAccountName() {
    AccountList accountList = api.getAccountsListOfUser();
    if (accountList.getAccounts() != null && !accountList.getAccounts().isEmpty()) {
      return accountList.getAccounts()
              .get(accountList.getAccounts().size() - 1)
              .getAccountName();
    }
    return null;
  }

  private String formatEmail(String email) {
    String[] values = email.split("@");
    if (values.length != 1) {
      return values[0].replace(".", "\\.") + "." + values[1] + ".";
    } else {
      return email;
    }
  }
}
