package denominator.ultradns.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Profile {

    @SerializedName("@context")
    private String context;
    private String description;
    private String order;
    private String conflictResolve;
    private List<RDataInfo> rdataInfo;
    private NoResponse noResponse;

    public Profile() { }

    public Profile(String context, String description, List<RDataInfo> rdataInfo) {
        this.context = context;
        this.description = description;
        this.rdataInfo = rdataInfo;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getConflictResolve() {
        return conflictResolve;
    }

    public void setConflictResolve(String conflictResolve) {
        this.conflictResolve = conflictResolve;
    }

    public List<RDataInfo> getRdataInfo() {
        return rdataInfo;
    }

    public void setRdataInfo(List<RDataInfo> rdataInfo) {
        this.rdataInfo = rdataInfo;
    }

    public NoResponse getNoResponse() {
        return noResponse;
    }

    public void setNoResponse(NoResponse noResponse) {
        this.noResponse = noResponse;
    }
}
