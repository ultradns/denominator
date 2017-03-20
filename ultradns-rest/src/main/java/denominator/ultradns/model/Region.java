package denominator.ultradns.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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
      if (this.isRegion()) {
        subRegionNames.add(this.getName());
      }

      if (childRegions != null) {
        for (Region childRegion : childRegions) {
          subRegionNames.add(childRegion.getName());
        }

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

  public Map<Region, Collection<Region>> getRegionHierarchyAsRegions() {
    Map<Region, Collection<Region>> regionNameSubRegionNames = new TreeMap<Region, Collection<Region>>();
    Collection<Region> subRegionNames = new TreeSet<Region>();
    TreeSet<Region> childRegions = getChildRegions();
    if (this.isCountry()) {
      if (childRegions == null || childRegions.size() == 0) {
        subRegionNames.add(this);
      } else {
        for (Region childRegion : childRegions) {
          subRegionNames.add(childRegion);
        }
      }
    } else {
      if (this.isRegion()) {
        subRegionNames.add(this);
      }

      if (childRegions != null) {
        for (Region childRegion : childRegions) {
          subRegionNames.add(childRegion);
        }

        for (Region childRegion : childRegions) {
          Map<Region, Collection<Region>> childChildRegionHierarchy = childRegion.getRegionHierarchyAsRegions();
          for (Map.Entry<Region, Collection<Region>> entry : childChildRegionHierarchy.entrySet()) {
            regionNameSubRegionNames.put(entry.getKey(), entry.getValue());
          }
        }
      }
    }
    if (subRegionNames.size() > 0) {
      regionNameSubRegionNames.put(this, subRegionNames);
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
