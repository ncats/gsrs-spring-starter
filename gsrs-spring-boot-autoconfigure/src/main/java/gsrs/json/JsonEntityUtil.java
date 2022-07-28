package gsrs.json;

import gov.nih.ncats.common.sneak.Sneak;
import ix.core.models.ParentReference;
import ix.core.util.EntityUtils;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility class for working on entity objects that have been deserialized from JSON.
 */
@Slf4j
public final class JsonEntityUtil {

    private JsonEntityUtil(){
        //can not instantiate
    }

    private static void setOwners(Object obj, boolean force){
        EntityUtils.EntityWrapper.of(obj)
                .traverse()
                .execute((parent, path, current)->{
                    if(current==null || parent ==null){
                        return;
                    }
                    Object currentObj = current.getValue();

                    Class<?> aClass = currentObj.getClass();
                    do {
                        setOwner(parent.getValue(), currentObj, aClass, force);
                        aClass = aClass.getSuperclass();
                    } while (aClass != null);

                });
    }

    private static void setOwner(Object owner, Object obj, Class<?> aClass, boolean force) {
        boolean found=false;
        for(Method m: aClass.getDeclaredMethods()){
            try {
                m.setAccessible(true);
                if (m.getAnnotation(ParentReference.class) != null) {
                    try {
                        m.invoke(obj, owner);
                    } catch (Throwable e) {
                        log.trace("error executing class {} method {}. Error: {}", aClass.getName(), m.getName(), e.getMessage());
                        Sneak.sneakyThrow(e);
                    }
                    found = true;
                }
            } catch (Throwable outer){
                log.trace("class: {}; method: {}; outer error: {} ", aClass.getName(), m.getName(), outer.getMessage());
                Sneak.sneakyThrow(outer);
            }
        }
        if(!found) {
            for (Field f : aClass.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getAnnotation(ParentReference.class) != null) {
                    try {
                        if (force || f.get(obj) == null) {
                            f.set(obj, owner);
                        }
                    } catch (IllegalAccessException e) {
                        Sneak.sneakyThrow(e);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Traverse the given entity and set any null field that is annotated
     * with @{@link ParentReference}.
     * Depending on how an Entity's JSON is deserialized,
     * an object's parent or owner field might not be set correctly which may affect
     * how an object is persisted.
     * @param entity the entity to fix.
     * @param <T> the type.
     * @return the same entity object but now with possibly updated fields.
     * @see #fixOwners(Object, boolean)
     * @apiNote this is the same as {@link #fixOwners(Object, boolean) fixOwners(entity, false)}
     */
    public static <T> T fixOwners(T entity){
        return fixOwners(entity, false);
    }
    /**
     * Traverse the given entity and set fields that are annotated
     * with @{@link ParentReference}.
     * Depending on how an Entity's JSON is deserialized,
     * an object's parent or owner field might not be set correctly which may affect
     * how an object is persisted.
     * @param entity the entity to fix.
     * @param force update the field even if not null.
     * @param <T> the type.
     * @return the same entity object but now with possibly updated fields.
     */
    public static <T> T fixOwners(T entity, boolean force){
        setOwners(entity, force);
        return entity;
    }
}
