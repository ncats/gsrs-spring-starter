package gsrs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gsrs.security.hasAdminRole;

import ix.core.util.EntityUtils.Key;

import ix.core.validator.ValidationResponse;
import lombok.Data;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.Principal;
import java.util.List;
import java.util.Map;

public interface GsrsEntityController<T, I> extends GsrsRetrievalEntityController<T, I>{

    @PreAuthorize("isAuthenticated()")
    @PostGsrsRestApiMapping()
    ResponseEntity<Object> createEntity(@RequestBody JsonNode newEntityJson,
                                        @RequestParam Map<String, String> queryParameters,
                                        Principal principal) throws IOException;

    @PreAuthorize("isAuthenticated()")
    @PostGsrsRestApiMapping("/@validate")
    ValidationResponse<T> validateEntity(@RequestBody JsonNode updatedEntityJson, @RequestParam Map<String, String> queryParameters) throws Exception;

    @PreAuthorize("isAuthenticated()")
    @PutGsrsRestApiMapping("")
    ResponseEntity<Object> updateEntity(@RequestBody JsonNode updatedEntityJson,
                                        @RequestParam Map<String, String> queryParameters,
                                        Principal principal) throws Exception;


    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"({id})/@rebackup", "/{id}/@rebackup"})
    ResponseEntity<Object> rebackupEntity(@PathVariable String id, @RequestParam Map<String, String> queryParameters) throws Exception;

    @hasAdminRole
    @PutGsrsRestApiMapping("/@rebackup")
    ResponseEntity<Object> rebackupEntities(@RequestBody ArrayNode idList, @RequestParam Map<String, String> queryParameters) throws Exception;


    @PreAuthorize("isAuthenticated()")
    @DeleteGsrsRestApiMapping(value = {"({id})", "/{id}" })
    ResponseEntity<Object> deleteById(@PathVariable String id, @RequestParam Map<String, String> queryParameters);

}
