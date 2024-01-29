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

import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "ix_batch_processingjob")
@Slf4j
public class ImportProcessingJob implements GeneralPurposeJob {

    @Id
    @Type(type = "uuid-char" )
    @Column(length =40, updatable = false, unique = true)
    private UUID id;
    
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

    @Indexable(facet = true)
    private Date finishDate;

    @Indexable
    private int totalRecords=0;

    @Indexable
    private int completedRecordCount=0;

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

    public Date getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
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

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getCompletedRecordCount() {
        return completedRecordCount;
    }

    public void setCompletedRecordCount(int completedRecordCount) {
        this.completedRecordCount = completedRecordCount;
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

    public ObjectNode toNode(boolean includeJobData){
        ObjectNode node =JsonNodeFactory.instance.objectNode();
        node.put("id", this.id.toString());
        node.put("startDate", this.startDate.getTime());
        if(this.finishDate!=null) {
            node.put("finishDate", this.finishDate.getTime());
        }
        node.put("category", this.getCategory());
        node.put("jobStatus", this.getJobStatus());
        node.put("statusMessage", this.getStatusMessage());
        node.set("results", this.getResults());
        node.put("totalRecords", this.totalRecords);
        node.put("completedRecordCount", this.completedRecordCount);

        ObjectMapper mapper = new ObjectMapper();
        if(includeJobData) {
            JsonNode jobDataNode = null;
            try {
                jobDataNode = mapper.readTree(this.getJobData());
                node.set("jobInputData", jobDataNode);
            } catch (JsonProcessingException e) {
                log.error("Error converting jobdata: ", e);
                log.trace(this.getJobData());
                //throw new RuntimeException(e);
            }
        }
        return node;
    }

    public ObjectNode toNode(){
        return toNode(false);
    }
}
