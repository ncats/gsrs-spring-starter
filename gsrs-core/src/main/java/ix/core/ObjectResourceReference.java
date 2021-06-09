package ix.core;

import ix.core.util.EntityUtils;
import lombok.Getter;

import java.util.function.Supplier;
@Getter
public class ObjectResourceReference extends ResourceReference{

    private final Class entityClass;
    private String id;

    private String fieldPath;
    public ObjectResourceReference(String classAsString, String idAsString) {
        this(classAsString, idAsString, null);
    }
    public ObjectResourceReference(String classAsString, String idAsString, String fieldPath) {
        super("fakeUrl", ()->idAsString);
        this.id = idAsString;
        this.fieldPath = fieldPath;
        try {
            this.entityClass = EntityUtils.getEntityInfoFor(classAsString).getEntityClass();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public ObjectResourceReference(Class entityClass, String idAsString) {
        this(entityClass, idAsString, null);
    }
    public ObjectResourceReference(Class entityClass, String idAsString, String fieldPath) {
        super("fakeUrl", ()->idAsString);
        this.entityClass = entityClass;
        this.id = idAsString;
        this.fieldPath = fieldPath;
    }
}
