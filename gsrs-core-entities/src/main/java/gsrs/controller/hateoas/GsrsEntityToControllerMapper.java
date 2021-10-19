package gsrs.controller.hateoas;

import lombok.Data;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A Mapping of GSRS Entity classes to their
 * Controller.  This assumes that
 * all entities have a single controller.
 */
public interface GsrsEntityToControllerMapper {
    /**
     * Get the Controller Class for the given GSRS Entity.
     * This method will search up the entity's class hierarchy for parent
     * entity classes until it finds a Controller.
     * @param entity the entity to search.
     * @return an Optional containing either the Class of the Controller;
     * or empty if none could be found.
     */
    Optional<Class> getControllerFor(Class entity);

    Stream<GsrsControllerInfo> getControllerInfos();

}
