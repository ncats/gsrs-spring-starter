package gsrs.security;


public class NonAuthenticatedUserAllowedException extends RuntimeException {
    public NonAuthenticatedUserAllowedException(String msg) {
        super(msg);
    }
}
