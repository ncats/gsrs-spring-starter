package ix.ginas.exporters;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.databind.JsonNode;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.model.GsrsApiAction;
import ix.core.FieldResourceReference;
import ix.core.ResourceReference;
import ix.core.models.Principal;
import ix.ginas.models.utils.JSONEntity;


import javax.persistence.Id;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by katzelda on 4/18/17.
 */
@JSONEntity(name = "metadata")
public class ExportMetaData implements Comparable<ExportMetaData>{
    private Consumer<Long> totalConsumer=(l)->{};
    @Id
    public String id = UUID.randomUUID().toString();
    
    private long numRecords;
    public Long totalRecotds=null;
    
    
    public String collectionId;
    public String originalQuery;
    public String username;
    public boolean publicOnly;
    public String extension;
    
    
    public String filename;
    
    @JsonIgnore
    public String displayfilename;
    
    
    public boolean cancelled=false;
    
    
    
    public String sha1;
    public long size;


    public Long getStarted() {
        return started;
    }

    public String getFilename() {
    	if(this.filename==null){
    		return this.id + "." +this.extension;
    	}
    	return this.filename;
    }
    
   

    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    
    public void setDisplayFilename(String filename) {
        this.displayfilename = filename;
    }
    
    public String getDisplayFilename() {
        if(this.displayfilename!=null){
            return this.displayfilename;
        }else{
            return this.getFilename();
        }
    }



    public Long started = TimeUtil.getCurrentTimeMillis(),finished;

    public ExportMetaData(){}

    public ExportMetaData(String collectionId, String originalQuery, String username, boolean publicOnly, String extension) {
        this.collectionId = collectionId;
        this.originalQuery = originalQuery;
        this.username = username;
        this.publicOnly = publicOnly;
        this.extension = extension;
    }
    
    
    public boolean isComplete(){
        return this.finished!=null;
    }
    
    //TODO: move status info here for better details
    public String getStatus(){
        if(this.isComplete() && this.sha1!=null && !cancelled){
            return "COMPLETE";
        }else if(this.cancelled==true){
            return "CANCELLED";
        }else if(this.isComplete() && this.sha1==null){
            return "ERROR";
        }else{
            return "RUNNING";
        }
    }
    
    /**
     * This key is meant to be the same if the generating query and output format is
     * the same.  
     * @return
     */
    public String getKey(){
    	return getKeyFor(this.collectionId,this.extension, this.publicOnly);
    }

    @JsonIgnore
    @GsrsApiAction("downloadUrl")
    public ResourceReference<String> downloadUrl () {
        if(isComplete()) {
            return FieldResourceReference.forField("download", () -> "");
        }
        return null;

    }
    @JsonIgnore
    @GsrsApiAction(value = "removeUrl", type = GsrsApiAction.Type.DELETE)
    public ResourceReference<String> removeUrl () {
        if(isComplete()) {
            return FieldResourceReference.forField("", () -> "");
        }
        return null;

    }
    @JsonIgnore
    @GsrsApiAction(value = "cancelUrl")
    public ResourceReference<String> cancelUrl () {
        if(!isComplete()) {
            return FieldResourceReference.forField("@cancel", () -> "");
        }
        return null;

    }

    public String getCollectionId() {
        return collectionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExportMetaData that = (ExportMetaData) o;

        if (getNumRecords() != that.getNumRecords()) return false;
        if (publicOnly != that.publicOnly) return false;
        if (!collectionId.equals(that.collectionId)) return false;
        if (originalQuery != null ? !originalQuery.equals(that.originalQuery) : that.originalQuery != null)
            return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        if (extension != null ? !extension.equals(that.extension) : that.extension != null) return false;
        if (started != null ? !started.equals(that.started) : that.started != null) return false;
        return finished != null ? finished.equals(that.finished) : that.finished == null;

    }
    
    

    @Override
    public int hashCode() {
        long result = getNumRecords();
        result = 31 * result + collectionId.hashCode();
        result = 31 * result + (originalQuery != null ? originalQuery.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (publicOnly ? 1 : 0);
        result = 31 * result + (extension != null ? extension.hashCode() : 0);
        result = 31 * result + (started != null ? started.hashCode() : 0);
        result = 31 * result + (finished != null ? finished.hashCode() : 0);
        return (int) result;
    }
    
    
    private static String getKeyFor(String collectionId,String extension, boolean publicOnly){
        // return new StringBuilder().append(collectionId).append('/').append(extension).append('/').append(publicOnly).toString();
         StringBuilder builder = new StringBuilder(collectionId);

         if(!publicOnly){
             builder.append("_private");
         }
         builder.append('.').append(extension);

         return builder.toString();
     }
    
    public void cancel(){
        this.cancelled=true;
    }



    public long getNumRecords() {
        return numRecords;
    }

    public void addRecord() {
        this.numRecords++;
        totalConsumer.accept(this.numRecords);
    }
    
    public ExportMetaData onTotalChanged(Consumer<Long> total){
        this.totalConsumer=total;
        return this;
    }

    @Override
    public int compareTo(ExportMetaData o) {
        int v = Long.compare(finished, o.finished);
        if( v!=0){
            v= Long.compare(started, o.started);
        }
        if(v !=0){
            v = getFilename().compareTo(o.getFilename());
        }
        return v;
    }
}
