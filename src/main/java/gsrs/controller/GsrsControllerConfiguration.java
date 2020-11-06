package gsrs.controller;

import gov.nih.ncats.common.util.CachedSupplier;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for GSRS controllers mostly used right now
 * to support backwards compatibility for the unusual way the legacy GSRS application
 * supported error handling and error codes.
 * <p>
 * Some Legacy GSRS API consumers could not handle all HTTP status codes
 * and so we added support for additional url parameters and configurations
 * to override the standard HTTP error codes that could be set by the API consumer.
 * </p>
 * For example, setting a url parameter to force the status code to be 500 even if
 * the standard REST API should return a 404.
 * <p>
 * Configuraing the url parameter name is set in the application properties
 * file as {@code gsrs.api.errorCodeParameter}.
 * Another configuration parameter to always force the api to override the usual
 * error code is {@code gsrs.api.forceErrorCodeValue} so clients don't
 * have to specify the url parameter in every request they make (or their software
 * could not be modified to add the url parameter).
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix="gsrs.api")
@Data
public class GsrsControllerConfiguration {

    private String errorCodeParameter;

    private Integer forceErrorCodeValue;

    private static boolean isValidErrorCode(int askedForStatus) {
        return askedForStatus >=400 && askedForStatus< 600;
    }

    private int overrideErrorCodeIfNeeded(int defaultStatus, WebRequest request){
        String value =request.getParameter(errorCodeParameter);
        return overrideErrorCodeIfNeeded(defaultStatus, Collections.singletonMap(errorCodeParameter, value));
    }
    private int overrideErrorCodeIfNeeded(int defaultStatus, Map<String, String> queryParameters){
        //GSRS-1598 force not found error to sometimes be a 500 instead of 404
        //if requests tells us
        try {
            String specifiedResponse = queryParameters.get(errorCodeParameter);
            if(specifiedResponse !=null){
                int askedForStatus = Integer.parseInt(specifiedResponse);
                //status must be a 4xx or 5xx so people can't make it 200
                if(isValidErrorCode(askedForStatus)){
                    return askedForStatus;
                }
            }

        }catch(Exception e){
            //no request?
        }

        if(forceErrorCodeValue!=null){
            int asInt = forceErrorCodeValue.intValue();
            if(isValidErrorCode(asInt)){
                return asInt;
            }
        }
        //use default
        return defaultStatus;
    }

    public ResponseEntity<Object> handleNotFound(Map<String, String> queryParameters){
        int status = overrideErrorCodeIfNeeded(404, queryParameters);
        return new ResponseEntity<>( createStatusJson("not found", status), HttpStatus.valueOf(status));

    }
    public ErrorInfo createErrorStatusBody(Throwable t, int status,  WebRequest request){
        int statusToUse = overrideErrorCodeIfNeeded(status, request);
        Object body = createStatusJson("not found", statusToUse);
        return ErrorInfo.builder()
                            .body(body)
                            .status(HttpStatus.valueOf(statusToUse))
                            .build();


    }

    public ResponseEntity<Object> handleBadRequest(Map<String, String> queryParameters) {
        int status = overrideErrorCodeIfNeeded(400, queryParameters);
        return new ResponseEntity<>( createStatusJson("bad request", status), HttpStatus.valueOf(status));

    }
    public ResponseEntity<Object> handleError(Throwable t, Map<String, String> queryParameters) {
        int status = overrideErrorCodeIfNeeded(500, queryParameters);
        return new ResponseEntity<>( getError(t, status), HttpStatus.valueOf(status));

    }

    @Data
    @Builder
    public static class ErrorInfo{
        private Object body;
        private HttpStatus status;
    }

    private static Object getError(Throwable t, int status){

        Map m=new HashMap();
        if(t instanceof InvocationTargetException){
            m.put("message", ((InvocationTargetException)t).getTargetException().getMessage());
        }else{
            m.put("message", t.getMessage());
        }
        m.put("status", status);
        return m;
    }

    private static Object createStatusJson(String message, int status){
        Map m=new HashMap();
        m.put("message", message);

        m.put("status", status);
        return m;
    }
}
