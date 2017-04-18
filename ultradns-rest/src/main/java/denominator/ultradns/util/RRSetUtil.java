package denominator.ultradns.util;

import denominator.ResourceTypeToValue;
import denominator.ultradns.model.RRSet;
import denominator.ultradns.model.Record;
import denominator.ultradns.model.DirectionalRecord;
import denominator.ultradns.model.RDataInfo;
import denominator.ultradns.model.NoResponse;
import denominator.ultradns.model.GeoInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;

import static denominator.ResourceTypeToValue.lookup;
import static denominator.ultradns.util.Constants.DIRECTIONAL_POOL_SCHEMA;

/**
 * This class will contain all utility methods to build
 * ResourceRecord, GeoDirectionalRecord from REST RRSet Response.
 */
public final class RRSetUtil {

    private RRSetUtil() { }

    /**
     * Creation of ResourceRecord with rData.
     * @return List of Resource Record
     */
    public static List<Record> buildRecords(List<RRSet> rrSets) {
        List<Record> records = new ArrayList<Record>();
        if (rrSets != null && !rrSets.isEmpty()) {
            for (RRSet rrSet : rrSets) {
                if (!isDirectionalRecord(rrSet) && rrSet.getRdata() != null && !rrSet.getRdata().isEmpty()) {
                    for (String rData : rrSet.getRdata()) {
                        Record r = new Record();
                        r.setName(rrSet.getOwnerName());
                        r.setTypeCode(intValueOfRrtype(rrSet.getRrtype()));
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
        return records;
    }

    public static List<DirectionalRecord> buildDirectionalRecords(List<RRSet> rrSets) {
        List<DirectionalRecord> records = new ArrayList<DirectionalRecord>();
        if (rrSets != null && !rrSets.isEmpty()) {
            for (RRSet rrSet : rrSets) {
                if (isDirectionalRecord(rrSet)) {
                    List<String> rDataList = new ArrayList<String>();
                    List<RDataInfo> rDataInfoList = new ArrayList<RDataInfo>();

                    if (rrSet.getRdata() != null) {
                        rDataList = rrSet.getRdata();
                    }
                    if (rrSet.getProfile() != null && rrSet.getProfile().getRdataInfo() != null) {
                        rDataInfoList = rrSet.getProfile().getRdataInfo();
                    }

                    /**
                     * Creation of DirectionalDNSRecord with rData and rDataInfo mapping
                     */
                    for (String rData : rDataList) {
                        DirectionalRecord r = new DirectionalRecord();
                        r.setName(rrSet.getOwnerName());
                        r.setTypeCode(intValueOfRrtype(rrSet.getRrtype()));
                        r.setType(stringValueOfRrtype(rrSet.getRrtype()));
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
                        r.setTypeCode(intValueOfRrtype(rrSet.getRrtype()));
                        r.setType(stringValueOfRrtype(rrSet.getRrtype()));
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

    public static Map<String, Integer> getNameAndType(List<RRSet> rrSets) {
        Map<String, Integer> nameAndType = new HashMap<String, Integer>();
        if (rrSets != null && !rrSets.isEmpty()) {
            for (RRSet rrSet : rrSets) {
                nameAndType.put(rrSet.getOwnerName(), intValueOfRrtype(rrSet.getRrtype()));
            }
        }
        return nameAndType;
    }

    public static List<DirectionalRecord> getDirectionalRecordsByGroup(List<RRSet> rrSets, String groupName) {
        List<DirectionalRecord> records = new ArrayList<DirectionalRecord>();
        if (groupName == null || groupName.length() == 0) {
            return records;
        }
        for (DirectionalRecord r : buildDirectionalRecords(rrSets)) {
            if (r.getGeoGroupName() != null && r.getGeoGroupName().equals(groupName)
                    || r.getIpGroupName() != null && r.getIpGroupName().equals(groupName)) {
                records.add(r);
            }
        }
        return records;
    }

    public static TreeSet<String> getDirectionalGroupDetails(List<RRSet> rrSets, String groupName) {
        TreeSet<String> countryCodes = new TreeSet<String>();
        if (rrSets != null && !rrSets.isEmpty()) {
            for (RRSet rrSet : rrSets) {
                if (rrSet.getProfile() != null && rrSet.getProfile().getRdataInfo() != null) {
                    List<RDataInfo> rDataInfoList = rrSet.getProfile().getRdataInfo();
                    for (RDataInfo rd : rDataInfoList) {
                        if (rd.getGeoInfo() != null && rd.getGeoInfo().getName() != null
                                && rd.getGeoInfo().getName().equals(groupName)) {
                            countryCodes = rd.getGeoInfo().getCodes();
                        }
                    }
                }
                if (rrSet.getProfile() != null && rrSet.getProfile().getNoResponse() != null) {
                    GeoInfo geoInfo = rrSet.getProfile().getNoResponse().getGeoInfo();
                    if (geoInfo != null && geoInfo.getName() != null && geoInfo.getName().equals(groupName)) {
                        countryCodes = geoInfo.getCodes();
                    }
                }
            }
        }
        return countryCodes;
    }

    private static boolean isDirectionalRecord(RRSet rrSet) {
        return rrSet.getProfile() != null
                && rrSet.getProfile().getContext() != null
                && rrSet.getProfile().getContext().equals(DIRECTIONAL_POOL_SCHEMA);
    }

    public static int intValueOfRrtype(String rrType) {
        if (rrType != null) {
            return Integer.parseInt(rrType.substring(rrType.indexOf("(") + 1,
                    rrType.indexOf(")")));
        }
        return 0;
    }

    public static String stringValueOfRrtype(String rrType) {
        if (rrType != null) {
            return rrType.substring(0, rrType.indexOf(" "));
        }
        return "";
    }

    public static int directionalRecordType(String type) {
        if (ResourceTypeToValue.ResourceTypes.A.name().equals(type)
                || ResourceTypeToValue.ResourceTypes.CNAME.name().equals(type)) {
            return lookup(ResourceTypeToValue.ResourceTypes.A.name());
        } else if (ResourceTypeToValue.ResourceTypes.AAAA.name().equals(type)) {
            return lookup(ResourceTypeToValue.ResourceTypes.AAAA.name());
        } else {
            return lookup(type);
        }
    }

}
