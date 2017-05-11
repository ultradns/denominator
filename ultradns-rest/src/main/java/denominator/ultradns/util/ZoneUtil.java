package denominator.ultradns.util;

import denominator.ultradns.model.Zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ZoneUtil {

    private ZoneUtil() { }

    /**
     * Extract zone names & account name from zone list.
     * @param zones
     * @return List of zone name with account name
     */
    public static List<Map<String, String>> getZoneAccountList(List<Zone> zones) {
        List<Map<String, String>> zoneAccountList = new ArrayList<Map<String, String>>();
        if (zones != null && !zones.isEmpty()) {
            for (Zone zone : zones) {
                Map<String, String> zoneAccount = new HashMap<String, String>();
                zoneAccount.put(
                        zone.getProperties().getName(),
                        zone.getProperties().getAccountName()
                    );
                zoneAccountList.add(zoneAccount);
            }
        }
        return zoneAccountList;
    }

    /**
     * Extract zone names & account name from a zone.
     * @param zone
     * @return zone name with account name
     */
    public static Map<String, String> getZoneAccount(Zone zone) {
        Map<String, String> zoneAccount = new HashMap<String, String>();
        if (zone != null && zone.getProperties() != null) {
            zoneAccount.put(
                    zone.getProperties().getName(),
                    zone.getProperties().getAccountName()
            );
        }
        return zoneAccount;
    }
}
