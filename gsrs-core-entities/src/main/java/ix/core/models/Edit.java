package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.flipkart.zjsonpatch.JsonDiff;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.model.GsrsApiAction;
import ix.core.EntityMapperOptions;
import ix.core.FieldResourceReference;
import ix.core.History;
import ix.core.ResourceReference;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.ginas.models.serialization.PrincipalDeserializer;
import ix.ginas.models.serialization.PrincipalSerializer;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;

import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name="ix_core_edit", indexes = {@Index(name = "refid_core_edit_index", columnList = "refid"),
		@Index(name = "kind_core_edit_index", columnList = "kind")})
@History(store = false)
@Indexable(indexed = false)
public class Edit extends BaseModel {


    public static <T> Edit create(T before, T after){
        EntityWrapper<?> ew = EntityWrapper.of(after);
        Edit edit = new Edit(ew.getEntityClass(), ew.getKey().getIdString());
        EntityWrapper<?> ewold = EntityWrapper.of(before);

        edit.oldValue = ewold.toFullJson();
        edit.version = ewold.getVersion().orElse(null);
        edit.comments = ew.getChangeReason().orElse(null);
        edit.kind = ew.getKind();
        edit.newValue = ew.toFullJson();

        return edit;
    }
    
    
    @JsonIgnore
    @Id
    @GenericGenerator(name = "NullUUIDGenerator", strategy = "ix.ginas.models.generators.NullUUIDGenerator")
    @GeneratedValue(generator = "NullUUIDGenerator")
    //maintain backwards compatibility with old GSRS store it as varchar(40) by default hibernate will store uuids as binary
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false)    
    
//    @JsonIgnore
//    @Id
//    @GeneratedValue
    public UUID id; // internal random id
    

    //don't use @CreateDate annotation here just set it on creation time and mark it final
    public final Long created = TimeUtil.getCurrentTimeMillis();

    public String refid; // edited entity
    public String kind;

    // this edit belongs to a chain of edit history
    @Column(length=64)
    public String batch;


    @CreatedBy
    @ManyToOne()
    @JsonDeserialize(using = PrincipalDeserializer.class)
    @JsonSerialize(using = PrincipalSerializer.class)
    public Principal editor;

    @Column(length=1024)
    public String path;

    @Lob
    @Basic(fetch= FetchType.EAGER)
    public String comments;
    
    public String version=null;

    @Basic(fetch= FetchType.LAZY)
    @Lob
    @JsonDeserialize(as= JsonNode.class)
    @Indexable(indexed=false)
    @JsonIgnore
//    @JsonView(BeanViews.Full.class)
//    @EntityMapperOptions(linkoutRawInEveryView = true)
    public String oldValue; // value as Json

    @Basic(fetch= FetchType.LAZY)
    @Lob
    @JsonDeserialize(as= JsonNode.class)
    @Indexable(indexed=false)
    @JsonIgnore
//    @JsonView(BeanViews.Full.class)
//    @EntityMapperOptions(linkoutRawInEveryView = true)
    public String newValue; // value as Json

    public Edit() {}


    public Edit(Class<?> type, Object refid) {
        this.kind = type.getName();
        this.refid = refid.toString();
    }
    
    public Edit(EntityWrapper<?> entity) {
        this.kind = entity.getKind();
        this.refid = entity.getKey().getIdString();
    }
    
    
//    public String getEditor(){
//    	if(editor==null)return null;
//    	return editor.username;
//    }

    @JsonIgnore
    @GsrsApiAction(value= "oldValue", serializeUrlOnly = true, isRaw = true)
    public FieldResourceReference<JsonNode> getOldValueReference() {
        if(oldValue ==null){
            return null;
        }
        return FieldResourceReference.forRawFieldAsJson("oldValue", oldValue);
    }
    @JsonIgnore
    @GsrsApiAction(value="newValue", serializeUrlOnly = true,  isRaw = true)
    public ResourceReference<JsonNode> getNewValueReference() {
        //we will always have new value

        return FieldResourceReference.forRawFieldAsJson("newValue", newValue);
    }
    

    @JsonIgnore
    @GsrsApiAction(value = "diff", serializeUrlOnly = true)
    public ResourceReference<JsonNode> getDiffLink () {
        return FieldResourceReference.forRawField("diff", this::getDiff);

    }

    @Override
    public String fetchGlobalId(){
    	if(this.id==null)return null;
    	return this.id.toString();
    }
    
    @JsonIgnore
    public Date getCreatedDate(){
    	return new Date(this.created);
    }

    @JsonIgnore
    public JsonNode getDiff(){
    	try{
	    	ObjectMapper om = new ObjectMapper();
	    	JsonNode js1=om.readTree(oldValue);
	    	JsonNode js2=om.readTree(newValue);
	    	return JsonDiff.asJson(js1, js2);
    	}catch(Exception e){
    		return null;
    	}
    }
}
