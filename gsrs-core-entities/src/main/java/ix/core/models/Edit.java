package ix.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.flipkart.zjsonpatch.JsonDiff;
import gov.nih.ncats.common.util.TimeUtil;
import ix.core.EntityMapperOptions;
import ix.core.ResourceReference;
import ix.core.util.EntityUtils.EntityWrapper;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

@Entity
@Table(name="ix_core_edit")
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
    @GeneratedValue
    public UUID id; // internal random id

    //don't use @CreateDate annotation here just set it on creation time and mark it final
    public final Long created = TimeUtil.getCurrentTimeMillis();

    public String refid; // edited entity
    public String kind;

    // this edit belongs to a chain of edit history
    @Column(length=64)
    public String batch;

    //TODO katzelda Dec 2020 turn off createdBy for now stackoverflow with userdetails
//    @CreatedBy
//    @ManyToOne(cascade= CascadeType.PERSIST)
//    public Principal editor;

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
    @JsonView(BeanViews.Full.class)
    public String oldValue; // value as Json

    @Basic(fetch= FetchType.LAZY)
    @Lob
    @JsonDeserialize(as= JsonNode.class)
    @Indexable(indexed=false)
    @JsonView(BeanViews.Full.class)
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
    //FIXME : katzelda Dec 2020 move these links to controller
    /*
    @JsonProperty("oldValue")
    public ResourceReference<JsonNode> getOldValueReference() {
    	//we will always have an old value
    	String uri = Global.getNamespace()+"/edits("+id+")/$oldValue";
        return ResourceReference.ofSerializedJson(uri, new Supplier<String>() {
            @Override
            public String get() {
                return oldValue;
            }
        });
    }
    @JsonProperty("newValue")
    public ResourceReference<JsonNode> getNewValueReference() {
        //we will always have new value

    	String uri = Global.getNamespace()+"/edits("+id+")/$newValue";
    	return ResourceReference.ofSerializedJson(uri, new Supplier<String>() {
            @Override
            public String get() {
                return newValue;
            }
        });
    }
    

    @JsonProperty("diff")
    public ResourceReference<JsonNode> getDiffLink () {
//    	if(this.newValue==null || this.oldValue==null)return null;
    	String uri = Global.getNamespace()+"/edits("+id+")/$diff";
    	return ResourceReference.of(uri, new Supplier<JsonNode>(){
			@Override
			public JsonNode get() {
				return getDiff();
			}
    	});
    }
*/
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
    @EntityMapperOptions(includeAsCallable=true)
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
