package denominator.ultradns.service;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import denominator.ultradns.service.integration.UltraDNSRest;
import denominator.ultradns.model.Region;
import denominator.ultradns.util.RegionUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Support class to all Geo Services & Directional pool record related activity.
 * Provide a hierarchical structure of all available regions.
 */
@Module(injects = UltraDNSRestGeoResourceRecordSetApi.Factory.class, complete = false)
public class UltraDNSRestGeoSupport {

  /**
   * Maximum number of code can pass to ultradns api at a time.
   */
  private static final int MAX_GEO_CODE = 100;
  private static final Logger LOGGER = Logger.getLogger(UltraDNSRestGeoSupport.class);

  /**
   * Return all regions in a hierarchical fashion.
   * @param api UltraDNSRest API
   * @return Map contains key as the region & value as all it's child regions/territories
   */
  @Provides
  @Named("geo")
  Map<Region, Collection<Region>> regions(UltraDNSRest api) {
    Map<Region, Collection<Region>> availableRegions = new TreeMap<Region, Collection<Region>>();

    Collection<Region> topLevelRegions = buildRegionHierarchy(api);

    for (Region topLevelRegion : topLevelRegions) {
      Map<Region, Collection<Region>> regionHierarchy = RegionUtil.getRegionHierarchy(topLevelRegion);
      for (Map.Entry<Region, Collection<Region>> regionSubRegions : regionHierarchy.entrySet()) {
        availableRegions.put(regionSubRegions.getKey(), regionSubRegions.getValue());
      }
    }

    return availableRegions;
  }

  /**
   * Build a collection of region which holds all it's child regions hierarchy.
   *
   * @param ultraApi UltraDNSRest API
   * @return collection of regions
   */
  private Collection<Region> buildRegionHierarchy(UltraDNSRest ultraApi) {
    // Level 1 - building continents & special territories in region hierarchy
    Collection<Collection<Region>> response = ultraApi.getAvailableRegions("");
    Collection<Region> regionHierarchy = response.iterator().next();

    // Level 2 - building countries in region hierarchy
    buildHierarchy(ultraApi, getSortedEffectiveCodes(regionHierarchy), regionHierarchy);

    // Level 3 - building states in region hierarchy
    TreeSet<Region> sortedSecondLevelRegions = getSortedSecondLevelRegions(regionHierarchy);
    buildHierarchy(ultraApi, getSortedEffectiveCodes(sortedSecondLevelRegions), regionHierarchy);

    return regionHierarchy;
  }

  /**
   * Get Child region of region by calling ultra api in batch.
   * At max it can call a fixed number of geo code in api call.
   *
   * @param u UltraDNSRest API
   * @param geoCodes codes of region
   * @param regions this will get updated with its child regions
   */
  private void buildHierarchy(UltraDNSRest u, TreeSet<String> geoCodes, Collection<Region> regions) {
    String[] geoCodesArray = new String[geoCodes.size()];
    geoCodes.toArray(geoCodesArray);

    boolean eoi = false;
    for (int lo = 0; lo < geoCodesArray.length; lo += MAX_GEO_CODE) {
      int hi = lo + MAX_GEO_CODE;
      if (hi > geoCodesArray.length) {
        hi = geoCodesArray.length - 1;
        eoi = true;
      }

      TreeSet<String> subGeoCodes = new TreeSet<String>(geoCodes.subSet(geoCodesArray[lo], geoCodesArray[hi]));
      // Adding the last code of geoCodes to last subset for the last batch
      if (eoi) {
        subGeoCodes.add(geoCodesArray[hi]);
      }

      Collection<Collection<Region>> response = u.getAvailableRegions(getCommaSeparatedEffectiveCodes(subGeoCodes));
      Iterator<Collection<Region>> itr = response.iterator();
      for (String effectiveCode : subGeoCodes) {
        Region rg = regionGiven(effectiveCode, regions);
        Collection<Region> regionList = itr.next();
        rg.setChildRegions(regionList);
      }
    }
  }

  /**
   * Gives second level region i.e. countries from region hierarchy.
   *
   * @param regions region hierarchy
   * @return sorted countries
   */
  private TreeSet<Region> getSortedSecondLevelRegions(Collection<Region> regions) {
    TreeSet<Region> secondLevelRegions = new TreeSet<Region>();
    for (Region region : regions) {
      secondLevelRegions.addAll(region.getChildRegions());
    }
    return secondLevelRegions;
  }

  /**
   * Return a region from a collection of regions using effective geo code.
   *
   * @param effectiveCode effective geo code
   * @param regions collection of regions
   * @return Searched region
   */
  private Region regionGiven(String effectiveCode, Collection<Region> regions) {
    if (effectiveCode == null || regions == null) {
      return null;
    }
    for (Region region : regions) {
      if (region.getEffectiveCode().equals(effectiveCode)) {
        return region;
      }
      TreeSet<Region> childRegions = region.getChildRegions();
      Region rg = regionGiven(effectiveCode, childRegions);
      if (rg != null) {
        return rg;
      }
    }
    return null;
  }

  /**
   * Return a sorted list of geo code from a collection of region.
   *
   * @param regions collection of region
   * @return sorted geo code
   */
  private TreeSet<String> getSortedEffectiveCodes(Collection<Region> regions) {
    TreeSet<String> codes = new TreeSet<String>();
    for (Region region : regions) {
      if (region.isRegion() || region.isCountry()) {
        region.setEffectiveCode();
        String effectiveCode = region.getEffectiveCode();
        codes.add(effectiveCode);
      }
    }
    return codes;
  }

  /**
   * Concatenate geo code with comma separator.
   * @param sortedEffectiveCodes geo codes
   * @return comma separated string
   */
  private String getCommaSeparatedEffectiveCodes(Collection<String> sortedEffectiveCodes) {
    String commaSeparatedEffectiveCodes = StringUtils.join(sortedEffectiveCodes, ',');
    return commaSeparatedEffectiveCodes;
  }
}
