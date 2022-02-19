package gsrs.sequence;


import ix.core.models.SequenceEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

public interface SequenceFileSupport {

    interface SequenceFileData{
        enum SequenceFileType{
            FASTA,
            //TODO add more formats if needed (fastq, etc)
            ;
        }
        SequenceEntity.SequenceType getSequenceType();
        SequenceFileType getSequenceFileType();
        InputStream createInputStream() throws IOException;
        String getName();
    }
    boolean hasSequenceFiles();

    Stream<SequenceFileData> getSequenceFileData();
}
