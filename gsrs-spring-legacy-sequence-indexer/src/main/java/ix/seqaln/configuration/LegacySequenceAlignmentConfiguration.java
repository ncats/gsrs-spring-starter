package ix.seqaln.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("ix.kmer")
@Data
public class LegacySequenceAlignmentConfiguration {

    private int defaultKmer=3;
    private int nuc=3;
    private int prot = 3;


    /*
      int kmersize = app.configuration().getInt("ix.kmer.default", 3);
            int nuc = app.configuration().getInt("ix.kmer.nuc", 3);
            int prot = app.configuration().getInt("ix.kmer.protein", 3);
     */
}
