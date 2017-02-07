package denominator.ultradns.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RRSetList {

    private String zoneName;
    private List<RRSet> rrSets;

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

    public List<Record> buildRecords(){
        List<Record> records = new ArrayList<Record>();
        if(getRrSets() != null && !getRrSets().isEmpty()) {
            for(RRSet rrset : getRrSets()){
                if(rrset.getRdata() != null && !rrset.getRdata().isEmpty()) {
                    for(String rData : rrset.getRdata()){
                        Record r = new Record();
                        r.setName(rrset.getOwnerName());
                        r.setTypeCode(rrset.getIntValueOfRRtype());
                        r.setTtl(rrset.getTtl());
                        if(rData != null){
                            r.setRdata(Arrays.asList(rData.split("\\s")));
                        }
                        records.add(r);
                    }
                }
            }
        }
        return records;
    }

    private class RRSet {

        private String zoneName;
        private String ownerName;
        private String rrtype;
        private Integer ttl;
        private List<String> rdata = new ArrayList<String>();

        public String getZoneName() {
            return zoneName;
        }

        public void setZoneName(String zoneName) {
            this.zoneName = zoneName;
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

        public int getIntValueOfRRtype() {
            return Integer.parseInt(rrtype.substring(rrtype.indexOf("(") + 1,
                    rrtype.indexOf(")")));
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
    }
}