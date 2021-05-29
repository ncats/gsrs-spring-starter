package gsrs.cv.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import java.util.Date;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GsrsVocabularyTermDTO {
    private Long id;
    private long version =1;
    private Date created;
    private Date modified;
    private boolean deprecated;

    private String value;
    private String display;
    private String description;

    private String regex;

    private String origin;
    private boolean hidden;
    private boolean selected;


}
