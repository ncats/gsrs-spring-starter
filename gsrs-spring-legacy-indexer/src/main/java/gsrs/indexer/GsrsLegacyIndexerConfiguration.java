package gsrs.indexer;

import gsrs.indexer.job.ReindexJobController;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ReindexJobController.class)
public class GsrsLegacyIndexerConfiguration {
}
