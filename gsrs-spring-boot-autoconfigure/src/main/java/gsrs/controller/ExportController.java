package gsrs.controller;

import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.service.ExportService;
import ix.ginas.exporters.ExportDir;
import ix.ginas.exporters.ExportMetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExposesResourceFor(ExportMetaData.class)
@GsrsRestApiController(context = "profile/downloads")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @PreAuthorize("isAuthenticated()")
    @GetGsrsRestApiMapping("")
    public List<ExportMetaData> myDownloads(Principal principal){
        return exportService.getExplicitExportMetaData(principal.getName());
    }
    @PreAuthorize("isAuthenticated()")
    @GetGsrsRestApiMapping(value = {"/{id}", "({id})"})
    public ResponseEntity<Object> getStatusOf(@PathVariable("id") String id, Principal principal, @RequestParam Map<String, String> parameters){
        Optional<ExportMetaData> opt = exportService.getStatusFor(principal.getName(), id);
        if(!opt.isPresent()){
            return new ResponseEntity<>("could not find etag with Id " + id,gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, parameters));

        }


        return new ResponseEntity<>(GsrsUnwrappedEntityModel.of(opt.get()), HttpStatus.OK);
    }
    @PreAuthorize("isAuthenticated()")
    @DeleteGsrsRestApiMapping(value = {"/{id}", "({id})"})
    public ResponseEntity<Object> delete(@PathVariable("id") String id, Principal principal, @RequestParam Map<String, String> parameters){
        Optional<ExportMetaData> opt = exportService.getStatusFor(principal.getName(), id);
        if(!opt.isPresent()){
            return new ResponseEntity<>("could not find exported data with Id " + id,gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, parameters));

        }
        exportService.remove(opt.get());


        return new ResponseEntity<>("removed", HttpStatus.ACCEPTED);
    }
    @PreAuthorize("isAuthenticated()")
    @GetGsrsRestApiMapping(value = {"/{id}/download", "({id})/download"})
    public ResponseEntity<Object> download(@PathVariable("id") String id, Principal principal, @RequestParam Map<String, String> parameters) throws IOException {
        Optional<ExportMetaData> opt = exportService.getStatusFor(principal.getName(), id);
        if(!opt.isPresent()){
            return new ResponseEntity<>("could not find exported data with Id " + id,gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, parameters));

        }

        Optional<ExportDir.ExportFile<ExportMetaData>> exportFile = exportService.getFile(principal.getName(), opt.get().getFilename());
        if(!opt.get().isComplete()){
           //should we not return unless complete?
            return new ResponseEntity<>("export not completed" + id,gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, parameters));

        }
        if(!exportFile.isPresent()){
            return new ResponseEntity<>("could not find exported file from Id " + id,gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, parameters));

        }
        File f = exportFile.get().getFile();

        Path path = Paths.get(f.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .contentLength(f.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}
