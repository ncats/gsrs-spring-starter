package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Facet {
    void setSelectedLabel(String... label);

    void setPrefix(String prefix);

    String getPrefix();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Set<String> getSelectedLabels();

    @JsonIgnore
    List<FV> getSelectedFV();

    @JsonIgnore
    Set<String> getMissingSelections();

    String getName();

    List<FV> getValues();

    int size();

    FV getValue(int index);

    String getLabel(int index);

    Integer getCount(int index);

    Integer getCount(String label);

    void sort();

    Facet filter(FacetFilter filter);

    void sortLabels(boolean desc);

    void sortCounts(boolean desc);

    Map<String,Integer> toCountMap();

    @JsonIgnore
    ArrayList<String> getLabelString();

    @JsonIgnore
    ArrayList<Integer> getLabelCount();

    FV add(String label, Integer count);

    void setSelectedLabels(List<String> collect);
}
