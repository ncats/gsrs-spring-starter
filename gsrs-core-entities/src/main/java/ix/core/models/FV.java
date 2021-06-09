package ix.core.models;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class FV {
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Facet container;
    private String label;
    private Integer count;



    public FV(Facet container, String label, Integer count) {
        this.container=container;
        this.label = label;
        this.count = count;
    }

//TODO katzelda October 2020: ignore it since it's not in json
//		@JsonIgnore
//		public String getToggleUrl(){
//			return container.sr.getFacetToggleURI(container.name, this.label);
//		}
}
