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
 * Expander Parameters
 * <p>
 * Factors that control the behavior of a Java class that appends related objects when examining a data object before the object is shared
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "includeDefinitionalItems",
        "definitionalGenerations",
        "includedRelatedItems",
        "relatedGenerations",
        "includeModifyingItems",
        "includeMediatingItems"
})
@Generated("jsonschema2pojo")
public class ExpanderParameterSchema {

    /**
     * Include definitional objects
     * <p>
     *
     *
     */
    @JsonProperty("includeDefinitionalItems")
    private Boolean includeDefinitionalItems;
    /**
     * Number of generations of objects to include
     * <p>
     *
     *
     */
    @JsonProperty("definitionalGenerations")
    private Integer definitionalGenerations;
    /**
     * Include related objects
     * <p>
     *
     *
     */
    @JsonProperty("includedRelatedItems")
    private Boolean includedRelatedItems;
    /**
     * Number of generations of relationships to include
     * <p>
     *
     *
     */
    @JsonProperty("relatedGenerations")
    private Integer relatedGenerations;
    /**
     * Include modification items
     * <p>
     *
     *
     */
    @JsonProperty("includeModifyingItems")
    private Boolean includeModifyingItems;
    /**
     * Include objects mentioned in amounts
     * <p>
     *
     *
     */
    @JsonProperty("includeMediatingItems")
    private Boolean includeMediatingItems;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Include definitional objects
     * <p>
     *
     *
     */
    @JsonProperty("includeDefinitionalItems")
    public Boolean getIncludeDefinitionalItems() {
        return includeDefinitionalItems;
    }

    /**
     * Include definitional objects
     * <p>
     *
     *
     */
    @JsonProperty("includeDefinitionalItems")
    public void setIncludeDefinitionalItems(Boolean includeDefinitionalItems) {
        this.includeDefinitionalItems = includeDefinitionalItems;
    }

    /**
     * Number of generations of objects to include
     * <p>
     *
     *
     */
    @JsonProperty("definitionalGenerations")
    public Integer getDefinitionalGenerations() {
        return definitionalGenerations;
    }

    /**
     * Number of generations of objects to include
     * <p>
     *
     *
     */
    @JsonProperty("definitionalGenerations")
    public void setDefinitionalGenerations(Integer definitionalGenerations) {
        this.definitionalGenerations = definitionalGenerations;
    }

    /**
     * Include related objects
     * <p>
     *
     *
     */
    @JsonProperty("includedRelatedItems")
    public Boolean getIncludedRelatedItems() {
        return includedRelatedItems;
    }

    /**
     * Include related objects
     * <p>
     *
     *
     */
    @JsonProperty("includedRelatedItems")
    public void setIncludedRelatedItems(Boolean includedRelatedItems) {
        this.includedRelatedItems = includedRelatedItems;
    }

    /**
     * Number of generations of relationships to include
     * <p>
     *
     *
     */
    @JsonProperty("relatedGenerations")
    public Integer getRelatedGenerations() {
        return relatedGenerations;
    }

    /**
     * Number of generations of relationships to include
     * <p>
     *
     *
     */
    @JsonProperty("relatedGenerations")
    public void setRelatedGenerations(Integer relatedGenerations) {
        this.relatedGenerations = relatedGenerations;
    }

    /**
     * Include modification items
     * <p>
     *
     *
     */
    @JsonProperty("includeModifyingItems")
    public Boolean getIncludeModifyingItems() {
        return includeModifyingItems;
    }

    /**
     * Include modification items
     * <p>
     *
     *
     */
    @JsonProperty("includeModifyingItems")
    public void setIncludeModifyingItems(Boolean includeModifyingItems) {
        this.includeModifyingItems = includeModifyingItems;
    }

    /**
     * Include objects mentioned in amounts
     * <p>
     *
     *
     */
    @JsonProperty("includeMediatingItems")
    public Boolean getIncludeMediatingItems() {
        return includeMediatingItems;
    }

    /**
     * Include objects mentioned in amounts
     * <p>
     *
     *
     */
    @JsonProperty("includeMediatingItems")
    public void setIncludeMediatingItems(Boolean includeMediatingItems) {
        this.includeMediatingItems = includeMediatingItems;
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
