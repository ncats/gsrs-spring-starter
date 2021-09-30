package gsrs.indexer.job;

import gsrs.cache.GsrsCache;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.GsrsControllerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@ExposesResourceFor(ReindexJob.class)
public class ReindexJobController {

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private GsrsCache gsrsCache;

    @GetMapping(value={"api/v1/jobs({ID})", "api/v1/jobs/{ID}"})
    public ResponseEntity getReindexJob(@PathVariable("ID") UUID uuid,
                                        @RequestParam Map<String, String> queryParameters){
        Object o = gsrsCache.get(uuid.toString());
        if(o ==null){
            gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        ReindexJob job = (ReindexJob) o;
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(job, queryParameters), HttpStatus.OK);

    }
}
