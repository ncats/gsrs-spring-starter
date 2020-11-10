package gsrs.model;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.SoftException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class SetterInterceptor {
    @Before("execution(* set*(*)) && target(instance) && args(newValue)")
    public void beforeSetterCalled(JoinPoint thisJoinPoint, Object instance, Object newValue) {
        String methodName = thisJoinPoint.getSignature().getName();
        try {
            System.out.println(
                    methodName.substring(3) + ": " +
                            instance
                                    .getClass()
                                    .getMethod(methodName.replaceFirst("set", "get"))
                                    .invoke(instance) +
                            " -> " + newValue
            );
        } catch (Exception e) {
            throw new SoftException(e);
        }
    }
}
