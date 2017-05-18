package denominator.ultradns.model;

import java.util.TreeSet;

public class GeoInfo {

    private String name;
    private TreeSet<String> codes = new TreeSet<String>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TreeSet<String> getCodes() {
        return codes;
    }

    public void setCodes(TreeSet<String> codes) {
        this.codes = codes;
    }
}
