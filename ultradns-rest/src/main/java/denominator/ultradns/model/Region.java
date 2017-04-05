package denominator.ultradns.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Region implements Comparable<Region> {

  private String name;
  private String code;
  private String type;
  private int id;
  private String effectiveCode;
  private String effectiveCodeForGeo;

  private Region parentRegion;
  private TreeSet<Region> childRegions;

  public Region(String name, String code, String type, int id) {
    this.name = name;
    this.code = code;
    this.type = type;
    this.id = id;
    this.setEffectiveCode();
    this.setEffectiveCodeForGeo();
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
    this.setEffectiveCodeForGeo();
  }

  public TreeSet<Region> getChildRegions() {
    return this.childRegions;
  }

  public void setChildRegions(Collection<Region> regions) {
    this.childRegions = new TreeSet<Region>(regions);
    for (Region childRegion : childRegions) {
      childRegion.setParent(this);
    }
  }

  public String getEffectiveCode() {
    if (this.effectiveCode == null && this.code != null) {
      this.setEffectiveCode();
    }
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
      childRegionEffectiveCodes[i] = region.getEffectiveCode();
      i++;
    }
    Arrays.sort(childRegionEffectiveCodes);
    return childRegionEffectiveCodes;
  }

  public String getEffectiveCodeForGeo() {
    if (this.effectiveCodeForGeo == null && this.code != null) {
      this.setEffectiveCodeForGeo();
    }
    return this.effectiveCodeForGeo;
  }

  public void setEffectiveCodeForGeo() {
    if (this.isCountry() || this.isRegion()) {
      this.effectiveCodeForGeo = this.getCode();
    } else {
      String parentRegionCode = "";
      if (this.getParentRegion() != null) {
        parentRegionCode = this.getParentRegion().getCode();
      }
      this.effectiveCodeForGeo = parentRegionCode + "-" + this.getCode();
    }
  }

  public boolean isRegion() {
    return "Region".equals(this.getType());
  }

  public boolean isCountry() {
    return "Country".equals(this.getType());
  }

  public int compareTo(Region region1) {
    return this.getName().compareTo(region1.getName());
  }

  public Map<String, Collection<String>> getRegionHierarchy() {
    Map<String, Collection<String>> regionNameSubRegionNames = new TreeMap<String, Collection<String>>();
    Collection<String> subRegionNames = new TreeSet<String>();
    TreeSet<Region> childRegions1 = getChildRegions();
    if (this.isCountry()) {
      if (childRegions1 == null || childRegions1.size() == 0) {
        subRegionNames.add(this.getName());
      } else {
        for (Region childRegion : childRegions1) {
          subRegionNames.add(childRegion.getName());
        }
      }
    } else {
      if (this.isRegion()) {
        subRegionNames.add(this.getName());
      }

      if (childRegions1 != null) {
        for (Region childRegion : childRegions1) {
          subRegionNames.add(childRegion.getName());
        }

        for (Region childRegion : childRegions1) {
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
    TreeSet<Region> childRegions1 = getChildRegions();
    if (this.isCountry()) {
      if (childRegions1 == null || childRegions1.size() == 0) {
        subRegionNames.add(this);
      } else {
        for (Region childRegion : childRegions1) {
          subRegionNames.add(childRegion);
        }
      }
    } else {
      if (this.isRegion()) {
        subRegionNames.add(this);
      }

      if (childRegions1 != null) {
        for (Region childRegion : childRegions1) {
          subRegionNames.add(childRegion);
        }

        for (Region childRegion : childRegions1) {
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
    str += "\"code\": \"" + this.code + "\"";
    str += ", \"name\": \"" + this.name + "\"";
    str += ", \"type\": \"" + this.type + "\"";
    str += ", \"id\": " + this.id;
    str += ", \"effectiveCode\": \"" + this.getEffectiveCode() + "\"";
    str += ", \"effectiveCodeForGeo\": \"" + this.getEffectiveCodeForGeo() + "\"";
    str += "}";
    return str;
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    }
    if (object == this) {
      return true;
    }
    if (this.getClass() != object.getClass()) {
      return false;
    }

    Region region1 = (Region) object;
    return new EqualsBuilder()
            .append(code, region1.code)
            .append(name, region1.name)
            .append(type, region1.type)
            .append(id, region1.id)
            .isEquals();
  }

  @Override
  public int hashCode() {
    int initialNonZeroOddNumber = 101;
    int multiplierNonZeroOddNumber = 103;
    return new HashCodeBuilder(
            initialNonZeroOddNumber, multiplierNonZeroOddNumber)
            .append(code)
            .append(name)
            .append(type)
            .append(id)
            .toHashCode();
  }

}
