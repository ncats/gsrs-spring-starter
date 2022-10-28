package ix.ginas.exporters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.core.models.Text;
import ix.core.util.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class SpecificExporterSettings {

    private String configurationId;
    private String configurationKey;
    private String exporterKey;
    private JsonNode scrubberSettings;
    private JsonNode exporterSettings;
    private JsonNode expanderSettings;
    private String entityClass;
    private String owner;

    public static String getEntityKeyFromClass(String className){
        return "export config " + className;
    }

    public Text asText() {
        Text txt = new Text(getEntityKeyFromClass(entityClass), EntityUtils.EntityWrapper.of(this).toInternalJson());
        return txt;
    }

    public static SpecificExporterSettings fromText(Text text) throws JsonProcessingException {
        log.trace("starting in fromText");
        ObjectMapper mapper = new ObjectMapper();
        SpecificExporterSettings conf = mapper.readValue(text.getValue(), SpecificExporterSettings.class);
        if(conf == null) {
            log.error("Error creating config from input {}", text.getValue());
            return null;
        }
        if(text.id!=null){
            conf.setConfigurationId(text.id.toString());
        }
        return conf;
    }

}
