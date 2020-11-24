package gsrs.startertests.controller;

import gsrs.AuditConfig;
import gsrs.controller.*;
import gsrs.junit.TimeTraveller;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.*;
import gsrs.validator.GsrsValidatorFactory;
import ix.core.search.text.TextIndexerEntityListener;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.GsrsMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@Disabled("can't get mockMVC to run yet")
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class, MyEntityRepository.class, MockMvc.class})
//@ContextConfiguration(classes = {GsrsSpringApplication.class})
@ActiveProfiles("test")
//@GsrsJpaTest(classes = { GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class, MyEntityRepository.class, MockMvc.class})
//@Import({MyEntity.class, MyController.class,
//         GsrsValidatorFactory.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import({ClearAuditorRule.class , ClearTextIndexerRule.class, AuditConfig.class, AutowireHelper.class,  TextIndexerEntityListener.class})
@Transactional
public class BasicControllerTest {


    @RegisterExtension
    TimeTraveller timeTraveller = new TimeTraveller(LocalDate.of(1955, 11, 05));





    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MyEntityRepository repo;

    @Test
    public void noDataLoadedShouldHave0Results() throws Exception {
        mockMvc.perform(get("/api/v1/myEntity"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.total", is(0)));
    }

    @Test
    public void loadSingleRecordNoTerms() throws Exception {
        MyEntity myEntity = new MyEntity();
        myEntity.setFoo("myFoo");
        MyEntity savedMyEntity = repo.saveAndFlush(myEntity);

        mockMvc.perform(get("/api/v1/myEntity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.count", is(1)))
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(myEntity.getUuid())))
                .andExpect(jsonPath("$.content[0].version", is(1)))
                .andExpect(jsonPath("$.content[0].foo", is(myEntity.getFoo())))
//                .andExpect(jsonPath("$.content[0].terms.length()", is(0)))
                .andExpect(jsonPath("$.content[0].modified", is(timeTraveller.getCurrentTimeMillis())))
                .andExpect(jsonPath("$.content[0].created", is(timeTraveller.getCurrentTimeMillis())))
        ;


        mockMvc.perform(get("/api/v1/myEntity/"+savedMyEntity.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedMyEntity.getUuid())))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.foo", is(myEntity.getFoo())))
//                .andExpect(jsonPath("$.terms.length()", is(0)))
                .andExpect(jsonPath("$.created", is(timeTraveller.getCurrentTimeMillis())))
                .andExpect(jsonPath("$.modified", is(timeTraveller.getCurrentTimeMillis())))
        ;

        mockMvc.perform(get("/api/v1/myEntity("+savedMyEntity.getUuid() + ")"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedMyEntity.getUuid())))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.foo", is(myEntity.getFoo())))
//                .andExpect(jsonPath("$.terms.length()", is(0)))
        ;
    }

    @Test
    public void updateSingleRecord() throws Exception {
        MyEntity vocab = new MyEntity();
        vocab.setFoo("myFoo");
        MyEntity savedVocab = repo.saveAndFlush(vocab);

        mockMvc.perform(get("/api/v1/myEntity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.count", is(1)))
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(vocab.getUuid())))
                .andExpect(jsonPath("$.content[0].version", is(1)))
                .andExpect(jsonPath("$.content[0].foo", is(vocab.getFoo())))
//                .andExpect(jsonPath("$.content[0].terms.length()", is(0)))
                .andExpect(jsonPath("$.content[0].created", is(timeTraveller.getCurrentTimeMillis())))
                .andExpect(jsonPath("$.content[0].modified", is(timeTraveller.getCurrentTimeMillis())))
        ;


        mockMvc.perform(get("/api/v1/myEntity/"+savedVocab.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedVocab.getUuid())))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(jsonPath("$.foo", is(vocab.getFoo())))
//                .andExpect(jsonPath("$.terms.length()", is(0)))
                .andExpect(jsonPath("$.created", is(timeTraveller.getCurrentTimeMillis())))
                .andExpect(jsonPath("$.modified", is(timeTraveller.getCurrentTimeMillis())))
        ;

        mockMvc.perform(get("/api/v1/vocabularies("+savedVocab.getUuid() + ")"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedVocab.getUuid())))
                .andExpect(jsonPath("$.version", is(1)))
//                .andExpect(jsonPath("$.deprecated", is(false)))
                .andExpect(jsonPath("$.foo", is(vocab.getFoo())))
                .andExpect(jsonPath("$.terms.length()", is(0)))
                .andExpect(jsonPath("$.created", is(timeTraveller.getCurrentTimeMillis())))
                .andExpect(jsonPath("$.modified", is(timeTraveller.getCurrentTimeMillis())))
        ;

        savedVocab.setFoo("foo2");

        timeTraveller.jumpAhead(1, TimeUnit.DAYS);

        repo.saveAndFlush(savedVocab);

        mockMvc.perform(get("/api/v1/vocabularies("+savedVocab.getUuid() + ")"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedVocab.getUuid())))
                .andExpect(jsonPath("$.version", is(2)))
//                .andExpect(jsonPath("$.deprecated", is(true)))
                .andExpect(jsonPath("$.foo", is("foo2")))
//                .andExpect(jsonPath("$.terms.length()", is(0)))
                .andExpect(jsonPath("$.created", is(timeTraveller.getWhereWeWereDate().get().getTime())))
                .andExpect(jsonPath("$.modified", is(timeTraveller.getCurrentTimeMillis())))
        ;

    }

//    @Test
//    public void loadSingleRecordWithTerms() throws Exception {
//        ControlledVocabulary vocab = new ControlledVocabulary();
//        vocab.setDomain("myDomain");
//        VocabularyTerm term1 = new VocabularyTerm();
//        term1.setValue("term1");
//        term1.setDisplay("term1Display");
//        vocab.addTerms( term1);
//
//        VocabularyTerm term2 = new VocabularyTerm();
//        term2.setValue("term2");
//        term2.setDisplay("term2Display");
//        vocab.addTerms( term2);
//
//        ControlledVocabulary savedVocab = repo.saveAndFlush(vocab);
//
//        mockMvc.perform(get("/api/v1/vocabularies"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.total", is(1)))
//                .andExpect(jsonPath("$.count", is(1)))
//                .andExpect(jsonPath("$.content.length()", is(1)))
//                .andExpect(jsonPath("$.content[0].id", is(1)))
//                .andExpect(jsonPath("$.content[0].version", is(1)))
//                .andExpect(jsonPath("$.content[0].domain", is(vocab.getDomain())))
//                .andExpect(jsonPath("$.content[0].terms.length()", is(2)))
//                .andExpect(jsonPath("$.content[0].terms[*].value", is(Arrays.asList("term1", "term2"))))
//        ;
//
//
//        mockMvc.perform(get("/api/v1/vocabularies/"+savedVocab.getId()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id", is(savedVocab.getId().intValue())))
//                .andExpect(jsonPath("$.version", is(1)))
//                .andExpect(jsonPath("$.domain", is(vocab.getDomain())))
//                .andExpect(jsonPath("$.terms.length()", is(2)))
//        ;
//
//        mockMvc.perform(get("/api/v1/vocabularies("+savedVocab.getId() + ")"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id", is(savedVocab.getId().intValue())))
//                .andExpect(jsonPath("$.version", is(1)))
//                .andExpect(jsonPath("$.domain", is(vocab.getDomain())))
//                .andExpect(jsonPath("$.terms.length()", is(2)))
        ;
//    }

//    @Test
//    public void updateSingleRecordWithNewTerms() throws Exception {
//        ControlledVocabulary vocab = new ControlledVocabulary();
//        vocab.setDomain("myDomain");
//        VocabularyTerm term1 = new VocabularyTerm();
//        term1.setValue("term1");
//        term1.setDisplay("term1Display");
//        vocab.addTerms( term1);
//
//        VocabularyTerm term2 = new VocabularyTerm();
//        term2.setValue("term2");
//        term2.setDisplay("term2Display");
//        vocab.addTerms( term2);
//
//        repo.saveAndFlush(vocab);
//
//        ControlledVocabulary savedVocab = repo.getOne(vocab.getId());
//        System.out.println("initial save version = " + savedVocab.getVersion());
//        mockMvc.perform(get("/api/v1/vocabularies"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.total", is(1)))
//                .andExpect(jsonPath("$.count", is(1)))
//                .andExpect(jsonPath("$.content.length()", is(1)))
//                .andExpect(jsonPath("$.content[0].id", is(1)))
//                .andExpect(jsonPath("$.content[0].version", is(1)))
//                .andExpect(jsonPath("$.content[0].domain", is(vocab.getDomain())))
//                .andExpect(jsonPath("$.content[0].terms.length()", is(2)))
//                .andExpect(jsonPath("$.content[0].terms[*].value", is(Arrays.asList("term1", "term2"))))
//        ;
//
//
//        mockMvc.perform(get("/api/v1/vocabularies/"+savedVocab.getId()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id", is(savedVocab.getId().intValue())))
//                .andExpect(jsonPath("$.version", is(1)))
//                .andExpect(jsonPath("$.domain", is(vocab.getDomain())))
//                .andExpect(jsonPath("$.terms.length()", is(2)))
//        ;
//
//        mockMvc.perform(get("/api/v1/vocabularies("+savedVocab.getId() + ")"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id", is(savedVocab.getId().intValue())))
//                .andExpect(jsonPath("$.version", is(1)))
//                .andExpect(jsonPath("$.domain", is(vocab.getDomain())))
//                .andExpect(jsonPath("$.terms.length()", is(2)))
//                .andExpect(jsonPath("$.created", is(timeTraveller.getCurrentTimeMillis())))
//                .andExpect(jsonPath("$.modified", is(timeTraveller.getCurrentTimeMillis())))
//                //TODO the json path with wildcards always returns lists so even if there are multiple objects with same value we need a list...
//                .andExpect(jsonPath("$.terms[*].created", is(Arrays.asList(timeTraveller.getCurrentTimeMillis(),timeTraveller.getCurrentTimeMillis()))))
//                .andExpect(jsonPath("$.terms[*].modified", is(Arrays.asList(timeTraveller.getCurrentTimeMillis(),timeTraveller.getCurrentTimeMillis()))))
//        ;
//
//        timeTraveller.jumpAhead(1, ChronoUnit.YEARS);
//        VocabularyTerm term3 = new VocabularyTerm();
//        term3.setValue("term3");
//        term3.setDisplay("term3Display");
//        savedVocab.addTerms( term3);
//        System.out.println("before saveAndFlush  savedVocab version = " + savedVocab.getVersion());
//        repo.saveAndFlush(savedVocab);
//
//        ControlledVocabulary version2 = repo.getOne(savedVocab.getId());
//        System.out.println("resaved vocab version = " + savedVocab.getVersion());
//
//        System.out.println("version = " + version2.getVersion());
//
//        mockMvc.perform(get("/api/v1/vocabularies("+savedVocab.getId() + ")"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id", is(savedVocab.getId().intValue())))
//                .andExpect(jsonPath("$.version", is(2)))
//                .andExpect(jsonPath("$.domain", is(vocab.getDomain())))
//                .andExpect(jsonPath("$.terms.length()", is(3)))
//                .andExpect(jsonPath("$.created", is(timeTraveller.getWhereWeWereDate().get().getTime())))
//                .andExpect(jsonPath("$.modified", is(timeTraveller.getCurrentTimeMillis())))
//                .andExpect(jsonPath("$.terms[:2].created", is(Arrays.asList(timeTraveller.getWhereWeWereDate().get().getTime(), timeTraveller.getWhereWeWereDate().get().getTime()))))
//                .andExpect(jsonPath("$.terms[:2].modified", is(Arrays.asList(timeTraveller.getWhereWeWereDate().get().getTime(), timeTraveller.getWhereWeWereDate().get().getTime()))))
//
//                .andExpect(jsonPath("$.terms[2].created", is(timeTraveller.getCurrentTimeMillis())))
//                .andExpect(jsonPath("$.terms[2].modified", is(timeTraveller.getCurrentTimeMillis())))
//        ;
//    }
}
