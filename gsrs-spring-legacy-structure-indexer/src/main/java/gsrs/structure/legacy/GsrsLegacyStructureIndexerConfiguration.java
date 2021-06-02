package gsrs.structure.legacy;

import gsrs.legacy.structureIndexer.LegacyStructureIndexerService;
import gsrs.legacy.structureIndexer.StructureIndexerService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
@Data
public class GsrsLegacyStructureIndexerConfiguration {

    @Value("${ix.structure.base}")
    private File dir;

    @Bean
    @ConditionalOnMissingBean(StructureIndexerService.class)
    public LegacyStructureIndexerService legacyStructureIndexerService() throws IOException {
        return new LegacyStructureIndexerService(dir);
    }
}
