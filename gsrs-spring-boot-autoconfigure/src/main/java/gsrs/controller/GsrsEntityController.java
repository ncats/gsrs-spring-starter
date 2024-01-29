package gsrs.controller;

import com.fasterxml.jackson.databind.JsonNode;
import ix.core.validator.ValidationResponse;
import lombok.Data;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.Principal;
import java.util.List;
import java.util.Map;

public interface GsrsEntityController<T, I> {

    @PostGsrsRestApiMapping()
    ResponseEntity<Object> createEntity(@RequestBody JsonNode newEntityJson,
                                        @RequestParam Map<String, String> queryParameters,
                                        Principal principal) throws IOException;

    @PostGsrsRestApiMapping("/@validate")
    ValidationResponse<T> validateEntity(@RequestBody JsonNode updatedEntityJson, @RequestParam Map<String, String> queryParameters) throws Exception;

    @PutGsrsRestApiMapping("")
    ResponseEntity<Object> updateEntity(@RequestBody JsonNode updatedEntityJson,
                                        @RequestParam Map<String, String> queryParameters,
                                        Principal principal) throws Exception;

    @GetGsrsRestApiMapping(value={"({id})/**", "/{id}/**"  })
    ResponseEntity<Object> getFieldById(@PathVariable String id, @RequestParam(value="urldecode", required = false) Boolean urlDecode, @RequestParam Map<String, String> queryParameters, HttpServletRequest request) throws UnsupportedEncodingException;

    @GetGsrsRestApiMapping("/@count")
    long getCount();

    @GetGsrsRestApiMapping("")
    ResponseEntity<Object> page(@RequestParam(value = "top", defaultValue = "16") long top,
                                @RequestParam(value = "skip", defaultValue = "0") long skip,
                                @RequestParam(value = "order", required = false) String order,
                                @RequestParam Map<String, String> queryParameters);

    @GetGsrsRestApiMapping(value = {"({id})", "/{id}"})
    ResponseEntity<Object> getById(@PathVariable String id, @RequestParam Map<String, String> queryParameters);

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
