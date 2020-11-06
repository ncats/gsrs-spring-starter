package gsrs;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Session;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.core.annotation.AnnotationUtils;

import javax.persistence.Version;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Custom Dirtiness Checker for G-SRS Entities since
 * Hibernate usually doesn't consider an entity "dirty"
 * when only it's children are updated.
 *
 * But we want the parent entity to be dirty so it's version
 * and last modified info get updated not only when something
 * in the entity's table row is updated but also when any child atribute is updated
 * as well.
 *
 * @see GsrsManualDirtyMaker
 */
public class GsrsEntityDirtinessStrategy implements CustomEntityDirtinessStrategy {

    //A Cache of the (possibly inherited) field that is mapped by a @Version
    private Map<Class, Optional<String>> versionFieldByClass = new ConcurrentHashMap<>();


    @Override
    public boolean canDirtyCheck(Object o, EntityPersister entityPersister, Session session) {
        return canCast(o) && !((GsrsManualDirtyMaker)o).getDirtyFields().isEmpty();
    }

    private boolean canCast(Object o) {
        return (o instanceof GsrsManualDirtyMaker);
    }


    @Override
    public boolean isDirty(Object o, EntityPersister entityPersister, Session session) {
        if(canCast(o)){
            if(!((GsrsManualDirtyMaker)o).getDirtyFields().isEmpty()){
                return true;
            }
        }
        return false;
   }

    @Override
    public void resetDirty(Object o, EntityPersister entityPersister, Session session) {
        if(canCast(o)){
            ((GsrsManualDirtyMaker)o).clearDirtyFields();
        }

    }
    private static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    private Optional<String> getVersionedFieldFor(Class clazz) {
        return versionFieldByClass.computeIfAbsent(clazz, c -> {
            List<Field> fields = getAllFields(new ArrayList<>(), c);
            return fields.stream().filter(f -> f.getDeclaredAnnotation(Version.class) != null).map(Field::getName).findAny();
        });
    }

    @Override
    public void findDirty(Object o, EntityPersister entityPersister, Session session, DirtyCheckContext dirtyCheckContext) {
        if(canCast(o)) {
            Optional<String> versionField = getVersionedFieldFor(o.getClass());

            Set<String> dirtyFields = ((GsrsManualDirtyMaker) o).getDirtyFields();
            if(!dirtyFields.isEmpty()){
                dirtyCheckContext.doDirtyChecking(attributeInformation -> {
                    //we always want to say the @Version field is dirty so it gets updated!
                    if(versionField.isPresent() && versionField.get().equals(attributeInformation.getName())){
                       return true;
                    }
                    return dirtyFields.contains(attributeInformation.getName());
//
                });
            }
        }
   }
}
