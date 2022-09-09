package ix.ginas.exporters;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ScrubberExportSettings {
    private List<String> allowedGroups = new ArrayList<>();
    private List<String> prohibitedGroups = new ArrayList<>();
    private boolean onlyPublic;
    private boolean removeDates;
    private boolean removeAuditorInfo;
    private List<String> nameTypesToRemove = new ArrayList<>();
    private List<String> relationshipTypesToRemove = new ArrayList<>();
}
