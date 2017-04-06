package denominator.ultradns;

import denominator.model.Zone;
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

public final class UltraDNSRestZoneApi implements denominator.ZoneApi {

  private final UltraDNSRest api;

  @Inject
  UltraDNSRestZoneApi(UltraDNSRest api) {
    this.api = api;
  }

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

  @Override
  public String put(Zone zone) {
    try {
      String accountName = getCurrentAccountName();
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
    api.updateResourceRecord(zone.name(), ResourceTypes.SOA.code(), zone.name(), soa);

    return zone.name();
  }

  @Override
  public void delete(String name) {
    try {
      api.deleteZone(name);
    } catch (UltraDNSRestException e) {
      if (e.code() != UltraDNSRestException.ZONE_NOT_FOUND) {
        throw e;
      }
    }
  }

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
    return values[0].replace(".", "\\.") + "." + values[1] + ".";
  }
}
