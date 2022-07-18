package gsrs.holdingArea.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
public class MatchableCalculationConfig {
    private Class objectClass;
    private List<String> codeSystems;
    private boolean includeApprovalId =true;
    private boolean includeUuid = true;
    private boolean includeDisplayName=true;
}
