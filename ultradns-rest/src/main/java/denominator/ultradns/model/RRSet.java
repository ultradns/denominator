package denominator.ultradns.model;

import java.util.ArrayList;
import java.util.List;

public class RRSet {

    private String ownerName;
    private String rrtype;
    private Integer ttl;
    private List<String> rdata = new ArrayList<String>();
    private Profile profile;

    public RRSet() { }

    public RRSet(Integer ttl, List<String> rdata) {
        this.ttl = ttl;
        this.rdata = rdata;
    }

    public int intValueOfRrtype() {
        if (rrtype != null) {
            return Integer.parseInt(rrtype.substring(rrtype.indexOf("(") + 1,
                    rrtype.indexOf(")")));
        }
        return 0;
    }

    public String stringValueOfRrtype() {
        if (rrtype != null) {
            return rrtype.substring(0, rrtype.indexOf(" "));
        }
        return "";
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getRrtype() {
        return rrtype;
    }

    public void setRrtype(String rrtype) {
        this.rrtype = rrtype;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public List<String> getRdata() {
        return rdata;
    }

    public void setRdata(List<String> rdata) {
        this.rdata = rdata;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

}
