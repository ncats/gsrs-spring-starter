package gsrs.controller.hateoas;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default mapping of Entities to Controllers using a HashMap.
 *
 */
public class DefaultGsrsEntityToControllerMapper implements GsrsEntityToControllerMapper{

    private Map<Class, Class> entityToController = new ConcurrentHashMap<>();

    public void addController(Class entity, Class controller){
        entityToController.put(Objects.requireNonNull(entity), Objects.requireNonNull(controller));
    }
    @Override
    public Optional<Class> getControllerFor(Class entity) {
        Class c = entity;
        do {
            Class controller = entityToController.get(c);
            if(controller !=null){
                return Optional.of(controller);
            }
            c = c.getSuperclass();
        }while(c !=null);
        return Optional.empty();
    }
}
