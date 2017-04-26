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

@Module(injects = UltraDNSRestGeoResourceRecordSetApi.Factory.class, complete = false)
public class UltraDNSRestGeoSupport {

  private static final int MAX_GEO_CODE = 100;

  @Provides
  @Named("geo")
  Map<Region, Collection<Region>> regions(UltraDNSRest api) {
    Map<Region, Collection<Region>> availableRegions = new TreeMap<Region, Collection<Region>>();

    Collection<Region> topLevelRegions = buildRegionHierarchyByCallingUltraDNSRest(api);
    for (Region topLevelRegion : topLevelRegions) {
      Map<Region, Collection<Region>> regionHierarchy = RegionUtil.getRegionHierarchy(topLevelRegion);
      for (Map.Entry<Region, Collection<Region>> regionSubregions : regionHierarchy.entrySet()) {
        availableRegions.put(regionSubregions.getKey(), regionSubregions.getValue());
      }
    }
    return availableRegions;
  }

  private Collection<Region> buildRegionHierarchyByCallingUltraDNSRest(UltraDNSRest api2) {
    Collection<Collection<Region>> response = api2.getAvailableRegions("");
    Collection<Region> topLevelRegions = response.iterator().next();
    TreeSet<String> sortedEffectiveCodes = getSortedEffectiveCodes(topLevelRegions);
    String commaSeparatedCodes = getCommaSeparatedEffectiveCodes(sortedEffectiveCodes);

    response = api2.getAvailableRegions(commaSeparatedCodes);
    Iterator<Collection<Region>> responseIterator = response.iterator();
    for (String effectiveCode : sortedEffectiveCodes) {
      Region region1 = regionGiven(effectiveCode, topLevelRegions);
      Collection<Region> secondLevelRegions = responseIterator.next();
      region1.setChildRegions(secondLevelRegions);
    }

    TreeSet<Region> sortedSecondLevelRegions = getSortedSecondLevelRegions(topLevelRegions);
    sortedEffectiveCodes = getSortedEffectiveCodes(sortedSecondLevelRegions);
    String[] sortedEffectiveCodesArr = new String[sortedEffectiveCodes.size()];
    sortedEffectiveCodes.toArray(sortedEffectiveCodesArr);
    // The REST API endpoint for "api.getAvailableRegions(commaSeparatedCodes)" accepts utmost
    // 100 effective codes. So, sending the effective codes in batches of 100.
    for (int minIdx = 0; minIdx < sortedEffectiveCodesArr.length; minIdx += MAX_GEO_CODE) {
      int maxIdx = minIdx + MAX_GEO_CODE;
      if (maxIdx > sortedEffectiveCodesArr.length) {
        maxIdx = sortedEffectiveCodesArr.length - 1;
      }
      String startEffectiveCode = sortedEffectiveCodesArr[minIdx];
      String endEffectiveCode = sortedEffectiveCodesArr[maxIdx];
      TreeSet<String> subSetOfSortedEffectiveCodes = new TreeSet<String>(sortedEffectiveCodes.subSet(startEffectiveCode,
              endEffectiveCode));
      commaSeparatedCodes = getCommaSeparatedEffectiveCodes(subSetOfSortedEffectiveCodes);
      response = api2.getAvailableRegions(commaSeparatedCodes);
      responseIterator = response.iterator();
      for (String effectiveCode : subSetOfSortedEffectiveCodes) {
        Region region1 = regionGiven(effectiveCode, topLevelRegions);
        Collection<Region> thirdLevelRegions = responseIterator.next();
        region1.setChildRegions(thirdLevelRegions);
      }
    }

    return topLevelRegions;
  }

  private TreeSet<Region> getSortedSecondLevelRegions(Collection<Region> topLevelRegions) {
    TreeSet<Region> secondLevelRegions = new TreeSet<Region>();
    for (Region topLevelRegion : topLevelRegions) {
      secondLevelRegions.addAll(topLevelRegion.getChildRegions());
    }
    return secondLevelRegions;
  }

  private Region regionGiven(String effectiveCode, Collection<Region> regions) {
    if (effectiveCode == null || regions == null) {
      return null;
    }
    for (Region region : regions) {
      if (region.getEffectiveCode().equals(effectiveCode)) {
        return region;
      }
      TreeSet<Region> childRegions = region.getChildRegions();
      Region region1 = regionGiven(effectiveCode, childRegions);
      if (region1 != null) {
        return region1;
      }
    }
    return null;
  }

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

  private String getCommaSeparatedEffectiveCodes(Collection<String> sortedEffectiveCodes) {
    String commaSeparatedEffectiveCodes = StringUtils.join(sortedEffectiveCodes, ',');
    return commaSeparatedEffectiveCodes;
  }
}
