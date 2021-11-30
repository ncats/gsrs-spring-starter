package gsrs.startertests.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import gsrs.validator.ValidatorConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Parsing from JSON requires a whole spring boot app
 * to get the application context, autowiring and package scanning
 * to find all the potential subclasses we need for
 * creating validator config subclasses.
 *
 * So I split the tests into 2 classes one that doesn't need
 * a Spring Boot app, and one that does which run 10x slower.
 */
@GsrsJpaTest(dirtyMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = GsrsSpringApplication.class)
public class ValidatorJsonConfigTest extends AbstractGsrsJpaEntityJunit5Test {


    private ObjectMapper mapper = new ObjectMapper();


    @Test
    public void jsonCreateConfigUsingParameters() throws ClassNotFoundException, JsonProcessingException {

        String json = "{\n" +
                "    \"validatorClass\" : \""+ MyValidator.class.getName() +"\",\n" +
                "    \n\"parameters\" : { \"foo\" : \"bar\"}\n" +
                "}";


        ValidatorConfig conf =  mapper.treeToValue(mapper.readTree(json), ValidatorConfig.class);

        MyValidator validator = (MyValidator) conf.newValidatorPlugin(mapper, this.getClass().getClassLoader());

        assertEquals("bar", validator.getFoo());

    }
    @Test
    public void jsonCreateConfigUsingUnknownField() throws ClassNotFoundException, JsonProcessingException {

        String json = "{\n" +
                "    \"validatorClass\" : \""+ MyValidator.class.getName() +"\",\n" +
                "\"foo\" : \"baz\"\n" +
                "}";


        ValidatorConfig conf =  mapper.treeToValue(mapper.readTree(json), ValidatorConfig.class);

        MyValidator validator = (MyValidator) conf.newValidatorPlugin(mapper, this.getClass().getClassLoader());

        assertEquals("baz", validator.getFoo());

    }
    @Test
    public void jsonCreateConfigUsingParametersTrumpsUnknownField() throws ClassNotFoundException, JsonProcessingException {

        String json = "{\n" +
                "    \"validatorClass\" : \""+ MyValidator.class.getName() +"\",\n" +
                "    \n\"parameters\" : { \"foo\" : \"bar\"},\n" +
                "\"foo\" : \"ignored\"\n" +
                "}";


        ValidatorConfig conf =  mapper.treeToValue(mapper.readTree(json), ValidatorConfig.class);

        MyValidator validator = (MyValidator) conf.newValidatorPlugin(mapper, this.getClass().getClassLoader());

        assertEquals("bar", validator.getFoo());

    }
}
