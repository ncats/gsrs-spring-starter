package gsrs;


import gsrs.repository.ControlledVocabularyRepository;
import ix.ginas.models.v1.ControlledVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;

@Profile("!test")
@Component
@Order(1)
public class LoadCvOnStartup implements ApplicationRunner {



    @Autowired
    private ControlledVocabularyRepository repository;


    @Autowired
    private JsonMapper jsonMapper;

    @Value("${gsrs.cv.jsonFile}")
    private String jsonPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //only run if not populated
        if(repository.count() >0){
            return;
        }
//        System.out.println("RUNNING");
//        System.out.println("reading property file at path '"+jsonPath + "'");
        JsonNode json;
        try(InputStream in = new ClassPathResource(jsonPath).getInputStream()){
            json = jsonMapper.readValue(in, JsonNode.class);

        }

//        System.out.println(json);

        List<ControlledVocabulary> cv = CvUtils.adaptList(json, jsonMapper, true);
        cv.forEach(v-> v.setVersion(null));
        repository.saveAll(cv);
        repository.flush();

    }
}
