package denominator.ultradns.util;

import denominator.ultradns.model.Region;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;

public final class RegionUtil {

    // This class is not supposed to be instantiated.
    private RegionUtil() { }

    /**
     * Return all region names in a hierarchical fashion.
     * @param regions
     * @return Map contains key as the region name & value as all it's child regions/territories names.
     */
    public static Map<String, Collection<String>> getRegionNameHierarchy(Map<Region, Collection<Region>> regions) {
        Map<String, Collection<String>> regionNameSubRegionNames = new TreeMap<String, Collection<String>>();
        for (Map.Entry<Region, Collection<Region>> entry : regions.entrySet()) {
            Collection<String> subRegionNames = new TreeSet<String>();
            Iterator<Region> itr = entry.getValue().iterator();
            while (itr.hasNext()) {
                subRegionNames.add(itr.next().getName());
            }
            regionNameSubRegionNames.put(entry.getKey().getName(), subRegionNames);
        }
        return regionNameSubRegionNames;
    }

    /**
     * Return all regions in a hierarchical fashion.
     * @param region
     * @return Map contains key as the region & value as all it's child regions/territories.
     */
    public static Map<Region, Collection<Region>> getRegionHierarchy(Region region) {
        Map<Region, Collection<Region>> regionSubRegions = new TreeMap<Region, Collection<Region>>();
        Collection<Region> subRegions = new TreeSet<Region>();
        TreeSet<Region> childRegions = region.getChildRegions();

        if (region.isCountry()) {
            if (childRegions == null || childRegions.size() == 0) {
                subRegions.add(region);
            } else {
                for (Region childRegion : childRegions) {
                    subRegions.add(childRegion);
                }
            }
        } else {
            if (region.isRegion()) {
                subRegions.add(region);
            }
            if (childRegions != null) {
                for (Region childRegion : childRegions) {
                    subRegions.add(childRegion);
                }
                for (Region childRegion : childRegions) {
                    Map<Region, Collection<Region>> childChildRegionHierarchy = getRegionHierarchy(childRegion);
                    for (Map.Entry<Region, Collection<Region>> entry : childChildRegionHierarchy.entrySet()) {
                        regionSubRegions.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        if (subRegions.size() > 0) {
            regionSubRegions.put(region, subRegions);
        }
        return regionSubRegions;
    }
}
