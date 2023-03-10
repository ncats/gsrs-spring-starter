package gsrs.startertests.payload;

import gsrs.startertests.GsrsSpringApplication;
import gsrs.repository.FileDataRepository;
import gsrs.repository.PayloadRepository;
import gsrs.payload.LegacyPayloadService;
import gsrs.service.PayloadService;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.Payload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@GsrsJpaTest(classes = GsrsSpringApplication.class)
public class LegacyPayloadEntityServiceTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    private LegacyPayloadService sut;

    @Autowired
    private PayloadRepository payloadRepository;
    @Autowired
    private FileDataRepository fileDataRepository;

//    @BeforeEach
//    public void setup() throws IOException {
//        LegacyPayloadConfiguration conf = new LegacyPayloadConfiguration();
//        File payloadDir = new File(tempDir, "payload");
//
//        Files.createDirectories(payloadDir.toPath());
//        conf.setRootDir(payloadDir);
//        sut = new LegacyPayloadService(payloadRepository,conf, fileDataRepository);
//    }

    @Test
    public void createPayloadTempPersist() throws IOException {
        Payload payload = sut.createPayload("testPayload", "text/plain", "foo bar baz");

        assertNotNull(payload.id);
        assertNotNull(payload.sha1);
        assertEquals("testPayload", payload.name);

        File fetchedFile = sut.getPayloadAsFile(payload).get();
        assertEquals(new String(Files.readAllBytes(fetchedFile.toPath())), "foo bar baz");

        assertEquals(0, fileDataRepository.count());
    }

    @Test
    public void createPayloadPermPersist() throws IOException {
        Payload payload = sut.createPayload("testPayload", "text/plain", "foo bar baz", PayloadService.PayloadPersistType.PERM);

        assertNotNull(payload.id);
        assertNotNull(payload.sha1);
        assertEquals("testPayload", payload.name);

        File fetchedFile = sut.getPayloadAsFile(payload).get();
        assertEquals(new String(Files.readAllBytes(fetchedFile.toPath())), "foo bar baz");

        assertEquals(1, fileDataRepository.count());

        assertEquals(new String(fileDataRepository.findBySha1(payload.sha1).get().data), "foo bar baz");
    }

    @Test
    public void sameSha1ShouldReuseFilePermPersist() throws IOException {
        Payload payload = sut.createPayload("testPayload", "text/plain", "foo bar baz", PayloadService.PayloadPersistType.PERM);

        assertNotNull(payload.id);
        assertNotNull(payload.sha1);
        assertEquals("testPayload", payload.name);

        File fetchedFile = sut.getPayloadAsFile(payload).get();


        Payload payload2 = sut.createPayload("testPayload", "text/plain", "foo bar baz", PayloadService.PayloadPersistType.PERM);

        assertEquals(payload.sha1, payload2.sha1);
        assertEquals(payload.id, payload2.id);
        File fetchedFile2 = sut.getPayloadAsFile(payload2).get();

        assertEquals(fetchedFile.getAbsolutePath(), fetchedFile2.getAbsolutePath());

        assertEquals(new String(Files.readAllBytes(fetchedFile.toPath())), "foo bar baz");

        assertEquals(1, fileDataRepository.count());
    }

}
