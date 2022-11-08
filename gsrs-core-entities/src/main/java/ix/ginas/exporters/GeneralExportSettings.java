package ix.ginas.exporters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralExportSettings {

    public enum ExportExpansion{
        None,
        OneGeneration,
        AllGenerations
    };
    private boolean removeApprovalId =false; //clear out the approval ID
    private boolean copyApprovalIdToCode = false; //Create a code with the same value as the approval ID.  There must be a value in approvalIdCodeSystem
    private String approvalIdCodeSystem; //code system for approval ID code
    private boolean removeUuids=false;  //clean out UUIDs from export
    private boolean changeAllRecordStatuses = false; //replace the status of each record
    private String newRecordStatus; //new value for the status
    private boolean setAllAuditorsToAbstractUser; //when true, replace the value of created by and modified by fields for each object at all levels
    private String newAbstractUser; //new user for audit fields
    private boolean generateNewUuids = false; //assign new values to all UUIDs at all levels
    private ExportExpansion definitionalExpansion = ExportExpansion.None;//include objects required by the definition of the selected object
        //for example, the components of a mixture

    private ExportExpansion referentialExpansion = ExportExpansion.None; //include object referenced by the selected object within relationships, etc.
    private List<String> codeSystemsForReferences;
}

