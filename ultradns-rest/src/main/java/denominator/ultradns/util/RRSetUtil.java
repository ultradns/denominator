package denominator.ultradns.util;

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
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

import static denominator.ResourceTypeToValue.lookup;
import denominator.ResourceTypeToValue.ResourceTypes;

import static denominator.ultradns.util.Constants.DIRECTIONAL_POOL_SCHEMA;

import org.apache.commons.lang.StringUtils;

/**
 * This class will contain all utility methods to build
 * ResourceRecord, GeoDirectionalRecord from REST RRSet Response.
 */
public final class RRSetUtil {

    private RRSetUtil() { }

    /**
     * Record types for which we will split the rdata.
     */
    private static final Set<String> RECORD_TYPE_TO_SPLIT = new HashSet<String>(
            Arrays.asList(
                ResourceTypes.MX.name(),
                ResourceTypes.SOA.name(),
                ResourceTypes.SRV.name(),
                ResourceTypes.CERT.name(),
                ResourceTypes.NAPTR.name(),
                ResourceTypes.SSHFP.name()
            ));

    /**
     * Creation of ResourceRecord with rData.
     *
     * @param rrSets List of RRSet
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
                        r.setRdata(buildRDataList(rData, stringValueOfRrtype(rrSet.getRrtype())));
                        if (rrSet.getTtl() != null) {
                            r.setTtl(rrSet.getTtl());
                        }
                        records.add(r);
                    }
                }
            }
        }
        return records;
    }

    /**
     * Creation of Directional Record with rData.
     *
     * @param rrSets List of RRSet
     * @return List of Directional Record
     */
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
                        if (!rDataInfoList.isEmpty()) {
                            RDataInfo rDataInfo = rDataInfoList.get(rDataList.indexOf(rData));
                            r.setType(rDataInfo.getType());
                            r.setTypeCode(lookup(rDataInfo.getType()));
                            r.setRdata(buildRDataList(rData, rDataInfo.getType()));
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
                        r.setRdata(Arrays.asList("No Data Response"));
                        r.setNoResponseRecord(true);
                        records.add(r);
                    }
                }
            }
        }
        return records;
    }

    /**
     * Builds a rdata parts list based on record type from rdata string
     * which came as part of UltraDNS Rest response.
     *
     * @param data rData String
     * @return RData parts List
     */
    private static List<String> buildRDataList(String data, String rrType) {
        List<String> rDataList = new ArrayList<String>();
        if (!StringUtils.isEmpty(data)) {
            rDataList = RECORD_TYPE_TO_SPLIT.contains(rrType)
                    ? Arrays.asList(data.split("\\s")) : Arrays.asList(data);

        }
        return rDataList;
    }

    /**
     * Returns map of Record with name as key and type as value.
     *
     * @param rrSets list
     * @return nameAndType map
     */
    public static Map<String, Integer> getNameAndType(List<RRSet> rrSets) {
        Map<String, Integer> nameAndType = new HashMap<String, Integer>();
        if (rrSets != null && !rrSets.isEmpty()) {
            for (RRSet rrSet : rrSets) {
                nameAndType.put(rrSet.getOwnerName(), intValueOfRrtype(rrSet.getRrtype()));
            }
        }
        return nameAndType;
    }

    /**
     * Returns list of Directional Record for specified rrSets and group name.
     *
     * @param rrSets List of RRSet
     * @param groupName Name of the directional group
     * @return List of Directional Record
     */
    public static List<DirectionalRecord> getDirectionalRecordsByGroup(List<RRSet> rrSets, String groupName) {
        List<DirectionalRecord> records = new ArrayList<DirectionalRecord>();
        if (groupName == null || groupName.length() == 0) {
            return records;
        }
        for (DirectionalRecord r : buildDirectionalRecords(rrSets)) {
            if (r.getGeoGroupName() != null && groupName.equals(r.getGeoGroupName())
                    || r.getIpGroupName() != null && groupName.equals(r.getIpGroupName())) {
                records.add(r);
            }
        }
        return records;
    }

    /**
     * Returns order set of region codes for specified rrSets and group name.
     *
     * @param rrSets List of RRSet
     * @param groupName Name of the directional group
     * @return Ordered Set of region codes
     */
    public static TreeSet<String> getDirectionalGroupDetails(List<RRSet> rrSets, String groupName) {
        TreeSet<String> countryCodes = new TreeSet<String>();
        if (rrSets != null && !rrSets.isEmpty()) {
            for (RRSet rrSet : rrSets) {
                if (rrSet.getProfile() != null && rrSet.getProfile().getRdataInfo() != null) {
                    List<RDataInfo> rDataInfoList = rrSet.getProfile().getRdataInfo();
                    for (RDataInfo rd : rDataInfoList) {
                        if (rd.getGeoInfo() != null && rd.getGeoInfo().getName() != null
                                && groupName.equals(rd.getGeoInfo().getName())) {
                            countryCodes = rd.getGeoInfo().getCodes();
                        }
                    }
                }
                if (rrSet.getProfile() != null && rrSet.getProfile().getNoResponse() != null) {
                    GeoInfo geoInfo = rrSet.getProfile().getNoResponse().getGeoInfo();
                    if (geoInfo != null && geoInfo.getName() != null && groupName.equals(geoInfo.getName())) {
                        countryCodes = geoInfo.getCodes();
                    }
                }
            }
        }
        return countryCodes;
    }

    /**
     * Returns true if the record is directional,false otherwise.
     *
     * @param rrSet Resource Record data.
     * @return boolean will be true or false base on record schema type.
     */
    private static boolean isDirectionalRecord(RRSet rrSet) {
        return rrSet.getProfile() != null
                && rrSet.getProfile().getContext() != null
                && DIRECTIONAL_POOL_SCHEMA.equals(rrSet.getProfile().getContext());
    }

    /**
     * Returns integer value of specified rrtype.
     *
     * @param rrType Resource Record type as per UltraDNS Rest Response
     * @return int type-code of Resource Record. For A it will be '1'
     */
    public static int intValueOfRrtype(String rrType) {
        if (rrType != null) {
            return Integer.parseInt(rrType.substring(rrType.indexOf("(") + 1,
                    rrType.indexOf(")")));
        }
        return 0;
    }

    /**
     * Returns string value of specified rrtype.
     *
     * @param rrType Resource Record type as per UltraDNS Rest Response
     * @return String name of Resource Record type. For A it will be 'A'
     */
    public static String stringValueOfRrtype(String rrType) {
        if (rrType != null) {
            return rrType.substring(0, rrType.indexOf(" "));
        }
        return "";
    }

    /**
     * Returns type code value of specified rrtype.
     *
     * @param type name of Resource Record type
     * @return int type-code of Resource Record.
     */
    public static int directionalRecordType(String type) {
        if (ResourceTypes.A.name().equals(type) || ResourceTypes.CNAME.name().equals(type)) {
            return lookup(ResourceTypes.A.name());
        } else if (ResourceTypes.AAAA.name().equals(type)) {
            return lookup(ResourceTypes.AAAA.name());
        } else {
            return lookup(type);
        }
    }
}
