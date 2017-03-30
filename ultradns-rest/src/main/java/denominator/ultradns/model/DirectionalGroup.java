package denominator.ultradns.model;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class DirectionalGroup {

    private String name;
    private Map<String, Collection<String>> regionToTerritories = new TreeMap<String, Collection<String>>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Collection<String>> getRegionToTerritories() {
        return regionToTerritories;
    }

    public void setRegionToTerritories(Map<String, Collection<String>> regionToTerritories) {
        this.regionToTerritories = regionToTerritories;
    }
}
