package gsrs.service;

import gov.nih.ncats.common.io.InputStreamSupplier;
import ix.core.models.Payload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface PayloadService {

    enum PayloadPersistType{
        /**
         * imply that the
         * data is not expected to be used in a long term fashion, and
         * can be deleted from its persistence area after some time.
         */
        TEMP,
        /**
         * implies that the data is meant to be kept until explicitly removed.
         */
        PERM
    }

    default Payload createPayload (String name, String mime, String content) throws IOException{
        return createPayload(name, mime, content, PayloadPersistType.TEMP);
    }

    default Payload createPayload (String name, String mime, byte[] content) throws IOException{
        return createPayload(name, mime, content, PayloadPersistType.TEMP);
    }

    default Payload createPayload (String name, String mime, InputStream content) throws IOException{
        return createPayload(name, mime, content, PayloadPersistType.TEMP);
    }

    default Payload createPayload (String name, String mime, String content, PayloadPersistType persistType) throws IOException{
        return createPayload(name, mime, content.getBytes("utf-8"), persistType);
    }

    default Payload createPayload (String name, String mime, byte[] content, PayloadPersistType persistType) throws IOException{
        return createPayload(name, mime, new ByteArrayInputStream(content), persistType);
    }

    Payload createPayload (String name, String mime, InputStream content, PayloadPersistType persistType) throws IOException;

    Optional<InputStream> getPayloadAsInputStream(Payload payload) throws IOException;

    default Optional<InputStream> getPayloadAsUncompressedInputStream(Payload payload)throws IOException {
        Optional<InputStream> compressed = getPayloadAsInputStream(payload);
        if(!compressed.isPresent()){
            return compressed; //empty
        }
        return Optional.of(InputStreamSupplier.forInputStream(compressed.get()).get());
    }

    Optional<File> getPayloadAsFile(Payload payload) throws IOException;
}
