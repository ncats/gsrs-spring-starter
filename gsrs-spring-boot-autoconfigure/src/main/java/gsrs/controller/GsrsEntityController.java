package gsrs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gsrs.security.canRunBackup;
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

public interface GsrsEntityController<T, I> {

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

    @GetGsrsRestApiMapping(value={"({id})/**", "/{id}/**"  })
    ResponseEntity<Object> getFieldById(@PathVariable String id, @RequestParam(value="urldecode", required = false) Boolean urlDecode, @RequestParam Map<String, String> queryParameters, HttpServletRequest request) throws UnsupportedEncodingException;

    @GetGsrsRestApiMapping("/@count")
    long getCount();
    
    @GetGsrsRestApiMapping("/@keys")
    List<Key> getKeys();

    @GetGsrsRestApiMapping("")
    ResponseEntity<Object> page(@RequestParam(value = "top", defaultValue = "16") long top,
                                @RequestParam(value = "skip", defaultValue = "0") long skip,
                                @RequestParam(value = "order", required = false) String order,
                                @RequestParam Map<String, String> queryParameters);

    @GetGsrsRestApiMapping(value = {"({id})", "/{id}"})
    ResponseEntity<Object> getById(@PathVariable String id, @RequestParam Map<String, String> queryParameters);


    @canRunBackup
    @GetGsrsRestApiMapping(value = {"({id})/@rebackup", "/{id}/@rebackup"})
    ResponseEntity<Object> rebackupEntity(@PathVariable String id, @RequestParam Map<String, String> queryParameters) throws Exception;

    @canRunBackup
    @PutGsrsRestApiMapping("/@rebackup")
    ResponseEntity<Object> rebackupEntities(@RequestBody ArrayNode idList, @RequestParam Map<String, String> queryParameters) throws Exception;


    @PreAuthorize("isAuthenticated()")
    @DeleteGsrsRestApiMapping(value = {"({id})", "/{id}" })
    ResponseEntity<Object> deleteById(@PathVariable String id, @RequestParam Map<String, String> queryParameters);

    @PostGsrsRestApiMapping("/@exists")
    ExistsCheckResult entitiesExists(@RequestBody List<String> idList, @RequestParam Map<String, String> queryParameters) throws Exception;

    @Data
    class ExistsCheckResult{
        private Map<String, EntityExists> found;
        private List<String> notFound;
    }
    @Data
    class EntityExists{
        private String id;
        private String query;
        private String url;

    }
	
}
