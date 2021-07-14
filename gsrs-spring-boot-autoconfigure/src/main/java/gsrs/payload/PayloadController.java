package gsrs.payload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.activation.MimetypesFileTypeMap;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import gov.nih.ncats.common.io.IOUtil;
import gsrs.controller.GetGsrsRestApiMapping;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.GsrsControllerUtil;
import gsrs.controller.GsrsRestApiController;
import gsrs.controller.PostGsrsRestApiMapping;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModelProcessor;
import gsrs.repository.PayloadRepository;
import gsrs.service.PayloadService;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.models.Payload;

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
    public ResponseEntity<Object> handleFileUpload(MultipartHttpServletRequest mpreq,
//          @RequestParam("file-type") String type, 
//          @RequestParam("file-name") String name, 
          @RequestParam("file-name") MultipartFile file, 
          @RequestParam Map<String, String> queryParameters) throws IOException {

        
   
            Payload payload = payloadService.createPayload(file.getOriginalFilename(), predictMimeTypeFromFile(file), file.getBytes(), PayloadService.PayloadPersistType.PERM);
            GsrsUnwrappedEntityModel unwrapped = GsrsControllerUtil.enhanceWithView(payload, queryParameters);

            //This is a very hacky way to force things as expected
            Link l1=  StaticContextAccessor.getBean(GsrsUnwrappedEntityModelProcessor.class).computeSelfLink(unwrapped, payload.id.toString());
            payload.url=l1.getHref() + "&format=raw";
            
            //OK to match GSRS 2.x API
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(payload, queryParameters), HttpStatus.OK);
    }
    
    public static String predictMimeTypeFromFile(MultipartFile file) {
        String defMime = "application/octet-stream";
        String ofname = file.getOriginalFilename();
        
        
        
        String mimeType = URLConnection.guessContentTypeFromName(ofname);
        if(mimeType==null) {
            MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
            mimeType = fileTypeMap.getContentType(ofname);
            if(mimeType==null || mimeType.equals(defMime)) {
                Tika tika = new Tika();
                try {
                    mimeType = tika.detect(ofname);
                } catch (Exception e) {
                    try {
                        mimeType = tika.detect(file.getInputStream());
                    }catch(Exception e2) {
                        
                    }                    
                }
            }
        }
        if(mimeType==null) {
            mimeType = defMime;
        }
        return mimeType;
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
