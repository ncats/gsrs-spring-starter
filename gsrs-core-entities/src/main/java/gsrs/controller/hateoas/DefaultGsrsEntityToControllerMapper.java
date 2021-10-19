package gsrs.controller.hateoas;

import gsrs.controller.GsrsRestApiController;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Default mapping of Entities to Controllers using a HashMap.
 *
 */
public class DefaultGsrsEntityToControllerMapper implements GsrsEntityToControllerMapper{

    private Map<Class, Class> entityToController = new ConcurrentHashMap<>();

    private Set<GsrsControllerInfo> infos = new LinkedHashSet<>();

    public void addController(Class entity, Class controller, GsrsRestApiController gsrsRestApiController){
        entityToController.put(Objects.requireNonNull(entity), Objects.requireNonNull(controller));
        if(gsrsRestApiController !=null) {
            infos.add(GsrsControllerInfo.builder()
                    .description(gsrsRestApiController.description())
                    .kind(entity.getName())
                    .name(gsrsRestApiController.context()[0])
                    .build());
        }
    }

    @Override
    public Stream<GsrsControllerInfo> getControllerInfos(){
        return infos.stream();

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
