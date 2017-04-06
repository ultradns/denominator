package denominator.ultradns.model;

import java.util.Collection;
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

  private static final int INITIAL_NON_ZERO_ODD_NUMBER = 101;
  private static final int MULTIPLIER_NON_ZERO_ODD_NUMBER = 103;

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

  @Override
  public int compareTo(Region region1) {
    return this.getName().compareTo(region1.getName());
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
    return new HashCodeBuilder(
            INITIAL_NON_ZERO_ODD_NUMBER, MULTIPLIER_NON_ZERO_ODD_NUMBER)
            .append(code)
            .append(name)
            .append(type)
            .append(id)
            .toHashCode();
  }

}
