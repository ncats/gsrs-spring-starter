package gsrs.controller;

import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a special controller that shouldn't be used by clients
 * and is only used internally by GSRS to help it figure out
 * what the URL paths are when computing new links.
 */
@RestController
@ExposesResourceFor(RelativePathDummyObject.class)
public class RelativePathController {



    @GetMapping(RelativePathDummyObject.ROUTE_PATH)
    public Object getFoo(){
        return new RelativePathDummyObject();
    }
}
