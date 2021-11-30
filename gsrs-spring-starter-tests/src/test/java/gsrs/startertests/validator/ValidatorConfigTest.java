package gsrs.startertests.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
public class ValidatorConfigTest{

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void programmaticallyCreateConfigNoParams() throws ClassNotFoundException {
        ValidatorConfig conf = new DefaultValidatorConfig();
        conf.setValidatorClass(MyValidator.class);
        MyValidator validator = (MyValidator) conf.newValidatorPlugin(mapper, this.getClass().getClassLoader());

        assertNull(validator.getFoo());
    }

    @Test
    public void programmaticallyCreateConfigUsingParametersField() throws ClassNotFoundException {
        ValidatorConfig conf = new DefaultValidatorConfig();
        conf.setValidatorClass(MyValidator.class);
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        conf.setParameters(map);
        MyValidator validator = (MyValidator) conf.newValidatorPlugin(mapper, this.getClass().getClassLoader());

        assertEquals("bar", validator.getFoo());

    }

    @Test
    public void programmaticallyCreateConfigUsingUnknownField() throws ClassNotFoundException {
        DefaultValidatorConfig conf = new DefaultValidatorConfig();
        conf.setValidatorClass(MyValidator.class);
        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        conf.setUnknownParameters(map);
        MyValidator validator = (MyValidator) conf.newValidatorPlugin(mapper, this.getClass().getClassLoader());

        assertEquals("bar", validator.getFoo());

    }

    @Test
    public void programmaticallyCreateConfigUsingParametersTrumpsUnknownField() throws ClassNotFoundException {
        DefaultValidatorConfig conf = new DefaultValidatorConfig();
        conf.setValidatorClass(MyValidator.class);
        Map<String, Object> unknownMap = new HashMap<>();
        unknownMap.put("foo", "ignored");
        conf.setUnknownParameters(unknownMap);

        Map<String, Object> map = new HashMap<>();
        map.put("foo", "bar");
        conf.setParameters(map);

        MyValidator validator = (MyValidator) conf.newValidatorPlugin(mapper, this.getClass().getClassLoader());

        assertEquals("bar", validator.getFoo());

    }



}
