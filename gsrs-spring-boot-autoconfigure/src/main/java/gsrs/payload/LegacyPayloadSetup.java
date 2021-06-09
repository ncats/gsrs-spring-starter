package gsrs.payload;

import gsrs.repository.FileDataRepository;
import gsrs.repository.PayloadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Configuration
@Import(LegacyPayloadConfiguration.class)
public class LegacyPayloadSetup {
    @Autowired
    private PayloadRepository payloadRepository;

    @Autowired
    private FileDataRepository fileDataRepository;


    @ConditionalOnMissingBean
    @Bean
    public LegacyPayloadService legacyPayloadService( LegacyPayloadConfiguration conf) throws IOException {
        return new LegacyPayloadService(payloadRepository, conf, fileDataRepository);
    }
}
