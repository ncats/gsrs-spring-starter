package gsrs.payload;

import gov.nih.ncats.common.io.IOUtil;
import gsrs.controller.*;
import gsrs.repository.PayloadRepository;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExposesResourceFor(Payload.class)
//not plural so it matches context from GSRS 2.x
@GsrsRestApiController(context="payload")
public class PayloadController{

    @Autowired
    private PayloadService payloadService;

    @Autowired
    private PayloadRepository payloadRepository;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @PostGsrsRestApiMapping("/upload")
    public ResponseEntity<Object> handleFileUpload(
            @RequestParam("file-type") String type,
            @RequestParam("file-name") String name,
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> queryParameters) throws IOException {

        //String type = requestData.get("file-type");
        //		String nam = requestData.get("file-name");

        Payload payload = payloadService.createPayload(name, type, file.getBytes(), PayloadService.PayloadPersistType.PERM);
        //OK to match GSRS 2.x API
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(payload, queryParameters), HttpStatus.OK);
    }

    @GetGsrsRestApiMapping("({id})")
    public Object get(@PathVariable("id") UUID id,
                      @RequestParam Map<String, String> queryParameters) throws IOException {
        Optional<Payload> payloadOptional = payloadRepository.findById(id);
        if(payloadOptional.isPresent()){
            Optional<File> in = payloadService.getPayloadAsFile(payloadOptional.get());
            if(in.isPresent()){
                String mimeType = payloadOptional.get().mimeType;
                File file = in.get();
                byte[] data;
                try(InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                    data= IOUtil.toByteArray(stream);
                }
                return ResponseEntity.ok()
                         .contentType(MediaType.parseMediaType(mimeType))
                         .contentLength(file.length())
                         .header(HttpHeaders.CONTENT_DISPOSITION,
                                 "attachment; filename=\"" + payloadOptional.get().name + "\"")
                         .body(data);

            }
        }
        //if we get here we didn't have a payload with that ID or couldn't find it

        return gsrsControllerConfiguration.handleNotFound(queryParameters);

    }
}
