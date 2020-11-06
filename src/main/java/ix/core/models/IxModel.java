package ix.core.models;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.GsrsEntityProcessorListener;
import ix.core.search.text.TextIndexerEntityListener;
import ix.ginas.models.serialization.GsrsDateDeserializer;
import ix.ginas.models.serialization.GsrsDateSerializer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import javax.persistence.*;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@MappedSuperclass
@EntityListeners(value= {AuditingEntityListener.class, GsrsEntityProcessorListener.class, TextIndexerEntityListener.class})
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
    @CreatedDate
    public Date created = TimeUtil.getCurrentDate();

    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    @LastModifiedDate
    public Date modified;

    public boolean deprecated;

    public IxModel() {}



	@Override
	public String fetchGlobalId() {
		if(id!=null)return this.getClass().getName() + ":" + id.toString();
		return null;
	}
}
