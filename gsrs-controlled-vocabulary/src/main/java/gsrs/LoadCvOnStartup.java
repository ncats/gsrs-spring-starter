package gsrs;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.repository.ControlledVocabularyRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import ix.ginas.models.v1.ControlledVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManagerFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Profile("!test")
@Component
@Order(1)
public class LoadCvOnStartup implements ApplicationRunner {



    @Autowired
    private ControlledVocabularyRepository repository;


    @Autowired
    private ObjectMapper objectMapper;

    @Value("${gsrs.cv.jsonFile}")
    private String jsonPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //only run if not populated
        if(repository.count() >0){
            return;
        }
        System.out.println("RUNNING");
        System.out.println("reading property file at path '"+jsonPath + "'");
        JsonNode json;
        try(InputStream in = new ClassPathResource(jsonPath).getInputStream()){
            json = objectMapper.readValue(in, JsonNode.class);

        }

//        System.out.println(json);

        List<ControlledVocabulary> cv = CvUtils.adaptList(json, objectMapper, true);
        cv.forEach(v-> v.setVersion(null));
        repository.saveAll(cv);
        repository.flush();

    }
}
