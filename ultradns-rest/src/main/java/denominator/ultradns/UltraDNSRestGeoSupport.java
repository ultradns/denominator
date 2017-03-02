package denominator.ultradns;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Named;

import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;
import org.apache.commons.lang.StringUtils;

@Module(injects = UltraDNSRestGeoResourceRecordSetApi.Factory.class, complete = false)
public class UltraDNSRestGeoSupport {

  @Provides
  @Named("geo")
  Map<String, Collection<String>> regions(UltraDNSRest api) {
    Map<String, Collection<String>> availableRegions = new TreeMap<String, Collection<String>>();

    Collection<Collection<Region>> response = api.getAvailableRegions("");
    Collection<Region> topLevelRegions = response.iterator().next();
    TreeSet<String> sortedEffectiveCodes = getSortedEffectiveCodes(topLevelRegions);
    String commaSeparatedCodes = getCommaSeparatedEffectiveCodes(sortedEffectiveCodes);

    response = api.getAvailableRegions(commaSeparatedCodes);
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
    for (int minIdx = 0; minIdx < sortedEffectiveCodesArr.length; minIdx += 100) {
      int maxIdx = minIdx + 100;
      if (maxIdx > sortedEffectiveCodesArr.length) {
        maxIdx = sortedEffectiveCodesArr.length - 1;
      }
      String startEffectiveCode = sortedEffectiveCodesArr[minIdx];
      String endEffectiveCode = sortedEffectiveCodesArr[maxIdx];
      TreeSet<String> subSetOfSortedEffectiveCodes = new TreeSet<String>(sortedEffectiveCodes.subSet(startEffectiveCode, endEffectiveCode));
      commaSeparatedCodes = getCommaSeparatedEffectiveCodes(subSetOfSortedEffectiveCodes);
      response = api.getAvailableRegions(commaSeparatedCodes);
      responseIterator = response.iterator();
      for (String effectiveCode : subSetOfSortedEffectiveCodes) {
        Region region1 = regionGiven(effectiveCode, topLevelRegions);
        Collection<Region> thirdLevelRegions = responseIterator.next();
        region1.setChildRegions(thirdLevelRegions);
      }
    }

    for (Region topLevelRegion : topLevelRegions) {
      Map<String, Collection<String>> regionHierarchy = topLevelRegion.getRegionHierarchy();
      for (Map.Entry<String, Collection<String>> regionSubregions : regionHierarchy.entrySet()) {
        availableRegions.put(regionSubregions.getKey(), regionSubregions.getValue());
      }
    }

    System.out.println("In UltraDNSRestGeoSupport.java, in regions(), returning: " + (new Gson()).toJson(availableRegions));
    return availableRegions;
  }

  private TreeSet<Region> getSortedSecondLevelRegions(Collection<Region> topLevelRegions) {
    TreeSet<Region> secondLevelRegions = new TreeSet<Region>();
    for(Region topLevelRegion : topLevelRegions) {
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

  public class Region implements Comparable<Region> {

    private String name;
    private String code;
    private String type;
    private int id;
    private String effectiveCode;

    private Region parentRegion;
    private TreeSet<Region> childRegions;

    public Region(String name, String code, String type, int id) {
      this.name = name;
      this.code = code;
      this.type = type;
      this.id = id;
    }

    public String getName() {
      return this.name;
    }

    public void setName(String name1) {
      this.name = name1;
    }

    public String getCode() {
      return this.code;
    }

    public void setCode(String code1) {
      this.code = code1;
    }

    public String getType() {
      return this.type;
    }

    public void setType(String type1) {
      this.type = type1;
    }

    public int getId() {
      return this.id;
    }

    public void setId(int id1) {
      this.id = id1;
    }

    public Region getParentRegion() {
      return this.parentRegion;
    }

    public void setParent(Region region) {
      this.parentRegion = region;
      this.setEffectiveCode();
    }

    public TreeSet<Region> getChildRegions() {
      return this.childRegions;
    }

    public void setChildRegions(Region[] regions) {
      TreeSet<Region> tmpChildRegions = (TreeSet<Region>) Arrays.asList(regions);
      this.setChildRegions(tmpChildRegions);
    }

    public void setChildRegions(Collection<Region> regions) {
      this.childRegions = new TreeSet<Region>(regions);
      for (Region childRegion : childRegions) {
        childRegion.setParent(this);
        childRegion.setEffectiveCode();
      }
    }

    public String getEffectiveCode() {
      return this.effectiveCode;
    }

    public void setEffectiveCode() {
      if (this.getParentRegion() != null) {
        this.effectiveCode = this.getParentRegion().getCode() + "-" + this.getCode();
      } else {
        this.effectiveCode = this.getCode();
      }
    }

    public String[] getEffectiveCodesOfChildRegions() {
      String[] childRegionEffectiveCodes = new String[this.getChildRegions().size()];
      int i = 0;
      for (Region region : this.getChildRegions()) {
        childRegionEffectiveCodes[i] = region.getCode();
        i++;
      }
      Arrays.sort(childRegionEffectiveCodes);
      return childRegionEffectiveCodes;
    }

    public boolean isRegion() {
      return this.getType().equals("Region");
    }

    public boolean isCountry() {
      return this.getType().equals("Country");
    }

    public int compareTo(Region region1) {
      return this.getName().compareTo(region1.getName());
    }

    public Map<String, Collection<String>> getRegionHierarchy() {
      Map<String, Collection<String>> regionNameSubRegionNames = new TreeMap<String, Collection<String>>();
      Collection<String> subRegionNames = new TreeSet<String>();
      TreeSet<Region> childRegions = getChildRegions();
      if (this.isCountry()) {
        if (childRegions == null || childRegions.size() == 0) {
          subRegionNames.add(this.getName());
        } else {
          for (Region childRegion : childRegions) {
            subRegionNames.add(childRegion.getName());
          }
        }
      } else {
        if (childRegions != null) {
          for (Region childRegion : childRegions) {
            Map<String, Collection<String>> childChildRegionHierarchy = childRegion.getRegionHierarchy();
            for (Map.Entry<String, Collection<String>> entry : childChildRegionHierarchy.entrySet()) {
              regionNameSubRegionNames.put(entry.getKey(), entry.getValue());
            }
          }
        }
      }
      if (subRegionNames.size() > 0) {
        regionNameSubRegionNames.put(this.getName(), subRegionNames);
      }
      return regionNameSubRegionNames;
    }

    @Override
    public String toString() {
      String str = "{";
      str += "code: " + this.code;
      str += ", name: " + this.name;
      str += ", type: " + this.type;
      str += ", id: " + this.id;
      str += ", effectiveCode: " + this.effectiveCode;
      str += "}";
      return str;
    }

    @Override
    public boolean equals(Object object) {
      if (object instanceof Region) {
        Region region1 = (Region) object;
        return ((this.name == region1.name || this.name.equals(region1.name)) &&
                (this.type == region1.type || this.type.equals(region1.type)) &&
                (this.code == region1.code || this.code.equals(region1.code)) &&
                (this.effectiveCode == region1.effectiveCode || this.effectiveCode.equals(region1.effectiveCode)) &&
                (this.parentRegion == region1.parentRegion || this.parentRegion.equals(region1.parentRegion)) &&
                (this.childRegions == region1.childRegions || this.childRegions.equals(region1.childRegions)) &&
                (this.id == region1.id));
      } else {
        return false;
      }
    }

  }

}
