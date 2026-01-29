package gsrs.controller;

import gsrs.security.canConfigureSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@canConfigureSystem
public class LogController {

    @Autowired
    private GsrsAdminLogConfiguration gsrsAdminLogConfiguration;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @GetMapping(value={"api/v1/admin/files","api/v1/admin/files/"})
    public List<GsrsAdminLogConfiguration.LogFileInfo> getLogFiles() throws IOException {
        return gsrsAdminLogConfiguration.getAllFiles();
    }
    @GetMapping(value={"api/v1/admin/logs","api/v1/admin/logs/"})
    public List<GsrsAdminLogConfiguration.LogFileInfo> getAllFiles() throws IOException {
        return gsrsAdminLogConfiguration.getLogFilesFor(gsrsAdminLogConfiguration.getLogPath());
    }

    @GetMapping("api/v1/admin/logs/**")
    public Object downloadLogFile(@RequestParam Map<String, String> queryParameters,  HttpServletRequest request) throws IOException {

        return downloadFile(queryParameters, request, gsrsAdminLogConfiguration.getLogPath());

    }

    @GetMapping("api/v1/admin/files/**")
    public Object downloadFile(@RequestParam Map<String, String> queryParameters,  HttpServletRequest request) throws IOException {

        return downloadFile(queryParameters, request, null);

    }

    private ResponseEntity<?> downloadFile(Map<String, String> queryParameters, HttpServletRequest request, String subPath) throws IOException {
        String path = GsrsControllerUtil.getEndWildCardMatchingPartOfUrl(request);

        Path root = gsrsAdminLogConfiguration.getRootPath().toPath().toAbsolutePath().normalize();
        Path file = subPath==null?root.resolve(path): root.resolve(subPath).resolve(path);
        GsrsAdminLogConfiguration.CanDownloadAnswer answer = gsrsAdminLogConfiguration.isAllowedToBeDownloaded(file);
        if(answer == GsrsAdminLogConfiguration.CanDownloadAnswer.NOT_FOUND){
            return gsrsControllerConfiguration.handleNotFound(queryParameters, "could not find download " + path);

        }
        if(answer == GsrsAdminLogConfiguration.CanDownloadAnswer.RESTRICTED){
            return gsrsControllerConfiguration.handleForbidden(queryParameters, "not allowed to access file : " + path);

        }
        Path absolutePath = file.normalize().toAbsolutePath();
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(absolutePath));
            File asFile = absolutePath.toFile();
            return ResponseEntity.ok()
                    .contentLength(asFile.length())
                    .header("Content-disposition", "attachment; filename=" + asFile.getName())
                    .header("Content-Type", "application/x-download")


                    .body(resource);


         }


}
