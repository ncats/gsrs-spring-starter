package gsrs.dataexchange.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingActionConfigSet {

    List<ProcessingActionConfig> processingActions = new ArrayList<>();

    List<MatchedIDs> stagingAreaRecords;

    @JsonIgnore
    public void addAction(ProcessingActionConfig actionConfig) {
        processingActions.add(actionConfig);
    }
}
