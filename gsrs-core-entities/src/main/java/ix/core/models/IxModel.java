package ix.core.models;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
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
     //Ebean added GeneratedValue by default we have to be explicit in hibernate
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "LONG_SEQ_ID")
    @Column(unique = true)
    public Long id;
    @Version
    public Long version;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JsonView(BeanViews.Full.class)
    public Namespace namespace; // namespace of dictionary, ontology, etc.

    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    public Date created = TimeUtil.getCurrentDate();

    @JsonSerialize(using = GsrsDateSerializer.class)
    @JsonDeserialize(using = GsrsDateDeserializer.class)
    public Date modified;

    public boolean deprecated;

    public IxModel() {}

    /**
     * override this method to make any changes during a
     * {@code @PrePersist} call.
     * Please make sure to call {@code super.prePersist()}.
     */
    protected void prePersist(){

    }
    /**
     * override this method to make any changes during a
     * {@code @PreUpdate} call.
     * Please make sure to call {@code super.preUpdate()}.
     */
    protected void preUpdate(){

    }

    @PrePersist
    private void markCreated(){
        Date date =TimeUtil.getCurrentDate();
        created = date;
        modified= date;
            prePersist();
    }
    @PreUpdate
    private void markUpdated(){
        Date date =TimeUtil.getCurrentDate();
        modified= date;
        preUpdate();
    }

	@Override
	public String fetchGlobalId() {
		if(id!=null)return this.getClass().getName() + ":" + id.toString();
		return null;
	}
}
