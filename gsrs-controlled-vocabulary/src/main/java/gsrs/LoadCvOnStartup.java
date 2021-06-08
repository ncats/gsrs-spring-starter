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
import org.springframework.stereotype.Component;

import javax.persistence.EntityManagerFactory;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Profile("!test")
@Component
public class LoadCvOnStartup implements ApplicationRunner {



    @Autowired
    private ControlledVocabularyRepository repository;

    @Autowired
    private UserProfileRepository userProfileRepository;

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
        try(InputStream in = getClass().getResourceAsStream(jsonPath)){
            json = objectMapper.readValue(in, JsonNode.class);

        }

//        System.out.println(json);

        List<ControlledVocabulary> cv = CvUtils.adaptList(json, objectMapper, true);
        cv.forEach(v-> v.setVersion(null));
        repository.saveAll(cv);
        repository.flush();


        UserProfile up = new UserProfile();
        up.user = new Principal("admin", "admin@example.com");
        up.setPassword("admin");
        up.active=true;
        up.deprecated=false;
        up.setRoles(Arrays.asList(Role.values()));

        userProfileRepository.saveAndFlush(up);

        UserProfile up2 = new UserProfile();
        up2.user = new Principal("user1", "user1@example.com");
        up2.setPassword("user1");
        up2.active=true;
        up2.deprecated=false;
        up2.setRoles(Arrays.asList(Role.Query));

        userProfileRepository.saveAndFlush(up2);
    }
}
