package gsrs.controller;

import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


public class RelativePathDummyObject {
    /**
     * An route path that is both known and chosen to be unlikely to be used by another controller
     */
    public static final String ROUTE_PATH = "/exampleOnlyUsedForPathDetermination";

}
