package ix.ginas.exporters;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Scrubber Parameters
 * <p>
 * Factors that control the behavior of a Java class that removes private parts of a data object before the object is shared
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "removeDates",
        "deidentifyAuditUser",
        "deidentifiedReferencePatterns",
        "accessGroupsToInclude",
        "accessGroupsToRemove",
        "removeElementsIfNoExportablePublicRef",
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
        "approvalIdCleanup",
        "removeApprovalId",
        "copyApprovalIdToCode",
        "approvalIdCodeSystem",
        "regenerateUUIDs",
        "changeAllStatuses",
        "newStatusValue",
        "AuditInformationCleanup",
        "newAuditorValue",
        "removeAllEntryTimestamps",
        "scrubbedDefinitionHandling",
        "removeScrubbedDefinitionalElementsEntirely",
        "setScrubbedDefinitionalElementsIncomplete",
        "convertScrubbedDefinitionsToConcepts",
        "addNoteToScrubbedDefinitions"
})
@Generated("jsonschema2pojo")
public class ScrubberParameterSchema {

    /**
     * Remove Date
     * <p>
     *
     *
     */
    @JsonProperty("removeDates")
    private Boolean removeDates;
    /**
     * Deidentify Audit User
     * <p>
     *
     *
     */
    @JsonProperty("deidentifyAuditUser")
    private Boolean deidentifyAuditUser;
    /**
     * Deidentified Reference Patterns
     * <p>
     *
     *
     */
    @JsonProperty("deidentifiedReferencePatterns")
    private String deidentifiedReferencePatterns;
    /**
     * Access Groups to Include
     * <p>
     *
     *
     */
    @JsonProperty("accessGroupsToInclude")
    private String accessGroupsToInclude;
    /**
     * Access Groups to Remove
     * <p>
     *
     *
     */
    @JsonProperty("accessGroupsToRemove")
    private String accessGroupsToRemove;
    /**
     * Remove Elements if no exportable selected public domain reference
     * <p>
     *
     *
     */
    @JsonProperty("removeElementsIfNoExportablePublicRef")
    private String removeElementsIfNoExportablePublicRef;
    /**
     * Remove all Locked
     * <p>
     *
     *
     */
    @JsonProperty("removeAllLocked")
    private Boolean removeAllLocked;
    /**
     * Remove Codes by System
     * <p>
     *
     *
     */
    @JsonProperty("removeCodesBySystem")
    private Boolean removeCodesBySystem;
    /**
     * Code Systems to Remove
     * <p>
     *
     *
     */
    @JsonProperty("codeSystemsToRemove")
    private String codeSystemsToRemove;
    /**
     * Code Systems to Keep
     * <p>
     *
     *
     */
    @JsonProperty("codeSystemsToKeep")
    private String codeSystemsToKeep;
    /**
     * Remove References by Criteria
     * <p>
     *
     *
     */
    @JsonProperty("removeReferencesByCriteria")
    private Boolean removeReferencesByCriteria;
    /**
     * Reference Types to Remove
     * <p>
     *
     *
     */
    @JsonProperty("referenceTypesToRemove")
    private String referenceTypesToRemove;
    /**
     * Citation Patterns to Remove
     * <p>
     *
     *
     */
    @JsonProperty("citationPatternsToRemove")
    private String citationPatternsToRemove;
    /**
     * Exclude Reference by Pattern
     * <p>
     *
     *
     */
    @JsonProperty("excludeReferenceByPattern")
    private Boolean excludeReferenceByPattern;
    /**
     * Substance Reference Cleanup
     * <p>
     *
     *
     */
    @JsonProperty("substanceReferenceCleanup")
    private Boolean substanceReferenceCleanup;
    /**
     * Remove References to Filtered Substances
     * <p>
     *
     *
     */
    @JsonProperty("removeReferencesToFilteredSubstances")
    private Boolean removeReferencesToFilteredSubstances;
    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     *
     *
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    private Boolean removeReferencesToSubstancesNonExportedDefinitions;
    /**
     * Remove Notes
     * <p>
     *
     *
     */
    @JsonProperty("removeNotes")
    private Boolean removeNotes;
    /**
     * Remove Change Reason
     * <p>
     *
     *
     */
    @JsonProperty("removeChangeReason")
    private Boolean removeChangeReason;
    /**
     * Approval Id clean-up
     * <p>
     *
     *
     */
    @JsonProperty("approvalIdCleanup")
    private Boolean approvalIdCleanup;
    /**
     * Remove Approval Id
     * <p>
     *
     *
     */
    @JsonProperty("removeApprovalId")
    private Boolean removeApprovalId;
    /**
     * Copy Approval Id to code if code not already present
     * <p>
     *
     *
     */
    @JsonProperty("copyApprovalIdToCode")
    private Boolean copyApprovalIdToCode;
    /**
     * Remove Approval Id
     * <p>
     *
     *
     */
    @JsonProperty("approvalIdCodeSystem")
    private String approvalIdCodeSystem;
    /**
     * Regenerate UUIDs
     * <p>
     *
     *
     */
    @JsonProperty("regenerateUUIDs")
    private Boolean regenerateUUIDs;
    /**
     * Change All Statuses
     * <p>
     *
     *
     */
    @JsonProperty("changeAllStatuses")
    private Boolean changeAllStatuses;
    /**
     * New Status Value
     * <p>
     *
     *
     */
    @JsonProperty("newStatusValue")
    private String newStatusValue;
    /**
     * Audit Information clean-up
     * <p>
     *
     *
     */
    @JsonProperty("AuditInformationCleanup")
    private Boolean auditInformationCleanup;
    /**
     * New Auditor Value
     * <p>
     *
     *
     */
    @JsonProperty("newAuditorValue")
    private String newAuditorValue;
    /**
     * Remove all entry timestamps
     * <p>
     *
     *
     */
    @JsonProperty("removeAllEntryTimestamps")
    private Boolean removeAllEntryTimestamps;
    /**
     * Scrubbed Definition Handling
     * <p>
     *
     *
     */
    @JsonProperty("scrubbedDefinitionHandling")
    private Boolean scrubbedDefinitionHandling;
    /**
     * Remove partially/fully scrubbed definitional records entirely
     * <p>
     *
     *
     */
    @JsonProperty("removeScrubbedDefinitionalElementsEntirely")
    private Boolean removeScrubbedDefinitionalElementsEntirely;
    /**
     * Set partially/fully scrubbed definitional records to definitional level "Incomplete"
     * <p>
     *
     *
     */
    @JsonProperty("setScrubbedDefinitionalElementsIncomplete")
    private Boolean setScrubbedDefinitionalElementsIncomplete;
    /**
     * Convert partially/fully scrubbed definitional records to "Concepts"
     * <p>
     *
     *
     */
    @JsonProperty("convertScrubbedDefinitionsToConcepts")
    private Boolean convertScrubbedDefinitionsToConcepts;
    /**
     * add a note to partially/fully scrubbed definitional records
     * <p>
     *
     *
     */
    @JsonProperty("addNoteToScrubbedDefinitions")
    private String addNoteToScrubbedDefinitions;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Remove Date
     * <p>
     *
     *
     */
    @JsonProperty("removeDates")
    public Boolean getRemoveDates() {
        return removeDates;
    }

    /**
     * Remove Date
     * <p>
     *
     *
     */
    @JsonProperty("removeDates")
    public void setRemoveDates(Boolean removeDates) {
        this.removeDates = removeDates;
    }

    /**
     * Deidentify Audit User
     * <p>
     *
     *
     */
    @JsonProperty("deidentifyAuditUser")
    public Boolean getDeidentifyAuditUser() {
        return deidentifyAuditUser;
    }

    /**
     * Deidentify Audit User
     * <p>
     *
     *
     */
    @JsonProperty("deidentifyAuditUser")
    public void setDeidentifyAuditUser(Boolean deidentifyAuditUser) {
        this.deidentifyAuditUser = deidentifyAuditUser;
    }

    /**
     * Deidentified Reference Patterns
     * <p>
     *
     *
     */
    @JsonProperty("deidentifiedReferencePatterns")
    public String getDeidentifiedReferencePatterns() {
        return deidentifiedReferencePatterns;
    }

    /**
     * Deidentified Reference Patterns
     * <p>
     *
     *
     */
    @JsonProperty("deidentifiedReferencePatterns")
    public void setDeidentifiedReferencePatterns(String deidentifiedReferencePatterns) {
        this.deidentifiedReferencePatterns = deidentifiedReferencePatterns;
    }

    /**
     * Access Groups to Include
     * <p>
     *
     *
     */
    @JsonProperty("accessGroupsToInclude")
    public String getAccessGroupsToInclude() {
        return accessGroupsToInclude;
    }

    /**
     * Access Groups to Include
     * <p>
     *
     *
     */
    @JsonProperty("accessGroupsToInclude")
    public void setAccessGroupsToInclude(String accessGroupsToInclude) {
        this.accessGroupsToInclude = accessGroupsToInclude;
    }

    /**
     * Access Groups to Remove
     * <p>
     *
     *
     */
    @JsonProperty("accessGroupsToRemove")
    public String getAccessGroupsToRemove() {
        return accessGroupsToRemove;
    }

    /**
     * Access Groups to Remove
     * <p>
     *
     *
     */
    @JsonProperty("accessGroupsToRemove")
    public void setAccessGroupsToRemove(String accessGroupsToRemove) {
        this.accessGroupsToRemove = accessGroupsToRemove;
    }

    /**
     * Remove Elements if no exportable selected public domain reference
     * <p>
     *
     *
     */
    @JsonProperty("removeElementsIfNoExportablePublicRef")
    public String getRemoveElementsIfNoExportablePublicRef() {
        return removeElementsIfNoExportablePublicRef;
    }

    /**
     * Remove Elements if no exportable selected public domain reference
     * <p>
     *
     *
     */
    @JsonProperty("removeElementsIfNoExportablePublicRef")
    public void setRemoveElementsIfNoExportablePublicRef(String removeElementsIfNoExportablePublicRef) {
        this.removeElementsIfNoExportablePublicRef = removeElementsIfNoExportablePublicRef;
    }

    /**
     * Remove all Locked
     * <p>
     *
     *
     */
    @JsonProperty("removeAllLocked")
    public Boolean getRemoveAllLocked() {
        return removeAllLocked;
    }

    /**
     * Remove all Locked
     * <p>
     *
     *
     */
    @JsonProperty("removeAllLocked")
    public void setRemoveAllLocked(Boolean removeAllLocked) {
        this.removeAllLocked = removeAllLocked;
    }

    /**
     * Remove Codes by System
     * <p>
     *
     *
     */
    @JsonProperty("removeCodesBySystem")
    public Boolean getRemoveCodesBySystem() {
        return removeCodesBySystem;
    }

    /**
     * Remove Codes by System
     * <p>
     *
     *
     */
    @JsonProperty("removeCodesBySystem")
    public void setRemoveCodesBySystem(Boolean removeCodesBySystem) {
        this.removeCodesBySystem = removeCodesBySystem;
    }

    /**
     * Code Systems to Remove
     * <p>
     *
     *
     */
    @JsonProperty("codeSystemsToRemove")
    public String getCodeSystemsToRemove() {
        return codeSystemsToRemove;
    }

    /**
     * Code Systems to Remove
     * <p>
     *
     *
     */
    @JsonProperty("codeSystemsToRemove")
    public void setCodeSystemsToRemove(String codeSystemsToRemove) {
        this.codeSystemsToRemove = codeSystemsToRemove;
    }

    /**
     * Code Systems to Keep
     * <p>
     *
     *
     */
    @JsonProperty("codeSystemsToKeep")
    public String getCodeSystemsToKeep() {
        return codeSystemsToKeep;
    }

    /**
     * Code Systems to Keep
     * <p>
     *
     *
     */
    @JsonProperty("codeSystemsToKeep")
    public void setCodeSystemsToKeep(String codeSystemsToKeep) {
        this.codeSystemsToKeep = codeSystemsToKeep;
    }

    /**
     * Remove References by Criteria
     * <p>
     *
     *
     */
    @JsonProperty("removeReferencesByCriteria")
    public Boolean getRemoveReferencesByCriteria() {
        return removeReferencesByCriteria;
    }

    /**
     * Remove References by Criteria
     * <p>
     *
     *
     */
    @JsonProperty("removeReferencesByCriteria")
    public void setRemoveReferencesByCriteria(Boolean removeReferencesByCriteria) {
        this.removeReferencesByCriteria = removeReferencesByCriteria;
    }

    /**
     * Reference Types to Remove
     * <p>
     *
     *
     */
    @JsonProperty("referenceTypesToRemove")
    public String getReferenceTypesToRemove() {
        return referenceTypesToRemove;
    }

    /**
     * Reference Types to Remove
     * <p>
     *
     *
     */
    @JsonProperty("referenceTypesToRemove")
    public void setReferenceTypesToRemove(String referenceTypesToRemove) {
        this.referenceTypesToRemove = referenceTypesToRemove;
    }

    /**
     * Citation Patterns to Remove
     * <p>
     *
     *
     */
    @JsonProperty("citationPatternsToRemove")
    public String getCitationPatternsToRemove() {
        return citationPatternsToRemove;
    }

    /**
     * Citation Patterns to Remove
     * <p>
     *
     *
     */
    @JsonProperty("citationPatternsToRemove")
    public void setCitationPatternsToRemove(String citationPatternsToRemove) {
        this.citationPatternsToRemove = citationPatternsToRemove;
    }

    /**
     * Exclude Reference by Pattern
     * <p>
     *
     *
     */
    @JsonProperty("excludeReferenceByPattern")
    public Boolean getExcludeReferenceByPattern() {
        return excludeReferenceByPattern;
    }

    /**
     * Exclude Reference by Pattern
     * <p>
     *
     *
     */
    @JsonProperty("excludeReferenceByPattern")
    public void setExcludeReferenceByPattern(Boolean excludeReferenceByPattern) {
        this.excludeReferenceByPattern = excludeReferenceByPattern;
    }

    /**
     * Substance Reference Cleanup
     * <p>
     *
     *
     */
    @JsonProperty("substanceReferenceCleanup")
    public Boolean getSubstanceReferenceCleanup() {
        return substanceReferenceCleanup;
    }

    /**
     * Substance Reference Cleanup
     * <p>
     *
     *
     */
    @JsonProperty("substanceReferenceCleanup")
    public void setSubstanceReferenceCleanup(Boolean substanceReferenceCleanup) {
        this.substanceReferenceCleanup = substanceReferenceCleanup;
    }

    /**
     * Remove References to Filtered Substances
     * <p>
     *
     *
     */
    @JsonProperty("removeReferencesToFilteredSubstances")
    public Boolean getRemoveReferencesToFilteredSubstances() {
        return removeReferencesToFilteredSubstances;
    }

    /**
     * Remove References to Filtered Substances
     * <p>
     *
     *
     */
    @JsonProperty("removeReferencesToFilteredSubstances")
    public void setRemoveReferencesToFilteredSubstances(Boolean removeReferencesToFilteredSubstances) {
        this.removeReferencesToFilteredSubstances = removeReferencesToFilteredSubstances;
    }

    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     *
     *
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    public Boolean getRemoveReferencesToSubstancesNonExportedDefinitions() {
        return removeReferencesToSubstancesNonExportedDefinitions;
    }

    /**
     * Remove References to Substances Non-Exported Definitions
     * <p>
     *
     *
     */
    @JsonProperty("removeReferencesToSubstancesNonExportedDefinitions")
    public void setRemoveReferencesToSubstancesNonExportedDefinitions(Boolean removeReferencesToSubstancesNonExportedDefinitions) {
        this.removeReferencesToSubstancesNonExportedDefinitions = removeReferencesToSubstancesNonExportedDefinitions;
    }

    /**
     * Remove Notes
     * <p>
     *
     *
     */
    @JsonProperty("removeNotes")
    public Boolean getRemoveNotes() {
        return removeNotes;
    }

    /**
     * Remove Notes
     * <p>
     *
     *
     */
    @JsonProperty("removeNotes")
    public void setRemoveNotes(Boolean removeNotes) {
        this.removeNotes = removeNotes;
    }

    /**
     * Remove Change Reason
     * <p>
     *
     *
     */
    @JsonProperty("removeChangeReason")
    public Boolean getRemoveChangeReason() {
        return removeChangeReason;
    }

    /**
     * Remove Change Reason
     * <p>
     *
     *
     */
    @JsonProperty("removeChangeReason")
    public void setRemoveChangeReason(Boolean removeChangeReason) {
        this.removeChangeReason = removeChangeReason;
    }

    /**
     * Approval Id clean-up
     * <p>
     *
     *
     */
    @JsonProperty("approvalIdCleanup")
    public Boolean getApprovalIdCleanup() {
        return approvalIdCleanup;
    }

    /**
     * Approval Id clean-up
     * <p>
     *
     *
     */
    @JsonProperty("approvalIdCleanup")
    public void setApprovalIdCleanup(Boolean approvalIdCleanup) {
        this.approvalIdCleanup = approvalIdCleanup;
    }

    /**
     * Remove Approval Id
     * <p>
     *
     *
     */
    @JsonProperty("removeApprovalId")
    public Boolean getRemoveApprovalId() {
        return removeApprovalId;
    }

    /**
     * Remove Approval Id
     * <p>
     *
     *
     */
    @JsonProperty("removeApprovalId")
    public void setRemoveApprovalId(Boolean removeApprovalId) {
        this.removeApprovalId = removeApprovalId;
    }

    /**
     * Copy Approval Id to code if code not already present
     * <p>
     *
     *
     */
    @JsonProperty("copyApprovalIdToCode")
    public Boolean getCopyApprovalIdToCode() {
        return copyApprovalIdToCode;
    }

    /**
     * Copy Approval Id to code if code not already present
     * <p>
     *
     *
     */
    @JsonProperty("copyApprovalIdToCode")
    public void setCopyApprovalIdToCode(Boolean copyApprovalIdToCode) {
        this.copyApprovalIdToCode = copyApprovalIdToCode;
    }

    /**
     * Remove Approval Id
     * <p>
     *
     *
     */
    @JsonProperty("approvalIdCodeSystem")
    public String getApprovalIdCodeSystem() {
        return approvalIdCodeSystem;
    }

    /**
     * Remove Approval Id
     * <p>
     *
     *
     */
    @JsonProperty("approvalIdCodeSystem")
    public void setApprovalIdCodeSystem(String approvalIdCodeSystem) {
        this.approvalIdCodeSystem = approvalIdCodeSystem;
    }

    /**
     * Regenerate UUIDs
     * <p>
     *
     *
     */
    @JsonProperty("regenerateUUIDs")
    public Boolean getRegenerateUUIDs() {
        return regenerateUUIDs;
    }

    /**
     * Regenerate UUIDs
     * <p>
     *
     *
     */
    @JsonProperty("regenerateUUIDs")
    public void setRegenerateUUIDs(Boolean regenerateUUIDs) {
        this.regenerateUUIDs = regenerateUUIDs;
    }

    /**
     * Change All Statuses
     * <p>
     *
     *
     */
    @JsonProperty("changeAllStatuses")
    public Boolean getChangeAllStatuses() {
        return changeAllStatuses;
    }

    /**
     * Change All Statuses
     * <p>
     *
     *
     */
    @JsonProperty("changeAllStatuses")
    public void setChangeAllStatuses(Boolean changeAllStatuses) {
        this.changeAllStatuses = changeAllStatuses;
    }

    /**
     * New Status Value
     * <p>
     *
     *
     */
    @JsonProperty("newStatusValue")
    public String getNewStatusValue() {
        return newStatusValue;
    }

    /**
     * New Status Value
     * <p>
     *
     *
     */
    @JsonProperty("newStatusValue")
    public void setNewStatusValue(String newStatusValue) {
        this.newStatusValue = newStatusValue;
    }

    /**
     * Audit Information clean-up
     * <p>
     *
     *
     */
    @JsonProperty("AuditInformationCleanup")
    public Boolean getAuditInformationCleanup() {
        return auditInformationCleanup;
    }

    /**
     * Audit Information clean-up
     * <p>
     *
     *
     */
    @JsonProperty("AuditInformationCleanup")
    public void setAuditInformationCleanup(Boolean auditInformationCleanup) {
        this.auditInformationCleanup = auditInformationCleanup;
    }

    /**
     * New Auditor Value
     * <p>
     *
     *
     */
    @JsonProperty("newAuditorValue")
    public String getNewAuditorValue() {
        return newAuditorValue;
    }

    /**
     * New Auditor Value
     * <p>
     *
     *
     */
    @JsonProperty("newAuditorValue")
    public void setNewAuditorValue(String newAuditorValue) {
        this.newAuditorValue = newAuditorValue;
    }

    /**
     * Remove all entry timestamps
     * <p>
     *
     *
     */
    @JsonProperty("removeAllEntryTimestamps")
    public Boolean getRemoveAllEntryTimestamps() {
        return removeAllEntryTimestamps;
    }

    /**
     * Remove all entry timestamps
     * <p>
     *
     *
     */
    @JsonProperty("removeAllEntryTimestamps")
    public void setRemoveAllEntryTimestamps(Boolean removeAllEntryTimestamps) {
        this.removeAllEntryTimestamps = removeAllEntryTimestamps;
    }

    /**
     * Scrubbed Definition Handling
     * <p>
     *
     *
     */
    @JsonProperty("scrubbedDefinitionHandling")
    public Boolean getScrubbedDefinitionHandling() {
        return scrubbedDefinitionHandling;
    }

    /**
     * Scrubbed Definition Handling
     * <p>
     *
     *
     */
    @JsonProperty("scrubbedDefinitionHandling")
    public void setScrubbedDefinitionHandling(Boolean scrubbedDefinitionHandling) {
        this.scrubbedDefinitionHandling = scrubbedDefinitionHandling;
    }

    /**
     * Remove partially/fully scrubbed definitional records entirely
     * <p>
     *
     *
     */
    @JsonProperty("removeScrubbedDefinitionalElementsEntirely")
    public Boolean getRemoveScrubbedDefinitionalElementsEntirely() {
        return removeScrubbedDefinitionalElementsEntirely;
    }

    /**
     * Remove partially/fully scrubbed definitional records entirely
     * <p>
     *
     *
     */
    @JsonProperty("removeScrubbedDefinitionalElementsEntirely")
    public void setRemoveScrubbedDefinitionalElementsEntirely(Boolean removeScrubbedDefinitionalElementsEntirely) {
        this.removeScrubbedDefinitionalElementsEntirely = removeScrubbedDefinitionalElementsEntirely;
    }

    /**
     * Set partially/fully scrubbed definitional records to definitional level "Incomplete"
     * <p>
     *
     *
     */
    @JsonProperty("setScrubbedDefinitionalElementsIncomplete")
    public Boolean getSetScrubbedDefinitionalElementsIncomplete() {
        return setScrubbedDefinitionalElementsIncomplete;
    }

    /**
     * Set partially/fully scrubbed definitional records to definitional level "Incomplete"
     * <p>
     *
     *
     */
    @JsonProperty("setScrubbedDefinitionalElementsIncomplete")
    public void setSetScrubbedDefinitionalElementsIncomplete(Boolean setScrubbedDefinitionalElementsIncomplete) {
        this.setScrubbedDefinitionalElementsIncomplete = setScrubbedDefinitionalElementsIncomplete;
    }

    /**
     * Convert partially/fully scrubbed definitional records to "Concepts"
     * <p>
     *
     *
     */
    @JsonProperty("convertScrubbedDefinitionsToConcepts")
    public Boolean getConvertScrubbedDefinitionsToConcepts() {
        return convertScrubbedDefinitionsToConcepts;
    }

    /**
     * Convert partially/fully scrubbed definitional records to "Concepts"
     * <p>
     *
     *
     */
    @JsonProperty("convertScrubbedDefinitionsToConcepts")
    public void setConvertScrubbedDefinitionsToConcepts(Boolean convertScrubbedDefinitionsToConcepts) {
        this.convertScrubbedDefinitionsToConcepts = convertScrubbedDefinitionsToConcepts;
    }

    /**
     * add a note to partially/fully scrubbed definitional records
     * <p>
     *
     *
     */
    @JsonProperty("addNoteToScrubbedDefinitions")
    public String getAddNoteToScrubbedDefinitions() {
        return addNoteToScrubbedDefinitions;
    }

    /**
     * add a note to partially/fully scrubbed definitional records
     * <p>
     *
     *
     */
    @JsonProperty("addNoteToScrubbedDefinitions")
    public void setAddNoteToScrubbedDefinitions(String addNoteToScrubbedDefinitions) {
        this.addNoteToScrubbedDefinitions = addNoteToScrubbedDefinitions;
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
