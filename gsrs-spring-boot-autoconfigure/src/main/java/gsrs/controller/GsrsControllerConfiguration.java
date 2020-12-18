package gsrs.controller;

import gov.nih.ncats.common.util.CachedSupplier;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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
    private int overrideErrorCodeIfNeededServlet(int defaultStatus, Map<String, String[]> queryParameters){
        return overrideErrorCodeIfNeeded(defaultStatus, k-> {
            String[] value = queryParameters.get(k);
            if(value ==null || value.length ==0){
                return null;
            }
            return value[0];
        });
    }

    private int overrideErrorCodeIfNeeded(int defaultStatus, Function<String,String> parameterFunction){
            //GSRS-1598 force not found error to sometimes be a 500 instead of 404
            //if requests tells us
            try {
                String specifiedResponse = parameterFunction.apply(errorCodeParameter);
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

    private int overrideErrorCodeIfNeeded(int defaultStatus, Map<String, String> queryParameters){
        return overrideErrorCodeIfNeeded(defaultStatus, queryParameters::get);
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
    public ResponseEntity<Object> handleBadRequest(int defaultStatus, Map<String, String> queryParameters) {
        int status = overrideErrorCodeIfNeeded(defaultStatus, queryParameters);
        return new ResponseEntity<>( createStatusJson("bad request", status), HttpStatus.valueOf(status));

    }
    public ResponseEntity<Object> handleBadRequest(Map<String, String> queryParameters) {
       return handleBadRequest(400, queryParameters);
    }
    public ResponseEntity<Object> handleError(Throwable t, Map<String, String> queryParameters) {
        int status = overrideErrorCodeIfNeeded(500, queryParameters);
        return new ResponseEntity<>( getError(t, status), HttpStatus.valueOf(status));

    }
    public ResponseEntity<Object> handleError(int defaultStatus, Throwable t, WebRequest request) {
        int status = overrideErrorCodeIfNeeded(defaultStatus, request);
        return new ResponseEntity<>( getError(t, status), HttpStatus.valueOf(status));

    }
    public ResponseEntity<Object> handleError(Throwable t, WebRequest request) {
      return handleError(500, t, request);
    }
    public HttpStatus getHttpStatusFor(HttpStatus origStatus, Map<String, String> queryParameters) {
        int code = origStatus.value();
        int newCode = overrideErrorCodeIfNeeded(code, queryParameters);
        if(code == newCode){
            return origStatus;
        }
        return HttpStatus.valueOf(newCode);
    }
    public HttpStatus getHttpStatusFor(HttpStatus origStatus, WebRequest request) {
        int code = origStatus.value();
        int newCode = overrideErrorCodeIfNeeded(code, request);
        if(code == newCode){
            return origStatus;
        }
        return HttpStatus.valueOf(newCode);
    }

    public int getStatusFor(int originalStatusCode, Map<String, String[]> parameterMap) {
        return overrideErrorCodeIfNeededServlet(originalStatusCode, parameterMap);
    }

    @Data
    @Builder
    public static class ErrorInfo{
        private Object body;
        private HttpStatus status;
    }

    public static Object getError(Throwable t, int status){

        Map m=new HashMap();
        if(t instanceof InvocationTargetException){
            m.put("message", ((InvocationTargetException)t).getTargetException().getMessage());
        }else{
            m.put("message", t.getMessage());
        }
        m.put("status", status);
        return m;
    }

    public static Object createStatusJson(String message, int status){
        Map m=new HashMap();
        m.put("message", message);

        m.put("status", status);
        return m;
    }
}
