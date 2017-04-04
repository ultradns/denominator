package denominator.ultradns.model;

import java.util.ArrayList;
import java.util.List;

public class ZoneList {

    private List<Zone> zones;
    private List<String> zoneNames = new ArrayList<String>();

    public List<Zone> getZones() {
        return zones;
    }

    public void setZones(List<Zone> zones) {
        this.zones = zones;
    }

    public List<String> getZoneNames() {
        if (getZones() != null && !getZones().isEmpty()) {
            for (Zone zone : getZones()) {
                zoneNames.add(zone.getProperties().getName());
            }
        }
        return zoneNames;
    }
}
