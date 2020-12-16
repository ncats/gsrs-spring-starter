package gsrs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.repository.EditRepository;
import ix.core.models.Edit;
import ix.core.validator.ValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.EntityResponse;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
@GsrsRestApiController(context = "edits")
public class EditController implements GsrsEntityController{

    @Autowired
    private EditRepository editRepository;
    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;


    private ResponseEntity<Object> addLinksTo(Edit edit) {
        GsrsUnwrappedEntityModel<Edit> model = new GsrsUnwrappedEntityModel<>(edit, getClass());
        model.add(GsrsLinkUtil.fieldLink("$oldValue", linkTo(methodOn(getClass()).getById(edit.id.toString(), null)).withRel("oldValue")));
        model.add(GsrsLinkUtil.fieldLink("$newValue", linkTo(methodOn(getClass()).getById(edit.id.toString(), null)).withRel("newValue")));
        model.add(GsrsLinkUtil.fieldLink("$diff", linkTo(methodOn(getClass()).getById(edit.id.toString(), null)).withRel("diff")));

        return new ResponseEntity<>(model, HttpStatus.OK);

    }

    @Override
    public ResponseEntity<Object> createEntity(JsonNode newEntityJson, Map queryParameters, Principal principal) throws IOException {
        return null;
    }

    @Override
    public ValidationResponse validateEntity(JsonNode updatedEntityJson, Map queryParameters) throws Exception {
        return null;
    }

    @Override
    public ResponseEntity<Object> updateEntity(JsonNode updatedEntityJson, Map queryParameters, Principal principal) throws Exception {
        return null;
    }

    @Override
    public ResponseEntity<Object> getFieldById(String id, Map queryParameters, HttpServletRequest request) {
        return null;
    }

    @Override
    public long getCount() {
        return editRepository.count();
    }

    @Override
    public ResponseEntity<Object> page(long top, long skip, String order, Map queryParameters) {
        return null;
    }

    @Override
    public ResponseEntity<Object> getById(String id, Map queryParameters) {
        UUID uuid = UUID.fromString(id);
        Optional<Edit> opt= editRepository.findById(uuid);
        if(opt.isPresent()){
            return addLinksTo(opt.get());
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @Override
    public ResponseEntity<Object> deleteById(String id, Map queryParameters) {
        return null;
    }
}
