package gsrs.controller;

import gsrs.security.NonAuthenticatedUserAllowedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;


/**
 * A Spring Controller Advice that is used to intercept different
 * Exceptions thrown by the controller so we can centralize
 * how to respond.
 *
 * This class used the {@link GsrsControllerConfiguration}
 * to allow the API to override the error code.
 */
@RestControllerAdvice(assignableTypes = AbstractGsrsEntityController.class)
public class GsrsApiControllerAdvice {
    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

//    @Value("${ix.sysadmin}")
//    private String sysAdminContact;

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity handleUnchecked(RuntimeException ex, WebRequest request){
        if(ex instanceof NonAuthenticatedUserAllowedException){
            return handleNotAuthenticatedUser((NonAuthenticatedUserAllowedException) ex, request);
        }
        ex.printStackTrace();
        return gsrsControllerConfiguration.handleError(ex, request);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public ResponseEntity<Object> handleAccessDeniedException(Exception ex, WebRequest request) {
        return gsrsControllerConfiguration.handleError(HttpStatus.UNAUTHORIZED.value(), ex, request);

    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity handleInvalidJsonInput(HttpMessageNotReadableException ex, WebRequest request){
        return gsrsControllerConfiguration.handleError(ex, request);
    }
    @ExceptionHandler(NonAuthenticatedUserAllowedException.class)
    public ResponseEntity handleNotAuthenticatedUser(NonAuthenticatedUserAllowedException ex, WebRequest request){

//        String message;
//        if(sysAdminContact ==null){
//            message= "You are not authorized to see this resource. Please contact an administrator to be granted access.";
//        }else{
//            message = "You are not authorized to see this resource. Please contact " +
//                    sysAdminContact
//                    + " to be granted access.";
//        }
        String message= "You are not authorized to see this resource. Please contact an administrator to be granted access.";

        //TODO should we use the controller config to override status code here?
        return  ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML)

                                            .body("<!DOCTYPE html>\n" +
                                                    "<html>\n" +
                                                    "<head></head>\n" +
                                                    "<body>\n" +
                                                    message +
                                                    "</body>\n" +
                                                    "</html>");


    }

}
