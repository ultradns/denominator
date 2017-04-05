package denominator.ultradns.util;

import denominator.ultradns.model.Zone;

import java.util.ArrayList;
import java.util.List;

public final class ZoneUtil {

    private ZoneUtil() { }

    /**
     * Extract zone names from zone list
     * @param zones
     * @return List of zone name
     */
    public static List<String> getZoneNames(List<Zone> zones) {
        List<String> zoneNames = new ArrayList<String>();
        if (zones != null && !zones.isEmpty()) {
            for (Zone zone : zones) {
                zoneNames.add(zone.getProperties().getName());
            }
        }
        return zoneNames;
    }
}
