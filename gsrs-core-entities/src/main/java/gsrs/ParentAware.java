package gsrs;

public interface ParentAware {
    default Object parentObject() {
        return null;
    }
}
