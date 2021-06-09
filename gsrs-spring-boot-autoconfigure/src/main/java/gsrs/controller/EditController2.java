package gsrs.controller;

import gsrs.service.GsrsEntityService;
import ix.core.models.Edit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@ExposesResourceFor(Edit.class)
@GsrsRestApiController(context = "edits")
public class EditController2 extends AbstractGsrsEntityController<EditController2, Edit, UUID>{

    @Autowired
    private EditEntityService editEntityService;

    @Override
    public GsrsEntityService<Edit, UUID> getEntityService() {
        return editEntityService;
    }
}
