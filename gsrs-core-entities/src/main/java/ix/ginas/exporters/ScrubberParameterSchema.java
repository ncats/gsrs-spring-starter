package ix.ginas.exporters;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * Scrubber Parameters
 * <p>
 * Factors that control the behavior of a Java class that removes private parts of a data object before the object is shared
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "removeDate",
        "deidentifyAuditUser",
        "deidentifiedReferencePatterns",
        "accessGroupsToInclude",
        "accessGroupsToRemove",
        "removeAllLocked",
        "removeCodesBySystem",
        "codeSystemsToRemove",
        "codeSystemsToKeep",
        "removeReferencesByCriteria",
        "referenceTypesToRemove",
        "citationPatternsToRemove",
        "excludeReferenceByPattern",
        "substanceReferenceCleanup",
        "removeReferencesToFilteredSubstances",
        "removeReferencesToSubstancesNonExportedDefinitions",
        "removeNotes",
        "removeChangeReason",
        "removeApprovalId",
        "approvalIdCodeSystem",
        "regenerateUUIDs",
        "changeAllStatuses",
        "newStatusValue",
        "newAuditorValue"
})
@Generated("jsonschema2pojo")
public class ScrubberParameterSchema {

    /**
     * When true, remove all date fields from output
     *
     */
    @JsonProperty("removeDate")
    @JsonPropertyDescription("When true, remove all date fields from output")
    private Boolean removeDate;
    /**
     * When true, remove users listed as creator or modifier of records and subrecords
     *
     */
    @JsonProperty("deidentifyAuditUser")
    @JsonPropertyDescription("When true, remove users listed as creator or modifier of records and subrecords")
    private Boolean deidentifyAuditUser;
    /**
     * References to replace (pattern applies to document type)
     *
     */
    @JsonProperty("deidentifiedReferencePatterns")
    @JsonPropertyDescription("References to replace (pattern applies to document type)")
    private String deidentifiedReferencePatterns;
    /**
     * names of access groups to that will NOT be removed
     *
     */
    @JsonProperty("accessGroupsToInclude")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    @JsonPropertyDescription("names of access groups to that will NOT be removed ")
    private Set<String> accessGroupsToInclude = null;
    /**
     * names of access groups to that WILL be removed
     *
     */
    @JsonProperty("accessGroupsToRemove")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    @JsonPropertyDescription("names of access groups to that WILL be removed ")
    private Set<String> accessGroupsToRemove = null;
    /**
     * When true, remove any data element that is marked as non-public
     *
     */
    @JsonProperty("removeAllLocked")
    @JsonPropertyDescription("When true, remove any data element that is marked as non-public")
    private Boolean removeAllLocked;
    /**
     * When true, remove any Codes whose CodeSystem is on the list
     *
     */
    @JsonProperty("removeCodesBySystem")
    @JsonPropertyDescription("When true, remove any Codes whose CodeSystem is on the list")
    private Boolean removeCodesBySystem;
    /**
     * Code Systems to remove
     *
     */
    @JsonProperty("codeSystemsToRemove")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    @JsonPropertyDescription("Code Systems to remove")
    private Set<String> codeSystemsToRemove = null;
    /**
     * Code Systems to keep
     *
     */
    @JsonProperty("codeSystemsToKeep")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    @JsonPropertyDescription("Code Systems to keep")
    private Set<String> codeSystemsToKeep = null;
    /**
     * When true, remove any References that meet specified criteria
     *
     */
    @JsonProperty("removeReferencesByCriteria")
    @JsonPropertyDescription("When true, remove any References that meet specified criteria")
    private Boolean removeReferencesByCriteria;
    /**
     * Document Types to look at. When a Reference is of that document type, remove it
     *
     */
    @JsonProperty("referenceTypesToRemove")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    @JsonPropertyDescription("Document Types to look at. When a Reference is of that document type, remove it")
    private Set<String> referenceTypesToRemove = null;
    /**
     * Patterns (RegExes) to apply to Reference citation. When a citation matches, remove the Reference
     *
     */
    @JsonProperty("citationPatternsToRemove")
    @JsonDeserialize(as = java.util.LinkedHashSet.class)
    @JsonPropertyDescription("Patterns (RegExes) to apply to Reference citation. When a citation matches, remove the Reference")
    private Set<String> citationPatternsToRemove = null;
    /**
     * Remove References by looking at citationPatternsToRemove
     *
     */
    @JsonProperty("excludeReferenceByPattern")
    @JsonPropertyDescription("Remove References by looking at citationPatternsToRemove")
    private Boolean excludeReferenceByPattern;
    /**
     * When true, next criteria are used to process substance references
     *
     */
    @JsonProperty("substanceReferenceCleanup")
    @JsonPropertyDescription("When true, next criteria are used to process substance references")
    private Boolean substanceReferenceCleanup;
    /**
     * When true, when a substance is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToFilteredSubstances")
    @JsonPropertyDescription("When true, when a substance is removed, remove any references to it")
    private Boolean removeReferencesToFilteredSubstances;
    /**
     * When true, when a substance's definition is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    @JsonPropertyDescription("When true, when a substance's definition is removed, remove any references to it")
    private Boolean removeReferencesToSubstancesNonExportedDefinitions;
    /**
     * When true, remove all Notes
     *
     */
    @JsonProperty("removeNotes")
    @JsonPropertyDescription("When true, remove all Notes")
    private Boolean removeNotes;
    /**
     * When true, delete the 'Change Reason' field
     *
     */
    @JsonProperty("removeChangeReason")
    @JsonPropertyDescription("When true, delete the 'Change Reason' field")
    private Boolean removeChangeReason;
    /**
     * When true, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is removed
     *
     */
    @JsonProperty("removeApprovalId")
    @JsonPropertyDescription("When true, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is removed")
    private Boolean removeApprovalId;
    /**
     * When this parameter has a value, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is copied to a code of this specified system
     *
     */
    @JsonProperty("approvalIdCodeSystem")
    @JsonPropertyDescription("When this parameter has a value, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is copied to a code of this specified system")
    private String approvalIdCodeSystem;
    /**
     * When true, all UUIDs in the object being exported will be given a newly-generated value
     *
     */
    @JsonProperty("regenerateUUIDs")
    @JsonPropertyDescription("When true, all UUIDs in the object being exported will be given a newly-generated value")
    private Boolean regenerateUUIDs;
    /**
     * When true, all status value in the object being exported will be given a value
     *
     */
    @JsonProperty("changeAllStatuses")
    @JsonPropertyDescription("When true, all status value in the object being exported will be given a value")
    private Boolean changeAllStatuses;
    /**
     * new string value to assign to all individual status fields throughout the object
     *
     */
    @JsonProperty("newStatusValue")
    @JsonPropertyDescription("new string value to assign to all individual status fields throughout the object")
    private String newStatusValue;
    /**
     * new string value to assign to all auditor (creator/modifier) fields throughout the object
     *
     */
    @JsonProperty("newAuditorValue")
    @JsonPropertyDescription("new string value to assign to all auditor (creator/modifier) fields throughout the object")
    private String newAuditorValue;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * When true, remove all date fields from output
     *
     */
    @JsonProperty("removeDate")
    public Boolean getRemoveDate() {
        return removeDate;
    }

    /**
     * When true, remove all date fields from output
     *
     */
    @JsonProperty("removeDate")
    public void setRemoveDate(Boolean removeDate) {
        this.removeDate = removeDate;
    }

    /**
     * When true, remove users listed as creator or modifier of records and subrecords
     *
     */
    @JsonProperty("deidentifyAuditUser")
    public Boolean getDeidentifyAuditUser() {
        return deidentifyAuditUser;
    }

    /**
     * When true, remove users listed as creator or modifier of records and subrecords
     *
     */
    @JsonProperty("deidentifyAuditUser")
    public void setDeidentifyAuditUser(Boolean deidentifyAuditUser) {
        this.deidentifyAuditUser = deidentifyAuditUser;
    }

    /**
     * References to replace (pattern applies to document type)
     *
     */
    @JsonProperty("deidentifiedReferencePatterns")
    public String getDeidentifiedReferencePatterns() {
        return deidentifiedReferencePatterns;
    }

    /**
     * References to replace (pattern applies to document type)
     *
     */
    @JsonProperty("deidentifiedReferencePatterns")
    public void setDeidentifiedReferencePatterns(String deidentifiedReferencePatterns) {
        this.deidentifiedReferencePatterns = deidentifiedReferencePatterns;
    }

    /**
     * names of access groups to that will NOT be removed
     *
     */
    @JsonProperty("accessGroupsToInclude")
    public Set<String> getAccessGroupsToInclude() {
        return accessGroupsToInclude;
    }

    /**
     * names of access groups to that will NOT be removed
     *
     */
    @JsonProperty("accessGroupsToInclude")
    public void setAccessGroupsToInclude(Set<String> accessGroupsToInclude) {
        this.accessGroupsToInclude = accessGroupsToInclude;
    }

    /**
     * names of access groups to that WILL be removed
     *
     */
    @JsonProperty("accessGroupsToRemove")
    public Set<String> getAccessGroupsToRemove() {
        return accessGroupsToRemove;
    }

    /**
     * names of access groups to that WILL be removed
     *
     */
    @JsonProperty("accessGroupsToRemove")
    public void setAccessGroupsToRemove(Set<String> accessGroupsToRemove) {
        this.accessGroupsToRemove = accessGroupsToRemove;
    }

    /**
     * When true, remove any data element that is marked as non-public
     *
     */
    @JsonProperty("removeAllLocked")
    public Boolean getRemoveAllLocked() {
        return removeAllLocked;
    }

    /**
     * When true, remove any data element that is marked as non-public
     *
     */
    @JsonProperty("removeAllLocked")
    public void setRemoveAllLocked(Boolean removeAllLocked) {
        this.removeAllLocked = removeAllLocked;
    }

    /**
     * When true, remove any Codes whose CodeSystem is on the list
     *
     */
    @JsonProperty("removeCodesBySystem")
    public Boolean getRemoveCodesBySystem() {
        return removeCodesBySystem;
    }

    /**
     * When true, remove any Codes whose CodeSystem is on the list
     *
     */
    @JsonProperty("removeCodesBySystem")
    public void setRemoveCodesBySystem(Boolean removeCodesBySystem) {
        this.removeCodesBySystem = removeCodesBySystem;
    }

    /**
     * Code Systems to remove
     *
     */
    @JsonProperty("codeSystemsToRemove")
    public Set<String> getCodeSystemsToRemove() {
        return codeSystemsToRemove;
    }

    /**
     * Code Systems to remove
     *
     */
    @JsonProperty("codeSystemsToRemove")
    public void setCodeSystemsToRemove(Set<String> codeSystemsToRemove) {
        this.codeSystemsToRemove = codeSystemsToRemove;
    }

    /**
     * Code Systems to keep
     *
     */
    @JsonProperty("codeSystemsToKeep")
    public Set<String> getCodeSystemsToKeep() {
        return codeSystemsToKeep;
    }

    /**
     * Code Systems to keep
     *
     */
    @JsonProperty("codeSystemsToKeep")
    public void setCodeSystemsToKeep(Set<String> codeSystemsToKeep) {
        this.codeSystemsToKeep = codeSystemsToKeep;
    }

    /**
     * When true, remove any References that meet specified criteria
     *
     */
    @JsonProperty("removeReferencesByCriteria")
    public Boolean getRemoveReferencesByCriteria() {
        return removeReferencesByCriteria;
    }

    /**
     * When true, remove any References that meet specified criteria
     *
     */
    @JsonProperty("removeReferencesByCriteria")
    public void setRemoveReferencesByCriteria(Boolean removeReferencesByCriteria) {
        this.removeReferencesByCriteria = removeReferencesByCriteria;
    }

    /**
     * Document Types to look at. When a Reference is of that document type, remove it
     *
     */
    @JsonProperty("referenceTypesToRemove")
    public Set<String> getReferenceTypesToRemove() {
        return referenceTypesToRemove;
    }

    /**
     * Document Types to look at. When a Reference is of that document type, remove it
     *
     */
    @JsonProperty("referenceTypesToRemove")
    public void setReferenceTypesToRemove(Set<String> referenceTypesToRemove) {
        this.referenceTypesToRemove = referenceTypesToRemove;
    }

    /**
     * Patterns (RegExes) to apply to Reference citation. When a citation matches, remove the Reference
     *
     */
    @JsonProperty("citationPatternsToRemove")
    public Set<String> getCitationPatternsToRemove() {
        return citationPatternsToRemove;
    }

    /**
     * Patterns (RegExes) to apply to Reference citation. When a citation matches, remove the Reference
     *
     */
    @JsonProperty("citationPatternsToRemove")
    public void setCitationPatternsToRemove(Set<String> citationPatternsToRemove) {
        this.citationPatternsToRemove = citationPatternsToRemove;
    }

    /**
     * Remove References by looking at citationPatternsToRemove
     *
     */
    @JsonProperty("excludeReferenceByPattern")
    public Boolean getExcludeReferenceByPattern() {
        return excludeReferenceByPattern;
    }

    /**
     * Remove References by looking at citationPatternsToRemove
     *
     */
    @JsonProperty("excludeReferenceByPattern")
    public void setExcludeReferenceByPattern(Boolean excludeReferenceByPattern) {
        this.excludeReferenceByPattern = excludeReferenceByPattern;
    }

    /**
     * When true, next criteria are used to process substance references
     *
     */
    @JsonProperty("substanceReferenceCleanup")
    public Boolean getSubstanceReferenceCleanup() {
        return substanceReferenceCleanup;
    }

    /**
     * When true, next criteria are used to process substance references
     *
     */
    @JsonProperty("substanceReferenceCleanup")
    public void setSubstanceReferenceCleanup(Boolean substanceReferenceCleanup) {
        this.substanceReferenceCleanup = substanceReferenceCleanup;
    }

    /**
     * When true, when a substance is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToFilteredSubstances")
    public Boolean getRemoveReferencesToFilteredSubstances() {
        return removeReferencesToFilteredSubstances;
    }

    /**
     * When true, when a substance is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToFilteredSubstances")
    public void setRemoveReferencesToFilteredSubstances(Boolean removeReferencesToFilteredSubstances) {
        this.removeReferencesToFilteredSubstances = removeReferencesToFilteredSubstances;
    }

    /**
     * When true, when a substance's definition is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    public Boolean getRemoveReferencesToSubstancesNonExportedDefinitions() {
        return removeReferencesToSubstancesNonExportedDefinitions;
    }

    /**
     * When true, when a substance's definition is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    public void setRemoveReferencesToSubstancesNonExportedDefinitions(Boolean removeReferencesToSubstancesNonExportedDefinitions) {
        this.removeReferencesToSubstancesNonExportedDefinitions = removeReferencesToSubstancesNonExportedDefinitions;
    }

    /**
     * When true, remove all Notes
     *
     */
    @JsonProperty("removeNotes")
    public Boolean getRemoveNotes() {
        return removeNotes;
    }

    /**
     * When true, remove all Notes
     *
     */
    @JsonProperty("removeNotes")
    public void setRemoveNotes(Boolean removeNotes) {
        this.removeNotes = removeNotes;
    }

    /**
     * When true, delete the 'Change Reason' field
     *
     */
    @JsonProperty("removeChangeReason")
    public Boolean getRemoveChangeReason() {
        return removeChangeReason;
    }

    /**
     * When true, delete the 'Change Reason' field
     *
     */
    @JsonProperty("removeChangeReason")
    public void setRemoveChangeReason(Boolean removeChangeReason) {
        this.removeChangeReason = removeChangeReason;
    }

    /**
     * When true, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is removed
     *
     */
    @JsonProperty("removeApprovalId")
    public Boolean getRemoveApprovalId() {
        return removeApprovalId;
    }

    /**
     * When true, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is removed
     *
     */
    @JsonProperty("removeApprovalId")
    public void setRemoveApprovalId(Boolean removeApprovalId) {
        this.removeApprovalId = removeApprovalId;
    }

    /**
     * When this parameter has a value, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is copied to a code of this specified system
     *
     */
    @JsonProperty("approvalIdCodeSystem")
    public String getApprovalIdCodeSystem() {
        return approvalIdCodeSystem;
    }

    /**
     * When this parameter has a value, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is copied to a code of this specified system
     *
     */
    @JsonProperty("approvalIdCodeSystem")
    public void setApprovalIdCodeSystem(String approvalIdCodeSystem) {
        this.approvalIdCodeSystem = approvalIdCodeSystem;
    }

    /**
     * When true, all UUIDs in the object being exported will be given a newly-generated value
     *
     */
    @JsonProperty("regenerateUUIDs")
    public Boolean getRegenerateUUIDs() {
        return regenerateUUIDs;
    }

    /**
     * When true, all UUIDs in the object being exported will be given a newly-generated value
     *
     */
    @JsonProperty("regenerateUUIDs")
    public void setRegenerateUUIDs(Boolean regenerateUUIDs) {
        this.regenerateUUIDs = regenerateUUIDs;
    }

    /**
     * When true, all status value in the object being exported will be given a value
     *
     */
    @JsonProperty("changeAllStatuses")
    public Boolean getChangeAllStatuses() {
        return changeAllStatuses;
    }

    /**
     * When true, all status value in the object being exported will be given a value
     *
     */
    @JsonProperty("changeAllStatuses")
    public void setChangeAllStatuses(Boolean changeAllStatuses) {
        this.changeAllStatuses = changeAllStatuses;
    }

    /**
     * new string value to assign to all individual status fields throughout the object
     *
     */
    @JsonProperty("newStatusValue")
    public String getNewStatusValue() {
        return newStatusValue;
    }

    /**
     * new string value to assign to all individual status fields throughout the object
     *
     */
    @JsonProperty("newStatusValue")
    public void setNewStatusValue(String newStatusValue) {
        this.newStatusValue = newStatusValue;
    }

    /**
     * new string value to assign to all auditor (creator/modifier) fields throughout the object
     *
     */
    @JsonProperty("newAuditorValue")
    public String getNewAuditorValue() {
        return newAuditorValue;
    }

    /**
     * new string value to assign to all auditor (creator/modifier) fields throughout the object
     *
     */
    @JsonProperty("newAuditorValue")
    public void setNewAuditorValue(String newAuditorValue) {
        this.newAuditorValue = newAuditorValue;
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
