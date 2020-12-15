package gsrs.startertests.controller;

import gsrs.controller.AbstractGsrsEntityController;
import gsrs.controller.GsrsRestApiController;

import java.util.UUID;

@GsrsRestApiController(context= MyEntityService.CONTEXT)
public class MyEntityController extends AbstractGsrsEntityController<MyEntityController, MyEntity, UUID> {
    @Override
    protected Class<MyEntityController> controllerClass() {
        return MyEntityController.class;
    }
}
