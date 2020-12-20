package gsrs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.repository.EditRepository;
import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import ix.core.validator.ValidationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;
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
@ExposesResourceFor(Edit.class)
@GsrsRestApiController(context = "edits")
public class EditController2 extends AbstractGsrsEntityController<EditController2, Edit, UUID>{




}
