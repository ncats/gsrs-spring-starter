package gsrs.stagingarea.service;

import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.repository.ImportMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import gsrs.legacy.LegacyGsrsSearchService;

@Service
public class ImportMetadataLegacySearchService extends LegacyGsrsSearchService<ImportMetadata> {

    @Autowired
    public ImportMetadataLegacySearchService(ImportMetadataRepository repository) {
        super(ImportMetadata.class, repository);
    }
}
