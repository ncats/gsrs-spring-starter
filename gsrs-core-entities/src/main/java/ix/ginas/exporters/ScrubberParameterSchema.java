package ix.ginas.exporters;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Scrubber Parameters
 * <p>
 * Factors that control the behavior of a Java class that removes private parts of a data object before the object is shared
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "removeDate",
        "deidentifyAuditUser",
        "excludedReferencePatterns",
        "deidentifiedReferencePatterns",
        "accessGroupsToInclude"
})
@Generated("jsonschema2pojo")
public class ScrubberParameterSchema {


    /**
     * When true, remove all date fields from output
     */
    @JsonProperty("removeDate")
    @JsonPropertyDescription("When true, remove all date fields from output")
    private Boolean removeDate;
    /**
     * When true, remove users listed as creator or modifier of records and subrecords
     */
    @JsonProperty("deidentifyAuditUser")
    @JsonPropertyDescription("When true, remove users listed as creator or modifier of records and subrecords")
    private Boolean deidentifyAuditUser;
    /**
     * References to omit (pattern applies to document type)
     */
    @JsonProperty("excludedReferencePatterns")
    @JsonPropertyDescription("References to omit (pattern applies to document type)")
    private String excludedReferencePatterns;
    /**
     * References to replace (pattern applies to document type)
     */
    @JsonProperty("deidentifiedReferencePatterns")
    @JsonPropertyDescription("References to replace (pattern applies to document type)")
    private String deidentifiedReferencePatterns;
    /**
     * names of access groups to that will NOT be removed
     */
    @JsonProperty("accessGroupsToInclude")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    @JsonPropertyDescription("names of access groups to that will NOT be removed ")
    private Set<String> accessGroupsToInclude = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * When true, remove all date fields from output
     */
    @JsonProperty("removeDate")
    public Boolean getRemoveDate() {
        return removeDate;
    }

    /**
     * When true, remove all date fields from output
     */
    @JsonProperty("removeDate")
    public void setRemoveDate(Boolean removeDate) {
        this.removeDate = removeDate;
    }

    /**
     * When true, remove users listed as creator or modifier of records and subrecords
     */
    @JsonProperty("deidentifyAuditUser")
    public Boolean getDeidentifyAuditUser() {
        return deidentifyAuditUser;
    }

    /**
     * When true, remove users listed as creator or modifier of records and subrecords
     */
    @JsonProperty("deidentifyAuditUser")
    public void setDeidentifyAuditUser(Boolean deidentifyAuditUser) {
        this.deidentifyAuditUser = deidentifyAuditUser;
    }

    /**
     * References to omit (pattern applies to document type)
     */
    @JsonProperty("excludedReferencePatterns")
    public String getExcludedReferencePatterns() {
        return excludedReferencePatterns;
    }

    /**
     * References to omit (pattern applies to document type)
     */
    @JsonProperty("excludedReferencePatterns")
    public void setExcludedReferencePatterns(String excludedReferencePatterns) {
        this.excludedReferencePatterns = excludedReferencePatterns;
    }

    /**
     * References to replace (pattern applies to document type)
     */
    @JsonProperty("deidentifiedReferencePatterns")
    public String getDeidentifiedReferencePatterns() {
        return deidentifiedReferencePatterns;
    }

    /**
     * References to replace (pattern applies to document type)
     */
    @JsonProperty("deidentifiedReferencePatterns")
    public void setDeidentifiedReferencePatterns(String deidentifiedReferencePatterns) {
        this.deidentifiedReferencePatterns = deidentifiedReferencePatterns;
    }

    /**
     * names of access groups to that will NOT be removed
     */
    @JsonProperty("accessGroupsToInclude")
    public Set<String> getAccessGroupsToInclude() {
        return accessGroupsToInclude;
    }

    /**
     * names of access groups to that will NOT be removed
     */
    @JsonProperty("accessGroupsToInclude")
    public void setAccessGroupsToInclude(Set<String> accessGroupsToInclude) {
        this.accessGroupsToInclude = accessGroupsToInclude;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }


}
