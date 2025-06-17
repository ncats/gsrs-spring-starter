package gsrs.validator;

import gov.nih.ncats.common.util.CachedSupplier;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationMessage;
import ix.core.validator.Validator;
import ix.core.validator.ValidatorCallback;
import ix.ginas.utils.validation.AbstractValidatorPlugin;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * A validator that uses Spring Expression Language (SpEL)
 * to evaluate validation expressions.  If the expression
 * evaluates to {@code true}, then the call back message
 * (which can also be a SpEL expression) with the given messageType
 *  will be included in the validation response.
 * @param <T>
 */
public class SpELValidator<T> extends AbstractValidatorPlugin<T> {

    private String expression;

    private String message;
    private Expression exp;

    private Expression callbackExpression;

    private ValidationMessage.MESSAGE_TYPE messageType = ValidationMessage.MESSAGE_TYPE.WARNING;

    @Override
    public void initialize() throws IllegalStateException {
        if(expression ==null){
            throw new IllegalStateException("expression can not be null");
        }
        if(message ==null){
            throw new IllegalStateException("message can not be null");
        }
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ValidationMessage.MESSAGE_TYPE getMessageType() {
        return messageType;
    }

    public void setMessageType(ValidationMessage.MESSAGE_TYPE messageType) {
        this.messageType = messageType;
    }

    private CachedSupplier initializer = CachedSupplier.runOnceInitializer(()->{
        ExpressionParser parser = new SpelExpressionParser();
        exp = parser.parseExpression(expression);

        callbackExpression = parser.parseExpression(message);
    });

    @Override
    public void validate(T objnew, T objold, ValidatorCallback callback) {
        initializer.getSync();
        ValidationParameters<T> validationParameters = new ValidationParameters<>(objnew, objold);
        boolean result = exp.getValue(validationParameters, boolean.class);
        if(result){
            callback.addMessage(createMessage(callbackExpression.getValue(validationParameters, String.class)));
        }
    }
    private ValidationMessage createMessage(String message){
        switch(messageType){
            case WARNING: return GinasProcessingMessage.WARNING_MESSAGE("W8960000", message);
            case ERROR: return GinasProcessingMessage.ERROR_MESSAGE("E8960000", message);
            case NOTICE: return GinasProcessingMessage.NOTICE_MESSAGE("N8960000", message);
            case INFO: return GinasProcessingMessage.INFO_MESSAGE("I8960000", message);
            case SUCCESS: return GinasProcessingMessage.SUCCESS_MESSAGE("S8960000", message);

            default: return GinasProcessingMessage.WARNING_MESSAGE("W8960000", message);
        }
    }
    @Data
    @AllArgsConstructor
    public class ValidationParameters<T>{
        private T objNew, objOld;
    }
}
