package denominator.ultradns.util;

import denominator.ultradns.model.Region;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class RegionUtil {
    public static Map<String, Collection<String>> getRegionHierarchy(Region region) {
        Map<String, Collection<String>> regionNameSubRegionNames = new
                TreeMap<String, Collection<String>>();
        Collection<String> subRegionNames = new TreeSet<String>();
        TreeSet<Region> childRegions1 = region.getChildRegions();
        if (region.isCountry()) {
            if (childRegions1 == null || childRegions1.size() == 0) {
                subRegionNames.add(region.getName());
            } else {
                for (Region childRegion : childRegions1) {
                    subRegionNames.add(childRegion.getName());
                }
            }
        } else {
            if (region.isRegion()) {
                subRegionNames.add(region.getName());
            }

            if (childRegions1 != null) {
                for (Region childRegion : childRegions1) {
                    subRegionNames.add(childRegion.getName());
                }

                for (Region childRegion : childRegions1) {
                    Map<String, Collection<String>> childChildRegionHierarchy =
                            getRegionHierarchy(childRegion);
                    for (Map.Entry<String, Collection<String>> entry :
                            childChildRegionHierarchy.entrySet()) {
                        regionNameSubRegionNames.put(
                                entry.getKey(),
                                entry.getValue());
                    }
                }
            }
        }
        if (subRegionNames.size() > 0) {
            regionNameSubRegionNames.put(region.getName(), subRegionNames);
        }
        return regionNameSubRegionNames;
    }

    public static Map<Region, Collection<Region>> getRegionHierarchyAsRegions(
            Region region) {
        Map<Region, Collection<Region>> regionNameSubRegionNames = new
                TreeMap<Region, Collection<Region>>();
        Collection<Region> subRegionNames = new TreeSet<Region>();
        TreeSet<Region> childRegions1 = region.getChildRegions();
        if (region.isCountry()) {
            if (childRegions1 == null || childRegions1.size() == 0) {
                subRegionNames.add(region);
            } else {
                for (Region childRegion : childRegions1) {
                    subRegionNames.add(childRegion);
                }
            }
        } else {
            if (region.isRegion()) {
                subRegionNames.add(region);
            }

            if (childRegions1 != null) {
                for (Region childRegion : childRegions1) {
                    subRegionNames.add(childRegion);
                }

                for (Region childRegion : childRegions1) {
                    Map<Region, Collection<Region>> childChildRegionHierarchy =
                            getRegionHierarchyAsRegions(childRegion);
                    for (Map.Entry<Region, Collection<Region>> entry :
                            childChildRegionHierarchy.entrySet()) {
                        regionNameSubRegionNames.put(
                                entry.getKey(),
                                entry.getValue());
                    }
                }
            }
        }
        if (subRegionNames.size() > 0) {
            regionNameSubRegionNames.put(region, subRegionNames);
        }
        return regionNameSubRegionNames;
    }

}
