package denominator.ultradns.model;

import java.util.ArrayList;
import java.util.List;

public class Record {

    private String name;
    private int typeCode;
    private int ttl;
    private List<String> rdata = new ArrayList<String>();
    private Profile profile;

    public RRSet buildRRSet() {
        return new RRSet(getTtl(), getRdata());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(int typeCode) {
        this.typeCode = typeCode;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public List<String> getRdata() {
        return rdata;
    }

    public void setRdata(List<String> rdata) {
        this.rdata = rdata;
    }

    public Profile getProfile() {
        return this.profile;
    }

    public void setProfile(Profile profile1) {
        this.profile = profile1;
    }

}
