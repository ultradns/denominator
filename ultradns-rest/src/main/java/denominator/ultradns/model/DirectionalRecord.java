package denominator.ultradns.model;

public class DirectionalRecord extends Record {

    private String geoGroupName;
    private String ipGroupName;
    private boolean noResponseRecord;
    private String type;

    public String getGeoGroupName() {
        return geoGroupName;
    }

    public void setGeoGroupName(String geoGroupName) {
        this.geoGroupName = geoGroupName;
    }

    public boolean isNoResponseRecord() {
        return noResponseRecord;
    }

    public void setNoResponseRecord(boolean noResponseRecord) {
        this.noResponseRecord = noResponseRecord;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIpGroupName() {
        return ipGroupName;
    }

    public void setIpGroupName(String ipGroupName) {
        this.ipGroupName = ipGroupName;
    }
}
