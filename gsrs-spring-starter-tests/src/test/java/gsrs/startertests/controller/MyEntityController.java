package gsrs.startertests.controller;

import gsrs.controller.AbstractGsrsEntityController;
import gsrs.controller.GsrsRestApiController;
import gsrs.service.GsrsEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;

import java.util.UUID;
@ExposesResourceFor(MyEntity.class)
@GsrsRestApiController(context= MyEntityService.CONTEXT)
public class MyEntityController extends AbstractGsrsEntityController<MyEntityController, MyEntity, UUID> {
    @Autowired
    private MyEntityService entityService;
    @Override
    public GsrsEntityService<MyEntity, UUID> getEntityService() {
        return entityService;
    }
}
