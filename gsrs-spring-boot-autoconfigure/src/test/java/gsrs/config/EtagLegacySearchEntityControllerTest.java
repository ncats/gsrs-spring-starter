package gsrs.config;

import gsrs.controller.EtagLegacySearchEntityController;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.service.GsrsEntityService;
import ix.core.search.bulk.ResultListRecordGenerator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class EtagLegacySearchEntityControllerTest {

    @Test
    public void testIsInteger() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String methodName = "isInteger";
        EtagLegacySearchEntityController controller = new EtagLegacySearchEntityController() {
            @Override
            protected Stream filterStream(Stream stream, boolean publicOnly, Map parameters) {
                return null;
            }

            @Override
            protected LegacyGsrsSearchService getlegacyGsrsSearchService() {
                return null;
            }

            @Override
            protected GsrsEntityService getEntityService() {
                return null;
            } 

        };
        Method[] listOfMethods = controller.getClass().getDeclaredMethods();
        for( Method method : listOfMethods){
            System.out.println(method.getName());
        }

        /*Method isIntegerMethod=controller.getClass().getDeclaredMethod(methodName, String.class);
        isIntegerMethod.setAccessible(true);*/
        String allDigits ="12346";
        Boolean result = controller.isInteger(allDigits);
        Assertions.assertTrue(result);

        List<String> falseExpected = Arrays.asList("9.2", "hello", " ", "1.2E3");
        falseExpected.forEach(s->Assertions.assertFalse(controller.isInteger(s)));
    }
}
