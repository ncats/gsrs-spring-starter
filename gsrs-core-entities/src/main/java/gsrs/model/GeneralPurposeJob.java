package gsrs.model;

import java.util.Date;
import java.util.UUID;

public interface GeneralPurposeJob {

    UUID getId();
    void setId(UUID id);

    String getJobData();
    void setJobData(String data);

    String getCategory();
    void setCategory(String category);

    Date getStartDate();
    void setStartDate(Date date);

    String getJobStatus();
    void setJobStatus(String status);

}
