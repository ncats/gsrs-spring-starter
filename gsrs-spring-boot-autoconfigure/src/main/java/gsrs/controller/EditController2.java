package gsrs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.repository.EditRepository;
import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import ix.core.validator.ValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@GsrsRestApiController(context = "edits")
public class EditController2 extends AbstractGsrsEntityController<EditController2, Edit, UUID>{


    @Override
    protected void addAdditionalLinks(GsrsUnwrappedEntityModel model) {
        Object obj = model.getObj();
        if(obj instanceof Edit){
            Edit edit = (Edit) obj;
            model.add(GsrsLinkUtil.fieldLink("$oldValue",
                    linkTo(methodOn(getClass()).getFieldById(edit.id.toString(), null, null)).withRel("oldValue").expand(edit.id)));
            model.add(GsrsLinkUtil.fieldLink("$newValue", linkTo(methodOn(getClass()).getFieldById(edit.id.toString(), null, null)).withRel("newValue").expand(edit.id)));
            model.add(GsrsLinkUtil.fieldLink("$diff", linkTo(methodOn(getClass()).getFieldById(edit.id.toString(), null,null)).withRel("diff").expand(edit.id)));

        }
    }


}
