package ix.ginas.exporters;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


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
     * Remove Date
     * <p>
     * When true, remove all date fields from output
     *
     */
    @JsonProperty("removeDate")
    @JsonPropertyDescription("When true, remove all date fields from output")
    private Boolean removeDate;
    /**
     * Deidentify Audit User
     * <p>
     * When true, remove users listed as creator or modifier of records and subrecords
     *
     */
    @JsonProperty("deidentifyAuditUser")
    @JsonPropertyDescription("When true, remove users listed as creator or modifier of records and subrecords")
    private Boolean deidentifyAuditUser;
    /**
     * Deidentified Reference Patterns
     * <p>
     * References to replace (pattern applies to document type)
     *
     */
    @JsonProperty("deidentifiedReferencePatterns")
    @JsonPropertyDescription("References to replace (pattern applies to document type)")
    private String deidentifiedReferencePatterns;
    /**
     * Access Groups to Include
     * <p>
     * names of access groups to that will NOT be removed
     *
     */
    @JsonProperty("accessGroupsToInclude")
    @JsonPropertyDescription("names of access groups to that will NOT be removed ")
    private String accessGroupsToInclude;
    /**
     * Access Groups to Remove
     * <p>
     * names of access groups to that WILL be removed
     *
     */
    @JsonProperty("accessGroupsToRemove")
    @JsonPropertyDescription("names of access groups to that WILL be removed ")
    private String accessGroupsToRemove;
    /**
     * Remove all Locked
     * <p>
     * When true, remove any data element that is marked as non-public
     *
     */
    @JsonProperty("removeAllLocked")
    @JsonPropertyDescription("When true, remove any data element that is marked as non-public")
    private Boolean removeAllLocked;
    /**
     * Remove Codes by System
     * <p>
     * When true, remove any Codes whose CodeSystem is on the list
     *
     */
    @JsonProperty("removeCodesBySystem")
    @JsonPropertyDescription("When true, remove any Codes whose CodeSystem is on the list")
    private Boolean removeCodesBySystem;
    /**
     * Code Systems to Remove
     * <p>
     * Code Systems to remove
     *
     */
    @JsonProperty("codeSystemsToRemove")
    @JsonPropertyDescription("Code Systems to remove")
    private String codeSystemsToRemove;
    /**
     * Code Systems to Keep
     * <p>
     * Code Systems to keep
     *
     */
    @JsonProperty("codeSystemsToKeep")
    @JsonPropertyDescription("Code Systems to keep")
    private String codeSystemsToKeep;
    /**
     * Remove References by Criteria
     * <p>
     * When true, remove any References that meet specified criteria
     *
     */
    @JsonProperty("removeReferencesByCriteria")
    @JsonPropertyDescription("When true, remove any References that meet specified criteria")
    private Boolean removeReferencesByCriteria;
    /**
     * Reference Types to Remove
     * <p>
     * Document Types to look at. When a Reference is of that document type, remove it
     *
     */
    @JsonProperty("referenceTypesToRemove")
    @JsonPropertyDescription("Document Types to look at. When a Reference is of that document type, remove it")
    private String referenceTypesToRemove;
    /**
     * Citation Patterns to Remove
     * <p>
     * Patterns (RegExes) to apply to Reference citation. When a citation matches, remove the Reference
     *
     */
    @JsonProperty("citationPatternsToRemove")
    @JsonPropertyDescription("Patterns (RegExes) to apply to Reference citation. When a citation matches, remove the Reference")
    private String citationPatternsToRemove;
    /**
     * Exclude Reference by Pattern
     * <p>
     * Remove References by looking at citationPatternsToRemove
     *
     */
    @JsonProperty("excludeReferenceByPattern")
    @JsonPropertyDescription("Remove References by looking at citationPatternsToRemove")
    private Boolean excludeReferenceByPattern;
    /**
     * Substance Reference Cleanup
     * <p>
     * When true, next criteria are used to process substance references
     *
     */
    @JsonProperty("substanceReferenceCleanup")
    @JsonPropertyDescription("When true, next criteria are used to process substance references")
    private Boolean substanceReferenceCleanup;
    /**
     * Remove References to Filtered Substances
     * <p>
     * When true, when a substance is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToFilteredSubstances")
    @JsonPropertyDescription("When true, when a substance is removed, remove any references to it")
    private Boolean removeReferencesToFilteredSubstances;
    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     * When true, when a substance's definition is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    @JsonPropertyDescription("When true, when a substance's definition is removed, remove any references to it")
    private Boolean removeReferencesToSubstancesNonExportedDefinitions;
    /**
     * Remove Notes
     * <p>
     * When true, remove all Notes
     *
     */
    @JsonProperty("removeNotes")
    @JsonPropertyDescription("When true, remove all Notes")
    private Boolean removeNotes;
    /**
     * Remove Change Reason
     * <p>
     * When true, delete the 'Change Reason' field
     *
     */
    @JsonProperty("removeChangeReason")
    @JsonPropertyDescription("When true, delete the 'Change Reason' field")
    private Boolean removeChangeReason;
    /**
     * Remove Approval Id
     * <p>
     * When true, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is removed
     *
     */
    @JsonProperty("removeApprovalId")
    @JsonPropertyDescription("When true, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is removed")
    private Boolean removeApprovalId;
    /**
     * Remove Approval Id
     * <p>
     * When this parameter has a value, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is copied to a code of this specified system
     *
     */
    @JsonProperty("approvalIdCodeSystem")
    @JsonPropertyDescription("When this parameter has a value, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is copied to a code of this specified system")
    private String approvalIdCodeSystem;
    /**
     * Regenerate UUIDs
     * <p>
     * When true, all UUIDs in the object being exported will be given a newly-generated value
     *
     */
    @JsonProperty("regenerateUUIDs")
    @JsonPropertyDescription("When true, all UUIDs in the object being exported will be given a newly-generated value")
    private Boolean regenerateUUIDs;
    /**
     * Change All Statuses
     * <p>
     * When true, all status value in the object being exported will be given a value
     *
     */
    @JsonProperty("changeAllStatuses")
    @JsonPropertyDescription("When true, all status value in the object being exported will be given a value")
    private Boolean changeAllStatuses;
    /**
     * New Status Value
     * <p>
     * new string value to assign to all individual status fields throughout the object
     *
     */
    @JsonProperty("newStatusValue")
    @JsonPropertyDescription("new string value to assign to all individual status fields throughout the object")
    private String newStatusValue;
    /**
     * New Auditor Value
     * <p>
     * new string value to assign to all auditor (creator/modifier) fields throughout the object
     *
     */
    @JsonProperty("newAuditorValue")
    @JsonPropertyDescription("new string value to assign to all auditor (creator/modifier) fields throughout the object")
    private String newAuditorValue;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Remove Date
     * <p>
     * When true, remove all date fields from output
     *
     */
    @JsonProperty("removeDate")
    public Boolean getRemoveDate() {
        return removeDate;
    }

    /**
     * Remove Date
     * <p>
     * When true, remove all date fields from output
     *
     */
    @JsonProperty("removeDate")
    public void setRemoveDate(Boolean removeDate) {
        this.removeDate = removeDate;
    }

    /**
     * Deidentify Audit User
     * <p>
     * When true, remove users listed as creator or modifier of records and subrecords
     *
     */
    @JsonProperty("deidentifyAuditUser")
    public Boolean getDeidentifyAuditUser() {
        return deidentifyAuditUser;
    }

    /**
     * Deidentify Audit User
     * <p>
     * When true, remove users listed as creator or modifier of records and subrecords
     *
     */
    @JsonProperty("deidentifyAuditUser")
    public void setDeidentifyAuditUser(Boolean deidentifyAuditUser) {
        this.deidentifyAuditUser = deidentifyAuditUser;
    }

    /**
     * Deidentified Reference Patterns
     * <p>
     * References to replace (pattern applies to document type)
     *
     */
    @JsonProperty("deidentifiedReferencePatterns")
    public String getDeidentifiedReferencePatterns() {
        return deidentifiedReferencePatterns;
    }

    /**
     * Deidentified Reference Patterns
     * <p>
     * References to replace (pattern applies to document type)
     *
     */
    @JsonProperty("deidentifiedReferencePatterns")
    public void setDeidentifiedReferencePatterns(String deidentifiedReferencePatterns) {
        this.deidentifiedReferencePatterns = deidentifiedReferencePatterns;
    }

    /**
     * Access Groups to Include
     * <p>
     * names of access groups to that will NOT be removed
     *
     */
    @JsonProperty("accessGroupsToInclude")
    public String getAccessGroupsToInclude() {
        return accessGroupsToInclude;
    }

    /**
     * Access Groups to Include
     * <p>
     * names of access groups to that will NOT be removed
     *
     */
    @JsonProperty("accessGroupsToInclude")
    public void setAccessGroupsToInclude(String accessGroupsToInclude) {
        this.accessGroupsToInclude = accessGroupsToInclude;
    }

    /**
     * Access Groups to Remove
     * <p>
     * names of access groups to that WILL be removed
     *
     */
    @JsonProperty("accessGroupsToRemove")
    public String getAccessGroupsToRemove() {
        return accessGroupsToRemove;
    }

    /**
     * Access Groups to Remove
     * <p>
     * names of access groups to that WILL be removed
     *
     */
    @JsonProperty("accessGroupsToRemove")
    public void setAccessGroupsToRemove(String accessGroupsToRemove) {
        this.accessGroupsToRemove = accessGroupsToRemove;
    }

    /**
     * Remove all Locked
     * <p>
     * When true, remove any data element that is marked as non-public
     *
     */
    @JsonProperty("removeAllLocked")
    public Boolean getRemoveAllLocked() {
        return removeAllLocked;
    }

    /**
     * Remove all Locked
     * <p>
     * When true, remove any data element that is marked as non-public
     *
     */
    @JsonProperty("removeAllLocked")
    public void setRemoveAllLocked(Boolean removeAllLocked) {
        this.removeAllLocked = removeAllLocked;
    }

    /**
     * Remove Codes by System
     * <p>
     * When true, remove any Codes whose CodeSystem is on the list
     *
     */
    @JsonProperty("removeCodesBySystem")
    public Boolean getRemoveCodesBySystem() {
        return removeCodesBySystem;
    }

    /**
     * Remove Codes by System
     * <p>
     * When true, remove any Codes whose CodeSystem is on the list
     *
     */
    @JsonProperty("removeCodesBySystem")
    public void setRemoveCodesBySystem(Boolean removeCodesBySystem) {
        this.removeCodesBySystem = removeCodesBySystem;
    }

    /**
     * Code Systems to Remove
     * <p>
     * Code Systems to remove
     *
     */
    @JsonProperty("codeSystemsToRemove")
    public String getCodeSystemsToRemove() {
        return codeSystemsToRemove;
    }

    /**
     * Code Systems to Remove
     * <p>
     * Code Systems to remove
     *
     */
    @JsonProperty("codeSystemsToRemove")
    public void setCodeSystemsToRemove(String codeSystemsToRemove) {
        this.codeSystemsToRemove = codeSystemsToRemove;
    }

    /**
     * Code Systems to Keep
     * <p>
     * Code Systems to keep
     *
     */
    @JsonProperty("codeSystemsToKeep")
    public String getCodeSystemsToKeep() {
        return codeSystemsToKeep;
    }

    /**
     * Code Systems to Keep
     * <p>
     * Code Systems to keep
     *
     */
    @JsonProperty("codeSystemsToKeep")
    public void setCodeSystemsToKeep(String codeSystemsToKeep) {
        this.codeSystemsToKeep = codeSystemsToKeep;
    }

    /**
     * Remove References by Criteria
     * <p>
     * When true, remove any References that meet specified criteria
     *
     */
    @JsonProperty("removeReferencesByCriteria")
    public Boolean getRemoveReferencesByCriteria() {
        return removeReferencesByCriteria;
    }

    /**
     * Remove References by Criteria
     * <p>
     * When true, remove any References that meet specified criteria
     *
     */
    @JsonProperty("removeReferencesByCriteria")
    public void setRemoveReferencesByCriteria(Boolean removeReferencesByCriteria) {
        this.removeReferencesByCriteria = removeReferencesByCriteria;
    }

    /**
     * Reference Types to Remove
     * <p>
     * Document Types to look at. When a Reference is of that document type, remove it
     *
     */
    @JsonProperty("referenceTypesToRemove")
    public String getReferenceTypesToRemove() {
        return referenceTypesToRemove;
    }

    /**
     * Reference Types to Remove
     * <p>
     * Document Types to look at. When a Reference is of that document type, remove it
     *
     */
    @JsonProperty("referenceTypesToRemove")
    public void setReferenceTypesToRemove(String referenceTypesToRemove) {
        this.referenceTypesToRemove = referenceTypesToRemove;
    }

    /**
     * Citation Patterns to Remove
     * <p>
     * Patterns (RegExes) to apply to Reference citation. When a citation matches, remove the Reference
     *
     */
    @JsonProperty("citationPatternsToRemove")
    public String getCitationPatternsToRemove() {
        return citationPatternsToRemove;
    }

    /**
     * Citation Patterns to Remove
     * <p>
     * Patterns (RegExes) to apply to Reference citation. When a citation matches, remove the Reference
     *
     */
    @JsonProperty("citationPatternsToRemove")
    public void setCitationPatternsToRemove(String citationPatternsToRemove) {
        this.citationPatternsToRemove = citationPatternsToRemove;
    }

    /**
     * Exclude Reference by Pattern
     * <p>
     * Remove References by looking at citationPatternsToRemove
     *
     */
    @JsonProperty("excludeReferenceByPattern")
    public Boolean getExcludeReferenceByPattern() {
        return excludeReferenceByPattern;
    }

    /**
     * Exclude Reference by Pattern
     * <p>
     * Remove References by looking at citationPatternsToRemove
     *
     */
    @JsonProperty("excludeReferenceByPattern")
    public void setExcludeReferenceByPattern(Boolean excludeReferenceByPattern) {
        this.excludeReferenceByPattern = excludeReferenceByPattern;
    }

    /**
     * Substance Reference Cleanup
     * <p>
     * When true, next criteria are used to process substance references
     *
     */
    @JsonProperty("substanceReferenceCleanup")
    public Boolean getSubstanceReferenceCleanup() {
        return substanceReferenceCleanup;
    }

    /**
     * Substance Reference Cleanup
     * <p>
     * When true, next criteria are used to process substance references
     *
     */
    @JsonProperty("substanceReferenceCleanup")
    public void setSubstanceReferenceCleanup(Boolean substanceReferenceCleanup) {
        this.substanceReferenceCleanup = substanceReferenceCleanup;
    }

    /**
     * Remove References to Filtered Substances
     * <p>
     * When true, when a substance is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToFilteredSubstances")
    public Boolean getRemoveReferencesToFilteredSubstances() {
        return removeReferencesToFilteredSubstances;
    }

    /**
     * Remove References to Filtered Substances
     * <p>
     * When true, when a substance is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToFilteredSubstances")
    public void setRemoveReferencesToFilteredSubstances(Boolean removeReferencesToFilteredSubstances) {
        this.removeReferencesToFilteredSubstances = removeReferencesToFilteredSubstances;
    }

    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     * When true, when a substance's definition is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    public Boolean getRemoveReferencesToSubstancesNonExportedDefinitions() {
        return removeReferencesToSubstancesNonExportedDefinitions;
    }

    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     * When true, when a substance's definition is removed, remove any references to it
     *
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    public void setRemoveReferencesToSubstancesNonExportedDefinitions(Boolean removeReferencesToSubstancesNonExportedDefinitions) {
        this.removeReferencesToSubstancesNonExportedDefinitions = removeReferencesToSubstancesNonExportedDefinitions;
    }

    /**
     * Remove Notes
     * <p>
     * When true, remove all Notes
     *
     */
    @JsonProperty("removeNotes")
    public Boolean getRemoveNotes() {
        return removeNotes;
    }

    /**
     * Remove Notes
     * <p>
     * When true, remove all Notes
     *
     */
    @JsonProperty("removeNotes")
    public void setRemoveNotes(Boolean removeNotes) {
        this.removeNotes = removeNotes;
    }

    /**
     * Remove Change Reason
     * <p>
     * When true, delete the 'Change Reason' field
     *
     */
    @JsonProperty("removeChangeReason")
    public Boolean getRemoveChangeReason() {
        return removeChangeReason;
    }

    /**
     * Remove Change Reason
     * <p>
     * When true, delete the 'Change Reason' field
     *
     */
    @JsonProperty("removeChangeReason")
    public void setRemoveChangeReason(Boolean removeChangeReason) {
        this.removeChangeReason = removeChangeReason;
    }

    /**
     * Remove Approval Id
     * <p>
     * When true, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is removed
     *
     */
    @JsonProperty("removeApprovalId")
    public Boolean getRemoveApprovalId() {
        return removeApprovalId;
    }

    /**
     * Remove Approval Id
     * <p>
     * When true, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is removed
     *
     */
    @JsonProperty("removeApprovalId")
    public void setRemoveApprovalId(Boolean removeApprovalId) {
        this.removeApprovalId = removeApprovalId;
    }

    /**
     * Remove Approval Id
     * <p>
     * When this parameter has a value, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is copied to a code of this specified system
     *
     */
    @JsonProperty("approvalIdCodeSystem")
    public String getApprovalIdCodeSystem() {
        return approvalIdCodeSystem;
    }

    /**
     * Remove Approval Id
     * <p>
     * When this parameter has a value, the record's approval ID (system-generated identifier created when the substance is verified by a second registrar) is copied to a code of this specified system
     *
     */
    @JsonProperty("approvalIdCodeSystem")
    public void setApprovalIdCodeSystem(String approvalIdCodeSystem) {
        this.approvalIdCodeSystem = approvalIdCodeSystem;
    }

    /**
     * Regenerate UUIDs
     * <p>
     * When true, all UUIDs in the object being exported will be given a newly-generated value
     *
     */
    @JsonProperty("regenerateUUIDs")
    public Boolean getRegenerateUUIDs() {
        return regenerateUUIDs;
    }

    /**
     * Regenerate UUIDs
     * <p>
     * When true, all UUIDs in the object being exported will be given a newly-generated value
     *
     */
    @JsonProperty("regenerateUUIDs")
    public void setRegenerateUUIDs(Boolean regenerateUUIDs) {
        this.regenerateUUIDs = regenerateUUIDs;
    }

    /**
     * Change All Statuses
     * <p>
     * When true, all status value in the object being exported will be given a value
     *
     */
    @JsonProperty("changeAllStatuses")
    public Boolean getChangeAllStatuses() {
        return changeAllStatuses;
    }

    /**
     * Change All Statuses
     * <p>
     * When true, all status value in the object being exported will be given a value
     *
     */
    @JsonProperty("changeAllStatuses")
    public void setChangeAllStatuses(Boolean changeAllStatuses) {
        this.changeAllStatuses = changeAllStatuses;
    }

    /**
     * New Status Value
     * <p>
     * new string value to assign to all individual status fields throughout the object
     *
     */
    @JsonProperty("newStatusValue")
    public String getNewStatusValue() {
        return newStatusValue;
    }

    /**
     * New Status Value
     * <p>
     * new string value to assign to all individual status fields throughout the object
     *
     */
    @JsonProperty("newStatusValue")
    public void setNewStatusValue(String newStatusValue) {
        this.newStatusValue = newStatusValue;
    }

    /**
     * New Auditor Value
     * <p>
     * new string value to assign to all auditor (creator/modifier) fields throughout the object
     *
     */
    @JsonProperty("newAuditorValue")
    public String getNewAuditorValue() {
        return newAuditorValue;
    }

    /**
     * New Auditor Value
     * <p>
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
