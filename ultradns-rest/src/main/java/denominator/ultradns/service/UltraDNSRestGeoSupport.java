package denominator.ultradns.service;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import denominator.ultradns.service.integration.UltraDNSRest;
import denominator.ultradns.model.DirectionalGroup;
import denominator.ultradns.model.Region;
import denominator.ultradns.util.RRSetUtil;
import denominator.ultradns.util.RegionUtil;
import org.apache.commons.lang.StringUtils;

@Module(injects = UltraDNSRestGeoResourceRecordSetApi.Factory.class, complete = false)
public class UltraDNSRestGeoSupport {

  private UltraDNSRest api = null;

  static final int MAX_EFFECTIVE_CODES_PER_API_CALL = 100;

  public UltraDNSRestGeoSupport() { }

  public UltraDNSRestGeoSupport(UltraDNSRest api) {
    this.api = api;
  }

  @Provides
  @Named("geo")
  Map<String, Collection<String>> regions(UltraDNSRest api1) {
    Map<String, Collection<String>> availableRegions = new TreeMap<String, Collection<String>>();

    Collection<Region> topLevelRegions = buildRegionHierarchyByCallingUltraDNSRest(api1);
    for (Region topLevelRegion : topLevelRegions) {
      Map<String, Collection<String>> regionHierarchy = RegionUtil.getRegionHierarchyNames(topLevelRegion);
      for (Map.Entry<String, Collection<String>> regionSubregions : regionHierarchy.entrySet()) {
        availableRegions.put(regionSubregions.getKey(), regionSubregions.getValue());
      }
    }
    return availableRegions;
  }

  Map<Region, Collection<Region>> regionsAsRegions() {
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
    for (int minIdx = 0; minIdx < sortedEffectiveCodesArr.length; minIdx += MAX_EFFECTIVE_CODES_PER_API_CALL) {
      int maxIdx = minIdx + MAX_EFFECTIVE_CODES_PER_API_CALL;
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

  public DirectionalGroup getDirectionalDNSGroupByName(String zoneName, String hostName, int rrType, String groupName) {

    TreeSet<String> codes = RRSetUtil.getDirectionalGroupDetails(
            api.getDirectionalDNSRecordsForHost(zoneName, hostName, rrType).rrSets(),
            groupName);

    Map<String, Collection<String>> regionToTerritories = new TreeMap<String, Collection<String>>();

    if (codes != null && !codes.isEmpty()) {
      regionToTerritories = getRegionToTerritories(codes);
    }

    DirectionalGroup directionalGroup = new DirectionalGroup();
    directionalGroup.setName(groupName);
    directionalGroup.setRegionToTerritories(regionToTerritories);

    return  directionalGroup;
  }

  private Map<String, Collection<String>> getRegionToTerritories(TreeSet<String> codes) {

    Map<Region, Collection<Region>> regions = regionsAsRegions();
    Map<String, Collection<String>> regionToTerritories = new TreeMap<String, Collection<String>>();

    Iterator<String> iterator = codes.iterator();
    while (iterator.hasNext()) {
      String code = iterator.next();
      Collection<String> list = new ArrayList<String>();
      boolean codeFound = false;

      if ("A1".equals(code) || "A2".equals(code) || "A3".equals(code)) {
        for (Region region : regions.keySet()) {
          if (region.getCode().equals(code)) {
            list.add(region.getName());
            if ("A1".equals(code) || "A2".equals(code)) {
              regionToTerritories.put(region.getName() + " (" + code + ")", list);
            } else {
              regionToTerritories.put(region.getName(), list);
            }
          }
        }
      } else {
        for (Map.Entry<Region, Collection<Region>> entry : regions.entrySet()) {
          for (Region region : entry.getValue()) {
            if (region.getEffectiveCodeForGeo().equals(code)) {
              if (regionToTerritories.keySet().contains(entry.getKey().getName())) {
                list = regionToTerritories.get(entry.getKey().getName());
              }
              list.add(region.getName());
              regionToTerritories.put(entry.getKey().getName(), list);
              codeFound = true;
              break;
            }
          }
          if (codeFound) {
            break;
          }
        }
      }
    }
    return regionToTerritories;
  }

  public TreeSet<String> getTerritoryCodes(Map<String, Collection<String>> regionToTerritories) {
    final TreeSet<String> territoryCodes = new TreeSet<String>();
    final Set<Region> regions = new TreeSet<Region>();
    final Set<String> regionNames = new TreeSet<String>();

    for (Map.Entry<Region, Collection<Region>> entry : regionsAsRegions().entrySet()) {
      regions.add(entry.getKey());
      regions.addAll(entry.getValue());
    }

    for (Map.Entry<String, Collection<String>> regionToTerritory : regionToTerritories.entrySet()) {
      regionNames.addAll(regionToTerritory.getValue());
    }

    Iterator<String> regionNamesIterator = regionNames.iterator();
    while (regionNamesIterator.hasNext()) {
      String regionName = regionNamesIterator.next();
      Iterator<Region> regionsIterator = regions.iterator();
      while (regionsIterator.hasNext()) {
        Region region = regionsIterator.next();
        if (regionName.equals(region.getName())) {
          territoryCodes.add(region.getEffectiveCodeForGeo());
          break;
        }
      }
    }
  return territoryCodes;
  }
}