package ix.core.search.text;

import gov.nih.ncats.common.util.CachedSupplier;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Configuration properties for the Legacy TextIndexer
 */
@Component
@Data
public class TextIndexerConfig {
    @Value("#{new Boolean('${ix.textindex.enabled: true}')}")
    private boolean enabled = true;
    @Value("#{new Boolean('${ix.textindex.fieldsuggest: true}')}")
    private boolean fieldsuggest;
    @Value("#{new Boolean('${ix.textindex.shouldLog: false}')}")
    private boolean shouldLog;
//    private static final boolean USE_ANALYSIS =    ConfigHelper.getBoolean("ix.textindex.fieldsuggest",true);

    @Value("#{new Integer('${ix.fetchWorkerCount: 4}')}")
    private int fetchWorkerCount = 4;
    @Value("${ix.index.deepfields:}")
    private List<String> deepFields = new ArrayList<>();
}
