package denominator.ultradns.model;

public class RDataInfo {

    private GeoInfo geoInfo;
    private IpInfo ipInfo;
    private Integer ttl;
    private String type;

    public GeoInfo getGeoInfo() {
        return geoInfo;
    }

    public void setGeoInfo(GeoInfo geoInfo) {
        this.geoInfo = geoInfo;
    }

    public IpInfo getIpInfo() {
        return ipInfo;
    }

    public void setIpInfo(IpInfo ipInfo) {
        this.ipInfo = ipInfo;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
