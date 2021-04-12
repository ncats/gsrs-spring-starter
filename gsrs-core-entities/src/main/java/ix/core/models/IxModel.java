package ix.core.models;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.nih.ncats.common.util.TimeUtil;
import ix.ginas.models.serialization.GsrsDateDeserializer;
import ix.ginas.models.serialization.GsrsDateSerializer;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@MappedSuperclass
@Getter
@Setter
public class IxModel extends BaseModel {
    @Id
    @GeneratedValue //Ebean added GeneratedValue by default we have to be explicit in hibernate
    public Long id;
    @Version
    public Long version;

    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    public Date created = TimeUtil.getCurrentDate();

    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    public Date modified;

    public boolean deprecated;

    public IxModel() {}


    @PrePersist
    private void markCreated(){
        Date date =TimeUtil.getCurrentDate();
        created = date;
        modified= date;
    }
    @PreUpdate
    private void markUpdated(){
        Date date =TimeUtil.getCurrentDate();
        modified= date;
    }

	@Override
	public String fetchGlobalId() {
		if(id!=null)return this.getClass().getName() + ":" + id.toString();
		return null;
	}
}
