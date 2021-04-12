import ix.seqaln.SequenceIndexer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class SequenceIndexerTest {

    @TempDir
    File dir;

    SequenceIndexer sut;

    @BeforeEach
    public void setup() throws  Exception{
        sut =  SequenceIndexer.open(dir);
    }

    @AfterEach
    public void tearDown(){
        sut.shutdown();
    }

    @Test
    public void noRecordsNoResults(){
        SequenceIndexer.ResultEnumeration result = sut.search("ACTGACGT", .5, SequenceIndexer.CutoffType.SUB, "nucleicacid");
        assertFalse(result.hasMoreElements());
    }
    @Test
    public void addOneRecordSearchForSomethingElseShouldHaveNoResult() throws IOException {
        sut.addNucleicAcidSequence("myId", "ACGTACGT");
        SequenceIndexer.ResultEnumeration result = sut.search("ACTGACGT", .5, SequenceIndexer.CutoffType.SUB, "nucleicacid");
        assertFalse(result.hasMoreElements());
    }
    @Test
    public void oneHit() throws IOException {
        sut.addNucleicAcidSequence("myId", "ACGTACGT");
        SequenceIndexer.ResultEnumeration result = sut.search("ACGTACGT", .5, SequenceIndexer.CutoffType.SUB, "nucleicacid");
        assertTrue(result.hasMoreElements());

            SequenceIndexer.Result r = result.nextElement();
            assertEquals("myId", r.id);
            assertEquals("ACGTACGT", r.alignments.get(0).target);
        assertEquals("ACGTACGT", r.alignments.get(0).query);
        assertFalse(result.hasMoreElements());
    }
    @Test
    public void oneHit100percentIdentitySearch() throws IOException {
        sut.addNucleicAcidSequence("myId", "ACGTACGT");
        SequenceIndexer.ResultEnumeration result = sut.search("ACGTACGT", 1, SequenceIndexer.CutoffType.SUB, "nucleicacid");
        assertTrue(result.hasMoreElements());

        SequenceIndexer.Result r = result.nextElement();
        assertEquals("myId", r.id);
        assertEquals("ACGTACGT", r.alignments.get(0).target);
        assertEquals("ACGTACGT", r.alignments.get(0).query);
        assertFalse(result.hasMoreElements());
    }
    @Test
    public void noHitsWhen100percentIdentitySearch() throws IOException {
        sut.addNucleicAcidSequence("myId", "ACGTACGT");
        SequenceIndexer.ResultEnumeration result = sut.search("ACGGACGT", 1, SequenceIndexer.CutoffType.SUB, "nucleicacid");
        assertFalse(result.hasMoreElements());

    }
    @Test
    public void noHitsWhenLoadNucButSearchProt() throws IOException {
        sut.addNucleicAcidSequence("myId", "ACGTACGT");
        SequenceIndexer.ResultEnumeration result = sut.search("ACGTACGT", .5, SequenceIndexer.CutoffType.SUB, "protein");
        assertFalse(result.hasMoreElements());

    }
}
