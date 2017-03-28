package denominator.ultradns.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;

public class RRSetList {

    private String zoneName;
    private List<RRSet> rrSets;

    private final String DIR_POOL_SCHEMA ="http://schemas.ultradns.com/DirPool.jsonschema";

    public List<Record> buildRecords(){
        List<Record> records = new ArrayList<Record>();
        if (getRrSets() != null && !getRrSets().isEmpty()) {
            for (RRSet rrSet : getRrSets()){
                if (!isDirectionalRecord(rrSet)) {
                    /**
                     * Creation of ResourceRecord with rData
                     */
                    if (rrSet.getRdata() != null && !rrSet.getRdata().isEmpty()) {
                        for (String rData : rrSet.getRdata()) {
                            Record r = new Record();
                            r.setName(rrSet.getOwnerName());
                            r.setTypeCode(rrSet.intValueOfRrtype());
                            if (rrSet.getTtl() != null) {
                                r.setTtl(rrSet.getTtl());
                            }
                            if (rData != null) {
                                r.setRdata(Arrays.asList(rData.split("\\s")));
                            }
                            records.add(r);
                        }
                    }
                }
            }
        }
        return records;
    }

    public List<DirectionalRecord> buildDirectionalRecords(){
        List<DirectionalRecord> records = new ArrayList<DirectionalRecord>();
        if (getRrSets() != null && !getRrSets().isEmpty()) {
            for (RRSet rrSet : getRrSets()) {
                if (isDirectionalRecord(rrSet)) {
                    List<String> rDataList = new ArrayList<String>();
                    List<RDataInfo> rDataInfoList = new ArrayList<RDataInfo>();

                    if (rrSet.getRdata() != null) {
                        rDataList = rrSet.getRdata();
                    }
                    if (rrSet.getProfile() != null && rrSet.getProfile().getRdataInfo() != null ) {
                        rDataInfoList = rrSet.getProfile().getRdataInfo();
                    }

                    /**
                     * Creation of DirectionalDNSRecord with rData & rDataInfo mapping
                     */
                    for (String rData : rDataList) {
                        DirectionalRecord r = new DirectionalRecord();
                        r.setName(rrSet.getOwnerName());
                        r.setTypeCode(rrSet.intValueOfRrtype());
                        r.setType(rrSet.stringValueOfRrtype());
                        if (!rDataInfoList.isEmpty()) {
                            RDataInfo rDataInfo = rDataInfoList.get(rDataList.indexOf(rData));
                            if (rDataInfo.getGeoInfo() != null) {
                                r.setGeoGroupName(rDataInfo.getGeoInfo().getName());
                            }
                            if (rDataInfo.getIpInfo() != null) {
                                r.setIpGroupName(rDataInfo.getIpInfo().getName());
                            }
                            if (rDataInfo.getTtl() != null) {
                                r.setTtl(rDataInfo.getTtl());
                            }
                        }
                        if (rData != null) {
                            r.setRdata(Arrays.asList(rData.split("\\s")));
                        }
                        r.setNoResponseRecord(false);
                        records.add(r);
                    }

                    /**
                     * Creation of DirectionalDNSRecord with No Data Response
                     */
                    if (rrSet.getProfile() != null && rrSet.getProfile().getNoResponse() != null) {
                        DirectionalRecord r = new DirectionalRecord();
                        r.setName(rrSet.getOwnerName());
                        r.setTypeCode(rrSet.intValueOfRrtype());
                        r.setType(rrSet.stringValueOfRrtype());
                        NoResponse noResponse = rrSet.getProfile().getNoResponse();
                        if (noResponse.getGeoInfo() != null) {
                            r.setGeoGroupName(noResponse.getGeoInfo().getName());
                        }
                        if (noResponse.getIpInfo() != null) {
                            r.setIpGroupName(noResponse.getIpInfo().getName());
                        }
                        if (noResponse.getTtl() != null) {
                            r.setTtl(noResponse.getTtl());
                        }
                        r.setRdata(Arrays.asList(new String[]{"No Data Response"}));
                        r.setNoResponseRecord(true);
                        records.add(r);
                    }
                }
            }
        }
        return records;
    }

    public Map<String, Integer> getNameAndType() {
        Map<String, Integer> nameAndType = new HashMap<String, Integer>();
        if (getRrSets() != null && !getRrSets().isEmpty()) {
            for (RRSet rrSet : getRrSets()) {
                nameAndType.put(rrSet.getOwnerName(), rrSet.intValueOfRrtype());
            }
        }
        return nameAndType;
    }

    public List<DirectionalRecord> getDirectionalRecordsByGroup(String groupName) {
        List<DirectionalRecord> records = new ArrayList<DirectionalRecord>();
        if ( groupName == null || groupName.length() == 0 ) {
            return records;
        }
        for (DirectionalRecord r : buildDirectionalRecords()) {
            if ( (r.getGeoGroupName() != null && r.getGeoGroupName().equals(groupName))
                    || (r.getIpGroupName() != null && r.getIpGroupName().equals(groupName)) ) {
                records.add(r);
            }
        }
        return records;
    }

    public TreeSet<String> getDirectionalGroupDetails(String groupName) {
        TreeSet<String> countryCodes = new TreeSet<String>();
        if (getRrSets() != null && !getRrSets().isEmpty()) {
            for (RRSet rrSet : getRrSets()) {
                if(rrSet.getProfile() != null & rrSet.getProfile().getRdataInfo() != null) {
                    List<RDataInfo> rDataInfoList = rrSet.getProfile().getRdataInfo();
                    for (RDataInfo rd : rDataInfoList) {
                        if (rd.getGeoInfo() != null && rd.getGeoInfo().getName() != null
                                && rd.getGeoInfo().getName().equals(groupName)) {
                            countryCodes = rd.getGeoInfo().getCodes();
                        }
                    }
                }
                if(rrSet.getProfile() != null & rrSet.getProfile().getNoResponse() != null) {
                    GeoInfo geoInfo = rrSet.getProfile().getNoResponse().getGeoInfo();
                    if (geoInfo != null && geoInfo.getName() != null && geoInfo.getName().equals(groupName)) {
                        countryCodes = geoInfo.getCodes();
                    }
                }
            }
        }
        return countryCodes;
    }

    private boolean isDirectionalRecord(RRSet rrSet) {
        return (
            rrSet.getProfile() != null
            && rrSet.getProfile().getContext() != null
            && rrSet.getProfile().getContext().equals(DIR_POOL_SCHEMA)
        );
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public List<RRSet> getRrSets() {
        return rrSets;
    }

    public void setRrSets(List<RRSet> rrSets) {
        this.rrSets = rrSets;
    }

}
