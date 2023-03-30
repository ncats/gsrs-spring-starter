package gsrs.dataexchange.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.model.GeneralPurposeJob;
import ix.core.models.Indexable;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "ix_batch_processingjob")
@Slf4j
public class ImportProcessingJob implements GeneralPurposeJob {

    @Id
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)private UUID id;
    
    @Lob
    private String data;

    @Indexable(facet = true)
    private Date startDate;

    @Indexable(facet = true)
    private String category ="Import Processing";

    @Indexable
    private String jobStatus;

    @Lob
    private String results ="";


    @Indexable
    private String statusMessage="";

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id=id;
    }

    @Override
    public String getJobData() {
        return data;
    }

    @Override
    public void setJobData(String data) {
        this.data=data;
    }


    @Override
    public String getCategory() {
        return this.category;
    }

    @Override
    public void setCategory(String category) {
        this.category=category;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public void setStartDate(Date date) {
        this.startDate=date;
    }

    @Override
    public String getJobStatus() {
        return this.jobStatus;
    }

    @Override
    public void setJobStatus(String status) {
        this.jobStatus=status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public ArrayNode getResults(){
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        if(results==null || results.length()==0) {
            return arrayNode;
        }
        try {
            arrayNode = (ArrayNode) mapper.readTree(this.results);
        } catch (JsonProcessingException e) {
            log.error("Error converting to ArrayNode", e);
            throw new RuntimeException(e);
        }
        return arrayNode;
    }

    public void setResults(ArrayNode results){
        log.trace("starting in setResults");
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.results =mapper.writeValueAsString(results);
            log.trace("set results to {}", mapper.writeValueAsString(results));
        } catch (JsonProcessingException e) {
            log.error("Error serializing results", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("id: ");
        builder.append(id);
        builder.append("; category: ");
        builder.append(category);
        builder.append(": start: ");
        builder.append(this.startDate);
        return builder.toString();
    }

    /*private ArrayNode getCleanResults() {
        ArrayNode pre = getResults();
        if(pre== null || pre.size()==0) {
            return pre;
        }
        ArrayNode returnNode = JsonNodeFactory.instance.arrayNode();
        for(int i=0; i<pre.size(); i++) {
            JsonNode currentNode = pre.get(i);
            ObjectNode nodeCopy = JsonNodeFactory.instance.objectNode();
            if(currentNode instanceof ObjectNode){
                ObjectNode objectNode=(ObjectNode) currentNode;
                Iterator<String> it = objectNode.fieldNames();
                while(it.hasNext()){
                    String fieldName = it.next();
                    node
                }
                for(int p=0; p< objectNode.fieldNames())
            }
            for(int l=0; l< currentNode.size();l++){
                if(currentNode.g`)
            }
            if( currentNode.hasNonNull("status")) {
                if(currentNode.get("status").asInt()==200){

                }
            }
        }
    }*/
    public ObjectNode toNode(){
        ObjectNode node =JsonNodeFactory.instance.objectNode();
        node.put("id", this.id.toString());
        node.put("startDate", this.startDate.getTime());
        node.put("category", this.getCategory());
        node.put("jobStatus", this.getJobStatus());
        node.put("statusMessage", this.getStatusMessage());
        node.set("results", this.getResults());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jobDataNode = null;
        try {
            jobDataNode = mapper.readTree(this.getJobData());
            node.set("jobData", jobDataNode);
        } catch (JsonProcessingException e) {
            log.error("Error converting jobdata: ", e);
            //throw new RuntimeException(e);
        }
        return node;
    }
}
