package ix.seqaln.service;

import ix.seqaln.configuration.LegacySequenceAlignmentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class LegacySequenceIndexerService implements SequenceIndexerService {
    @Value("${ix.sequence.base}")
    private File dir;

    private LegacySequenceAlignmentConfiguration configuration;
}
