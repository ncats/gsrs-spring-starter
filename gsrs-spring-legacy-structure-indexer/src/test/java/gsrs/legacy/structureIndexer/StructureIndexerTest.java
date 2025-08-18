package gsrs.legacy.structureIndexer;

import gov.nih.ncats.common.io.IOUtil;
import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.search.MolSearcherFactory;

import gov.nih.ncats.structureIndexer.StructureIndexer;
import ix.core.util.EntityUtils.EntityWrapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


public class StructureIndexerTest{

    @TempDir
	File tempDir;

	StandardizedStructureIndexer structureIndexer;
	

	@BeforeEach
	public void createIndexer() throws IOException {
		structureIndexer = new StandardizedStructureIndexer(StructureIndexer.open(tempDir));
	}
	@AfterEach
	public void shutdown(){
		structureIndexer.shutdown();
	}

	@Test
	public void directoryDoesNotExistShouldCreateIt() throws Exception {
		shutdown();
		IOUtil.deleteRecursively(tempDir);
		assertFalse(tempDir.exists());
		createIndexer();
		ensureIndexing2StructuresWithSameIdReturnsTheIdTwiceWhenSearchMatches();
	}

	@Test
	public void ensureIndexing2StructuresWithSameIdReturnsTheIdTwiceWhenSearchMatches() throws Exception{
		String id="1234567";
		String structure="C1CCCCC1";
		structureIndexer.add(id, structure);
		structureIndexer.add(id, structure);
		
		assertEquals(2, StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
	}
	
	@Test
	public void ensureIndexing2StructuresWithSameIdThenDeletingTheIDReturnsNothing() throws Exception{
		String id="1234567";
		String structure="C1CCCCC1";
		structureIndexer.add(id, structure);
		structureIndexer.add(id, structure);
		structureIndexer.remove(null,id);
		assertEquals(0,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());		
	}
	
	@Test
	//TODO fixme
	@Disabled("currently fails for CDK molwitch")
	public void ensureIsobutaneSSSDoesntReturnIsoPentene() throws Exception{


		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209182020002D\n" + 
				"\n" + 
				"  4  3  0  0  0  0              0 V2000\n" + 
				"   23.1921   -7.4013    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   23.6531   -8.8915    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   25.1741   -9.2375    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   22.5929  -10.0359    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  1  0  0  0  0\n" + 
				"  2  3  1  0  0  0  0\n" + 
				"  2  4  1  0  0  0  0\n" + 
				"M  END");
		
		Chemical t=Chemical.parseMol("\n" + 
				"   JSDraw209182020002D\n" + 
				"\n" + 
				"  5  4  0  0  0  0              0 V2000\n" + 
				"   23.1921   -7.4013    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   23.6531   -8.8915    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   25.1741   -9.2375    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   22.5929  -10.0359    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   26.2344   -8.0932    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  2  0  0  0  0\n" + 
				"  2  3  1  0  0  0  0\n" + 
				"  2  4  1  0  0  0  0\n" + 
				"  3  5  1  0  0  0  0\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		assertEquals("false",""+hit.isPresent());
	}
	
	@Test
	//TODO fixme
	@Disabled("currently fails for CDK molwitch")
	public void ensureSubstructureSearchHasBasicSmartsSupport() throws Exception{

		String structure="[#7,#8]c1ccc(O)c2c(O)c([#6])c3OC([#6])(O)C(=O)c3c12";
		structureIndexer.add("1", "COC1=CC=C(O)C2=C(O)C(C)=C3OC(C)(O)C(=O)C3=C12");
		structureIndexer.add("2", "CC1=C2OC(C)(O)C(=O)C2=C3C4=C(C=C(O)C3=C1O)N5C=CC=CC5=N4");
		assertEquals(2,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
	}
	
	@Test
	public void ensureBenzeneSearchWorks() throws Exception{

		String structure="C1=CC=CC=C1";
		structureIndexer.add("1", "C1=CC=CC=C1");
		assertEquals(1,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
	}
	
	@Test
	public void ensureBenzeneAromaticSearchWorks() throws Exception{

		String structure="c1ccccc1";
		structureIndexer.add("1", "c1ccccc1");
		assertEquals(1,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
	}
	
	@Test
	public void ensureSingleDoubleBondSearchDoNotReturnAromaticBenzene() throws Exception{

		String structure="CC=C";
		structureIndexer.add("1", "c1ccccc1");
		assertEquals(0,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
	}
	
	@Test
	public void ensureSingleDoubleBondSearchDoNotReturnKekulizeBenzene() throws Exception{

		String structure="CC=C";
		structureIndexer.add("1", "C1=CC=CC=C1");
		assertEquals(0,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
	}
	
	@Test
	public void ensureSingleDoubleBondSearchDoesReturnSingleDoubleBondStructure() throws Exception{

		String structure="CC=C";
		structureIndexer.add("1", "CC=CC=CC");
		assertEquals(1,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
	}
	
	@Test
	public void ensureBasicSingleDoubleBondSearchDoesReturnSingleDoubleBondStructure() throws Exception{


		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209162018192D\n" + 
				"\n" + 
				"  3  2  0  0  0  0            999 V2000\n" + 
				"   18.0440   -9.4640    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   19.3950   -8.6840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   20.7460   -9.4640    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  1  0  0  0  0\n" + 
				"  2  3  2  0  0  0  0\n" + 
				"M  END");
		
		Chemical t=Chemical.parseMol("\n" + 
				"   JSDraw209162018202D\n" + 
				"\n" + 
				"  6  5  0  0  0  0            999 V2000\n" + 
				"   19.1880  -14.0400    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   17.8370  -13.2600    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   17.8370  -11.7000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   20.5390  -13.2600    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   20.5390  -11.7000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   19.1880  -10.9200    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  2  3  1  0  0  0  0\n" + 
				"  1  4  1  0  0  0  0\n" + 
				"  4  5  2  0  0  0  0\n" + 
				"  5  6  1  0  0  0  0\n" + 
				"  6  3  2  0  0  0  0\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		assertEquals("true",""+hit.isPresent());
	}
	
	@Test
	public void ensureBasicBenzeneSearchWorksKekule() throws Exception{


		
		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209162017232D\n" + 
				"\n" + 
				"  6  6  0  0  0  0            999 V2000\n" + 
				"   26.4680  -12.3240    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   25.1170  -11.5440    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   25.1170   -9.9840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   27.8190  -11.5440    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   27.8190   -9.9840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   26.4680   -9.2040    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  2  0  0  0  0\n" + 
				"  2  3  1  0  0  0  0\n" + 
				"  1  4  1  0  0  0  0\n" + 
				"  4  5  2  0  0  0  0\n" + 
				"  5  6  1  0  0  0  0\n" + 
				"  6  3  2  0  0  0  0\n" + 
				"M  END");
		Chemical t=Chemical.parseMol("\n" + 
				"   JSDraw209162017232D\n" + 
				"\n" + 
				"  6  6  0  0  0  0            999 V2000\n" + 
				"   26.4680  -12.3240    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   25.1170  -11.5440    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   25.1170   -9.9840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   27.8190  -11.5440    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   27.8190   -9.9840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   26.4680   -9.2040    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  2  0  0  0  0\n" + 
				"  2  3  1  0  0  0  0\n" + 
				"  1  4  1  0  0  0  0\n" + 
				"  4  5  2  0  0  0  0\n" + 
				"  5  6  1  0  0  0  0\n" + 
				"  6  3  2  0  0  0  0\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		assertEquals("true",""+hit.isPresent());
	}
	
	@Test
	public void ensureBasicBenzeneSearchWorksAromatic() throws Exception{


		
		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209162017232D\n" + 
				"\n" + 
				"  6  6  0  0  0  0            999 V2000\n" + 
				"   26.4680  -12.3240    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   25.1170  -11.5440    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   25.1170   -9.9840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   27.8190  -11.5440    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   27.8190   -9.9840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   26.4680   -9.2040    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  4  0  0  0  0\n" + 
				"  2  3  4  0  0  0  0\n" + 
				"  1  4  4  0  0  0  0\n" + 
				"  4  5  4  0  0  0  0\n" + 
				"  5  6  4  0  0  0  0\n" + 
				"  6  3  4  0  0  0  0\n" + 
				"M  END");
		Chemical t=Chemical.parseMol("\n" + 
				"   JSDraw209162017232D\n" + 
				"\n" + 
				"  6  6  0  0  0  0            999 V2000\n" + 
				"   26.4680  -12.3240    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   25.1170  -11.5440    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   25.1170   -9.9840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   27.8190  -11.5440    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   27.8190   -9.9840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   26.4680   -9.2040    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  4  0  0  0  0\n" + 
				"  2  3  4  0  0  0  0\n" + 
				"  1  4  4  0  0  0  0\n" + 
				"  4  5  4  0  0  0  0\n" + 
				"  5  6  4  0  0  0  0\n" + 
				"  6  3  4  0  0  0  0\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		assertEquals("true",""+hit.isPresent());
	}
	
	@Test
	public void ensureBasicSSSCanGiveNegativeResult() throws Exception{
		
		
		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209162010482D\n" + 
				"\n" + 
				"  1  0  0  0  0  0            999 V2000\n" + 
				"   15.8080   -7.0436    0.0000 P   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"M  END");
		Chemical t=Chemical.parseMol("\n" + 
				"   JSDraw209162010482D\n" + 
				"\n" + 
				"  1  0  0  0  0  0            999 V2000\n" + 
				"   15.8080   -7.0436    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		assertEquals("false",""+hit.isPresent());
		
		
	}
	
	@Test
	public void ensureBasicSSSCanGivePositiveResult() throws Exception{
		
		
		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209162010482D\n" + 
				"\n" + 
				"  1  0  0  0  0  0            999 V2000\n" + 
				"   15.8080   -7.0436    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"M  END");
		Chemical t=Chemical.parseMol("\n" + 
				"   JSDraw209162010482D\n" + 
				"\n" + 
				"  1  0  0  0  0  0            999 V2000\n" + 
				"   15.8080   -7.0436    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		assertEquals("true",""+hit.isPresent());
		
		
	}
	
	@Test
	public void ensureBasicSSSCanGivePositiveResultOnStrictSubstructure() throws Exception{
		
		
		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209162010482D\n" + 
				"\n" + 
				"  1  0  0  0  0  0            999 V2000\n" + 
				"   15.8080   -7.0436    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"M  END");
		Chemical t=Chemical.parseMol("\n" + 
				"   JSDraw209162016432D\n" + 
				"\n" + 
				"  2  0  0  0  0  0            999 V2000\n" + 
				"   19.0840   -8.3720    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   22.7760   -9.9320    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		assertEquals("true",""+hit.isPresent());
		
		
	}
	
	@Test
	public void ensureBasicSSSCanGivePositiveResultOnStrictSubstructureConnected() throws Exception{
		
		
		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209162010482D\n" + 
				"\n" + 
				"  1  0  0  0  0  0            999 V2000\n" + 
				"   15.8080   -7.0436    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"M  END");
		Chemical t=Chemical.parseMol("\n" + 
				"   JSDraw209162016482D\n" + 
				"\n" + 
				"  2  1  0  0  0  0            999 V2000\n" + 
				"   19.0840   -8.3720    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   22.7760   -9.9320    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  1  0  0  0  0\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		assertEquals("true",""+hit.isPresent());		
	}
	
	@Test
	public void ensureBasicSSSCanGivePositiveResultOnStrictSubstructureConnectedQueryConnectedTarget() throws Exception{
		
		
		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209162016482D\n" + 
				"\n" + 
				"  2  1  0  0  0  0            999 V2000\n" + 
				"   19.0840   -8.3720    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   22.7760   -9.9320    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  1  0  0  0  0\n" + 
				"M  END");
		Chemical t=Chemical.parseMol("\n" + 
				"   JSDraw209162016482D\n" + 
				"\n" + 
				"  2  1  0  0  0  0            999 V2000\n" + 
				"   19.0840   -8.3720    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   22.7760   -9.9320    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  1  0  0  0  0\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		assertEquals("true",""+hit.isPresent());		
	}
	
	@Test
	public void ensureBasicSSSCanGivePositiveResultOnStrictSubstructureDisconnectedQueryConnectedTarget() throws Exception{
		
		
		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209162016482D\n" + 
				"\n" + 
				"  2  0  0  0  0  0            999 V2000\n" + 
				"   19.0840   -8.3720    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   22.7760   -9.9320    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"M  END");
		Chemical t=Chemical.parseMol("\n" + 
				"   JSDraw209162016482D\n" + 
				"\n" + 
				"  2  1  0  0  0  0            999 V2000\n" + 
				"   19.0840   -8.3720    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   22.7760   -9.9320    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  1  0  0  0  0\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		assertEquals("true",""+hit.isPresent());		
	}
	
	@Test
	public void ensureBasicSSSCanGiveNegativeResultOnCyclohexaneCase() throws Exception{
		
		
		Chemical p=Chemical.parseMol("\n" + 
				"   JSDraw209162014542D\n" + 
				"\n" + 
				"  6  6  0  0  0  0              0 V2000\n" + 
				"   23.1920   -9.2560    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   21.8410   -8.4760    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   21.8410   -6.9160    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   23.1920   -6.1360    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   24.5430   -6.9160    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"   24.5430   -8.4760    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" + 
				"  1  2  1  0  0  0  0\n" + 
				"  2  3  1  0  0  0  0\n" + 
				"  3  4  1  0  0  0  0\n" + 
				"  4  5  1  0  0  0  0\n" + 
				"  5  6  1  0  0  0  0\n" + 
				"  6  1  1  0  0  0  0\n" + 
				"M  END");
		Chemical t=Chemical.parseMol("\n" + 
				"  Symyx   08281518122D 1   1.00000     0.00000     0\n" + 
				"\n" + 
				" 27 29  0  0  1  0            999 V2000\n" + 
				"    7.1375   -2.2042    0.0000 N   0  0  3  0  0  0           0  0  0\n" + 
				"    6.0500   -1.8583    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    6.0500   -0.7125    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    7.1292   -0.3542    0.0000 N   0  0  0  0  0  0           0  0  0\n" + 
				"    7.8083   -1.2792    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    5.0042   -0.0375    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    5.0042   -2.4667    0.0000 N   0  0  0  0  0  0           0  0  0\n" + 
				"    3.9333   -0.6458    0.0000 N   0  0  0  0  0  0           0  0  0\n" + 
				"    3.9333   -1.8583    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    5.0042    1.1417    0.0000 N   0  0  0  0  0  0           0  0  0\n" + 
				"    7.1042   -3.9750    0.0000 C   0  0  1  0  0  0           0  0  0\n" + 
				"    6.7583   -5.0833    0.0000 C   0  0  1  0  0  0           0  0  0\n" + 
				"    6.1333   -3.2625    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				"    5.1750   -3.9625    0.0000 C   0  0  1  0  0  0           0  0  0\n" + 
				"    5.5583   -5.0833    0.0000 C   0  0  1  0  0  0           0  0  0\n" + 
				"    4.1500   -3.3708    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    3.1250   -3.9625    0.0000 S   0  3  2  0  0  0           0  0  0\n" + 
				"    2.1000   -3.3708    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    1.0750   -3.9625    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    0.0500   -3.3708    0.0000 C   0  0  1  0  0  0           0  0  0\n" + 
				"   -0.9750   -3.9625    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"   -2.0000   -3.3708    0.0000 O   0  5  0  0  0  0           0  0  0\n" + 
				"    5.2458   -6.2250    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				"    7.3458   -6.1083    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				"    3.1208   -5.1417    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    0.0458   -2.1917    0.0000 N   0  0  0  0  0  0           0  0  0\n" + 
				"   -0.9792   -5.1417    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				" 11  1  1  1     0  0\n" + 
				" 12 11  1  0     0  0\n" + 
				" 13 11  1  0     0  0\n" + 
				" 14 13  1  0     0  0\n" + 
				" 15 12  1  0     0  0\n" + 
				" 15 14  1  0     0  0\n" + 
				"  6  3  1  0     0  0\n" + 
				" 14 16  1  1     0  0\n" + 
				"  7  2  1  0     0  0\n" + 
				" 16 17  1  0     0  0\n" + 
				"  8  9  1  0     0  0\n" + 
				" 17 18  1  0     0  0\n" + 
				"  9  7  2  0     0  0\n" + 
				" 18 19  1  0     0  0\n" + 
				" 10  6  1  0     0  0\n" + 
				" 19 20  1  0     0  0\n" + 
				"  3  4  1  0     0  0\n" + 
				" 20 21  1  0     0  0\n" + 
				"  8  6  2  0     0  0\n" + 
				" 21 22  1  0     0  0\n" + 
				" 15 23  1  6     0  0\n" + 
				"  2  1  1  0     0  0\n" + 
				" 12 24  1  6     0  0\n" + 
				"  3  2  2  0     0  0\n" + 
				" 17 25  1  1     0  0\n" + 
				"  4  5  2  0     0  0\n" + 
				" 20 26  1  1     0  0\n" + 
				"  5  1  1  0     0  0\n" + 
				" 21 27  2  0     0  0\n" + 
				"M  CHG  2  17   1  22  -1\n" + 
				"M  END");
		
		
		
		Optional<int[]> hit = MolSearcherFactory.create(p).get().search(t);
		
		if(hit.isPresent()){
			System.out.println(Arrays.toString(hit.get()));
		}
		assertEquals("false",""+hit.isPresent());
		
		
	}

	@Test
	//TODO fixme
	@Disabled("currently fails for CDK molwitch")
	public void ensureSubstructureSearchHasBasicSmartsSupportForAnyBond() throws Exception{

		String structure="[#7,#8]~C1=c2c3c(OC([#6])(O)C3=O)cc(O)c2=C(O)\\C=C/1";
		structureIndexer.add("1", "COC1=CC=C(O)C2=C(O)C(C)=C3OC(C)(O)C(=O)C3=C12");
		structureIndexer.add("2", "CC1=C2OC(C)(O)C(=O)C2=C3C4=C(C=C(O)C3=C1O)N5C=CC=CC5=N4");
		assertEquals(2,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
	}
	
	@Test
	public void ensureSearchForPhosphorousInNonPhosphorousStructureReturnsNothing() throws Exception{

		String structure="P";
		structureIndexer.add("1", "\n"
				+ "  Symyx   08281518352D 1   1.00000     0.00000     0\n"
				+ "\n" + 
				" 13 13  0  0  0  0            999 V2000\n" + 
				"   -1.5207    0.5690    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    0.1103   -0.3621    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"   -3.1655   -0.3621    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"   -1.5207    2.4828    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    0.1103   -2.2690    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    1.7793    0.6069    0.0000 Se  0  0  0  0  0  0           0  0  0\n" + 
				"   -3.1655   -2.2690    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    0.1103    3.4069    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				"   -3.1655    3.4069    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				"   -1.5207   -3.2414    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    3.4345   -0.3621    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    5.0793    0.6069    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    3.4345   -2.2172    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				"  1  2  2  0     0  0\n" + 
				"  1  3  1  0     0  0\n" + 
				"  1  4  1  0     0  0\n" + 
				"  2  5  1  0     0  0\n" + 
				"  2  6  1  0     0  0\n" + 
				"  3  7  2  0     0  0\n" + 
				"  4  8  1  0     0  0\n" + 
				"  4  9  2  0     0  0\n" + 
				"  5 10  2  0     0  0\n" + 
				"  6 11  1  0     0  0\n" + 
				" 11 12  1  0     0  0\n" + 
				" 11 13  2  0     0  0\n" + 
				"  7 10  1  0     0  0\n" + 
				"M  END");
		assertEquals(0,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
	}
	
	@Test
	public void ensureCyclohexaneSearchInNonCyclohexaneStructureReturnsNothing() throws Exception{

		String structure="C1CCCCC1";
		structureIndexer.add("1", "\n" + 
				"  Symyx   08281518122D 1   1.00000     0.00000     0\n" + 
				"\n" + 
				" 27 29  0  0  1  0            999 V2000\n" + 
				"    7.1375   -2.2042    0.0000 N   0  0  3  0  0  0           0  0  0\n" + 
				"    6.0500   -1.8583    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    6.0500   -0.7125    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    7.1292   -0.3542    0.0000 N   0  0  0  0  0  0           0  0  0\n" + 
				"    7.8083   -1.2792    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    5.0042   -0.0375    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    5.0042   -2.4667    0.0000 N   0  0  0  0  0  0           0  0  0\n" + 
				"    3.9333   -0.6458    0.0000 N   0  0  0  0  0  0           0  0  0\n" + 
				"    3.9333   -1.8583    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    5.0042    1.1417    0.0000 N   0  0  0  0  0  0           0  0  0\n" + 
				"    7.1042   -3.9750    0.0000 C   0  0  1  0  0  0           0  0  0\n" + 
				"    6.7583   -5.0833    0.0000 C   0  0  1  0  0  0           0  0  0\n" + 
				"    6.1333   -3.2625    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				"    5.1750   -3.9625    0.0000 C   0  0  1  0  0  0           0  0  0\n" + 
				"    5.5583   -5.0833    0.0000 C   0  0  1  0  0  0           0  0  0\n" + 
				"    4.1500   -3.3708    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    3.1250   -3.9625    0.0000 S   0  3  2  0  0  0           0  0  0\n" + 
				"    2.1000   -3.3708    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    1.0750   -3.9625    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    0.0500   -3.3708    0.0000 C   0  0  1  0  0  0           0  0  0\n" + 
				"   -0.9750   -3.9625    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"   -2.0000   -3.3708    0.0000 O   0  5  0  0  0  0           0  0  0\n" + 
				"    5.2458   -6.2250    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				"    7.3458   -6.1083    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				"    3.1208   -5.1417    0.0000 C   0  0  0  0  0  0           0  0  0\n" + 
				"    0.0458   -2.1917    0.0000 N   0  0  0  0  0  0           0  0  0\n" + 
				"   -0.9792   -5.1417    0.0000 O   0  0  0  0  0  0           0  0  0\n" + 
				" 11  1  1  1     0  0\n" + 
				" 12 11  1  0     0  0\n" + 
				" 13 11  1  0     0  0\n" + 
				" 14 13  1  0     0  0\n" + 
				" 15 12  1  0     0  0\n" + 
				" 15 14  1  0     0  0\n" + 
				"  6  3  1  0     0  0\n" + 
				" 14 16  1  1     0  0\n" + 
				"  7  2  1  0     0  0\n" + 
				" 16 17  1  0     0  0\n" + 
				"  8  9  1  0     0  0\n" + 
				" 17 18  1  0     0  0\n" + 
				"  9  7  2  0     0  0\n" + 
				" 18 19  1  0     0  0\n" + 
				" 10  6  1  0     0  0\n" + 
				" 19 20  1  0     0  0\n" + 
				"  3  4  1  0     0  0\n" + 
				" 20 21  1  0     0  0\n" + 
				"  8  6  2  0     0  0\n" + 
				" 21 22  1  0     0  0\n" + 
				" 15 23  1  6     0  0\n" + 
				"  2  1  1  0     0  0\n" + 
				" 12 24  1  6     0  0\n" + 
				"  3  2  2  0     0  0\n" + 
				" 17 25  1  1     0  0\n" + 
				"  4  5  2  0     0  0\n" + 
				" 20 26  1  1     0  0\n" + 
				"  5  1  1  0     0  0\n" + 
				" 21 27  2  0     0  0\n" + 
				"M  CHG  2  17   1  22  -1\n" + 
				"M  END");
		assertEquals(0,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
	}

    @Test
    //TODO: this test would have always worked on CDK,
    // but not on jchem. So it's misleading as-is.
    public void ensureSSSForSRUPolymerWorks() throws Exception{

        String structure="COC";
        structureIndexer.add("1", "\n"
                + "   JSDraw210262119232D\n"
                + "\n"
                + " 12 11  0  0  0  0              0 V2000\n"
                + "   18.4130   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   19.7640   -7.8260    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   21.1150   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   22.4660   -7.8260    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   23.8170   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   25.1680   -7.8260    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   26.5190   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   27.8700   -7.8260    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   29.2210   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   30.5720   -7.8260    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   31.9230   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   31.9230   -5.4860    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "  1  2  1  0  0  0  0\n"
                + "  2  3  1  0  0  0  0\n"
                + "  3  4  1  0  0  0  0\n"
                + "  4  5  1  0  0  0  0\n"
                + "  5  6  1  0  0  0  0\n"
                + "  6  7  1  0  0  0  0\n"
                + "  7  8  1  0  0  0  0\n"
                + "  8  9  1  0  0  0  0\n"
                + "  9 10  1  0  0  0  0\n"
                + " 10 11  1  0  0  0  0\n"
                + " 11 12  1  0  0  0  0\n"
                + "M  STY  1   1 SRU\n"
                + "M  SAL   1  3   4   5   6\n"
                + "M  SBL   1  2   3   6\n"
                + "M  SMT   1 A\n"
                + "M  SDI   1  4   21.6370   -9.0220   21.6370   -6.3700\n"
                + "M  SDI   1  4   26.1090   -6.3700   26.1090   -9.0220\n"
                + "M  END");
        assertEquals(1,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
    }
    
    @Test
    public void ensureSSSForSRUPolymerWIthPoorFormattingWorks() throws Exception{

        String structure="COC";
        structureIndexer.add("1", "\n"
                + "   JSDraw210262119232D\n"
                + "\n"
                + " 12 11  0  0  0  0              0 V2000\n"
                + "   18.4130   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   19.7640   -7.8260    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   21.1150   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   22.4660   -7.8260    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   23.8170   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   25.1680   -7.8260    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   26.5190   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   27.8700   -7.8260    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   29.2210   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   30.5720   -7.8260    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   31.9230   -7.0460    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "   31.9230   -5.4860    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n"
                + "  1  2  1  0  0  0  0\n"
                + "  2  3  1  0  0  0  0\n"
                + "  3  4  1  0  0  0  0\n"
                + "  4  5  1  0  0  0  0\n"
                + "  5  6  1  0  0  0  0\n"
                + "  6  7  1  0  0  0  0\n"
                + "  7  8  1  0  0  0  0\n"
                + "  8  9  1  0  0  0  0\n"
                + "  9 10  1  0  0  0  0\n"
                + " 10 11  1  0  0  0  0\n"
                + " 11 12  1  0  0  0  0\n"
                + "M  STY  1   1 FFF\n"
                + "M  SAL   1  7   4   5   6\n"
                + "M  SMT   1 A\n"
                + "M  SDI   1  9   21.6370   -9.0220   21.6370   -6.3700\n"
                + "M  SDI   1  4   26.1090   -6.3700   26.1090   -9.0220\n"
                + "M  END");
        assertEquals(1,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
    }
	
	//TODO move this to substances
/*
	@Test
	public void ensureAddingSubstanceAndRemovingGives0ResultsOnSearch() throws Exception{

		String structure="C1CCCCC1";
		ChemicalSubstance cs=new SubstanceBuilder()
				.asChemical()
				.setStructure(structure)
				.addName("Test")
				.generateNewUUID()
				.build();
		
		Java8ForOldEbeanHelper.makeStructureIndexesForBean(EntityPersistAdapter.getInstance(), EntityWrapper.of(cs));
		assertEquals(1,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());		
		Java8ForOldEbeanHelper.removeStructureIndexesForBean(EntityPersistAdapter.getInstance(), EntityWrapper.of(cs));
		assertEquals(0,StreamUtil.forEnumeration(structureIndexer.substructure(structure, 10)).count());
		
		
	}
	*/


	@Test
	public void testAddTroublesomeMolfile() throws Exception {
		String id = "C4B1M20D81";
		String fileName = "mols/" + id + ".mol";
		Chemical t=Chemical.parseMol(molfileC4B1M20D81Text);
		structureIndexer.add(id, t);
		assertEquals(1, StreamUtil.forEnumeration(structureIndexer.substructure(molfileC4B1M20D81Text, 10)).count());
	}

	String molfileC4B1M20D81Text = "DALBAVANCIN A0\n" +
			"  Marvin  01132111422D          \n" +
			"\n" +
			"648700  0  0  1  0            999 V2000\n" +
			"   25.6395   -7.7023    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.5932   -2.4085    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   29.9388   -2.3943    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   26.3465   -8.1125    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.3440   -3.1108    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   32.0599   -9.2673    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4934   -9.2909    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.9983   -8.6168    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.1737   -3.1108    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0599   -8.0983    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   32.7765   -7.6834    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -8.1031    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4915   -7.6928    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2146  -13.0858    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.3481   -7.6882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4981  -11.0350    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0632   -7.6976    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7909  -10.6155    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2079   -8.1077    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4886  -13.4911    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.1878   -1.6919    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   27.0443  -10.1583    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.3582   -1.6919    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   29.9292   -4.6429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7909   -9.9273    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   35.6331   -7.6740    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4886  -14.3161    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   29.9245   -7.6882    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   34.9214   -8.0888    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2173   -5.0530    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -8.0936    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7749   -8.1125    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -5.0483    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9264  -13.5053    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4981  -11.8507    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9292   -3.8226    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9245   -6.2835    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2051  -14.7309    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   28.5009   -4.6429    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3386   -9.6728    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9264  -14.3255    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   34.2049   -7.6788    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   25.6395   -6.2929    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   27.0443   -9.3333    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2146  -12.2608    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   34.1954  -10.4835    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7749   -8.9280    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.4228   -2.4085    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3529   -4.6335    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6190  -10.4835    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.1042   -2.3943    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.3559   -5.8827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3720  -10.1064    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3386  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7670  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3449   -9.2579    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3449   -8.0841    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   29.2173   -5.8733    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7890   -5.0577    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -5.8780    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0457  -10.8984    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7863  -12.2560    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6190   -9.6632    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7670   -9.6774    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -4.6476    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.3559   -5.0625    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7608  -10.5731    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.3924   -1.9747    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0696  -11.0209    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0415   -8.5131    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3481   -6.8632    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0632   -6.8773    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2079   -8.9280    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3308  -10.8984    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6331   -6.8538    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0696  -11.8411    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -8.9137    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   34.2049   -6.2787    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4821   -9.3380    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -6.2929    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0567   -9.6681    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.8331   -1.6966    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.4474  -10.9172    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7890   -5.8827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0646   -5.0436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.3924   -1.1499    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4821  -10.1630    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0567  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7721  -13.0763    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.3446   -9.7845    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9481   -0.9661    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.6073   -0.9801    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7674  -14.7263    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6221   -9.2579    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0616   -7.6693    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2051  -15.5558    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -5.8686    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -3.8274    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.9182   -5.8827    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.8331   -3.1203    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6127  -10.8937    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   19.7566   -9.6037    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7608  -11.3933    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6382  -14.7403    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3308  -11.7234    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3484  -12.2514    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7765   -4.6335    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0646   -5.8638    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7718   -6.2740    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -5.0436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   20.8535   -9.7981    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6382  -15.5606    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6759   -2.3850    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   21.4972  -10.3869    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   20.1602  -10.2467    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.7780   -8.0794    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   19.6978   -8.6509    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   18.9238   -9.9316    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   21.9578   -2.4038    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9642   -1.9795    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.6791   -1.9890    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3908   -2.3944    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2475   -2.3943    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.1074   -1.9843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.8192   -2.3944    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.5358   -1.9843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.3494   -1.8051    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.6791   -1.1476    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   43.7719   -9.0566    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.6395   -7.7023    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.5932   -2.4085    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   29.9388   -2.3943    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   26.3465   -8.1125    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.3440   -3.1108    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   32.0599   -9.2673    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4934   -9.2909    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.9983   -8.6168    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.1737   -3.1108    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0599   -8.0983    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   32.7765   -7.6834    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -8.1031    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4915   -7.6928    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2146  -13.0858    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.3481   -7.6882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4981  -11.0350    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0632   -7.6976    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7909  -10.6155    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2079   -8.1077    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4886  -13.4911    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.1878   -1.6919    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   27.0443  -10.1583    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.3582   -1.6919    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   29.9292   -4.6429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7909   -9.9273    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   35.6331   -7.6740    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4886  -14.3161    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   29.9245   -7.6882    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   34.9214   -8.0888    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2173   -5.0530    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -8.0936    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7749   -8.1125    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -5.0483    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9264  -13.5053    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4981  -11.8507    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9292   -3.8226    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9245   -6.2835    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2051  -14.7309    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   28.5009   -4.6429    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3386   -9.6728    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9264  -14.3255    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   34.2049   -7.6788    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   25.6395   -6.2929    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   27.0443   -9.3333    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2146  -12.2608    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   34.1954  -10.4835    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7749   -8.9280    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.4228   -2.4085    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3529   -4.6335    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6190  -10.4835    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.1042   -2.3943    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.3559   -5.8827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3720  -10.1064    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3386  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7670  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3449   -9.2579    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3449   -8.0841    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   29.2173   -5.8733    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7890   -5.0577    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -5.8780    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0457  -10.8984    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7863  -12.2560    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6190   -9.6632    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7670   -9.6774    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -4.6476    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.3559   -5.0625    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7608  -10.5731    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.3924   -1.9747    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0696  -11.0209    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0415   -8.5131    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3481   -6.8632    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0632   -6.8773    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2079   -8.9280    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3308  -10.8984    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6331   -6.8538    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0696  -11.8411    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -8.9137    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   34.2049   -6.2787    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4821   -9.3380    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -6.2929    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0567   -9.6681    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.8331   -1.6966    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.4474  -10.9172    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7890   -5.8827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0646   -5.0436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.3924   -1.1499    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4821  -10.1630    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0567  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7721  -13.0763    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.3446   -9.7845    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9481   -0.9661    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.6073   -0.9801    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7674  -14.7263    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6221   -9.2579    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0616   -7.6693    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2051  -15.5558    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -5.8686    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -3.8274    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.9182   -5.8827    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.8331   -3.1203    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6127  -10.8937    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   19.7566   -9.6037    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7608  -11.3933    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6382  -14.7403    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3308  -11.7234    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3484  -12.2514    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7765   -4.6335    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0646   -5.8638    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7718   -6.2740    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -5.0436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   20.8535   -9.7981    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6382  -15.5606    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6759   -2.3850    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   21.4972  -10.3869    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   20.1602  -10.2467    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.7780   -8.0794    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   19.6978   -8.6509    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   18.9238   -9.9316    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   21.9578   -2.4038    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9642   -1.9795    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.6791   -1.9890    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3908   -2.3944    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2475   -2.3943    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.1074   -1.9843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.8192   -2.3944    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.5358   -1.9843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.3494   -1.8051    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.6791   -1.1476    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.6395   -7.7023    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.5932   -2.4085    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   29.9388   -2.3943    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   26.3465   -8.1125    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.3440   -3.1108    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   32.0599   -9.2673    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4934   -9.2909    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.9983   -8.6168    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.1737   -3.1108    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0599   -8.0983    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   32.7765   -7.6834    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -8.1031    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4915   -7.6928    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2146  -13.0858    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.3481   -7.6882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4981  -11.0350    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0632   -7.6976    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7909  -10.6155    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2079   -8.1077    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4886  -13.4911    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.1878   -1.6919    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   27.0443  -10.1583    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.3582   -1.6919    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   29.9292   -4.6429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7909   -9.9273    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   35.6331   -7.6740    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4886  -14.3161    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   29.9245   -7.6882    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   34.9214   -8.0888    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2173   -5.0530    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -8.0936    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7749   -8.1125    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -5.0483    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9264  -13.5053    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4981  -11.8507    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9292   -3.8226    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9245   -6.2835    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2051  -14.7309    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   28.5009   -4.6429    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3386   -9.6728    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9264  -14.3255    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   34.2049   -7.6788    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   25.6395   -6.2929    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   27.0443   -9.3333    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2146  -12.2608    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   34.1954  -10.4835    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7749   -8.9280    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.4228   -2.4085    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3529   -4.6335    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6190  -10.4835    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.1042   -2.3943    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.3559   -5.8827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3720  -10.1064    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3386  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7670  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3449   -9.2579    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3449   -8.0841    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   29.2173   -5.8733    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7890   -5.0577    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -5.8780    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0457  -10.8984    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7863  -12.2560    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6190   -9.6632    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7670   -9.6774    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -4.6476    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.3559   -5.0625    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7608  -10.5731    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.3924   -1.9747    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0696  -11.0209    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0415   -8.5131    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3481   -6.8632    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0632   -6.8773    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2079   -8.9280    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3308  -10.8984    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6331   -6.8538    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0696  -11.8411    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -8.9137    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   34.2049   -6.2787    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4821   -9.3380    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -6.2929    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0567   -9.6681    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.8331   -1.6966    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.4474  -10.9172    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7890   -5.8827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0646   -5.0436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.3924   -1.1499    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4821  -10.1630    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0567  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7721  -13.0763    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.3446   -9.7845    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9481   -0.9661    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.6073   -0.9801    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7674  -14.7263    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6221   -9.2579    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0616   -7.6693    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2051  -15.5558    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -5.8686    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -3.8274    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.9182   -5.8827    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.8331   -3.1203    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6127  -10.8937    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   19.7566   -9.6037    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7608  -11.3933    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6382  -14.7403    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3308  -11.7234    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3484  -12.2514    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7765   -4.6335    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0646   -5.8638    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7718   -6.2740    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -5.0436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   20.8535   -9.7981    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6382  -15.5606    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6759   -2.3850    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   21.4972  -10.3869    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   20.1602  -10.2467    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.7780   -8.0794    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   19.6978   -8.6509    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   18.9238   -9.9316    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   21.9578   -2.4038    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9642   -1.9795    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.6791   -1.9890    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3908   -2.3944    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2475   -2.3943    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.1074   -1.9843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.8192   -2.3944    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.5358   -1.9843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.3494   -1.8051    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.6791   -1.1476    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.6395   -7.7023    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.5932   -2.4085    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   29.9388   -2.3943    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   26.3465   -8.1125    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.3440   -3.1108    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   32.0599   -9.2673    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4934   -9.2909    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.9983   -8.6168    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.1737   -3.1108    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0599   -8.0983    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   32.7765   -7.6834    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -8.1031    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4915   -7.6928    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2146  -13.0858    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.3481   -7.6882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4981  -11.0350    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0632   -7.6976    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7909  -10.6155    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2079   -8.1077    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4886  -13.4911    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.1878   -1.6919    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   27.0443  -10.1583    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.3582   -1.6919    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   29.9292   -4.6429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7909   -9.9273    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   35.6331   -7.6740    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4886  -14.3161    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   29.9245   -7.6882    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   34.9214   -8.0888    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2173   -5.0530    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -8.0936    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7749   -8.1125    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -5.0483    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9264  -13.5053    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4981  -11.8507    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9292   -3.8226    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9245   -6.2835    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2051  -14.7309    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   28.5009   -4.6429    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3386   -9.6728    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9264  -14.3255    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   34.2049   -7.6788    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   25.6395   -6.2929    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   27.0443   -9.3333    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2146  -12.2608    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   34.1954  -10.4835    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7749   -8.9280    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.4228   -2.4085    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3529   -4.6335    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6190  -10.4835    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.1042   -2.3943    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.3559   -5.8827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3720  -10.1064    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3386  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7670  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3449   -9.2579    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3449   -8.0841    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   29.2173   -5.8733    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7890   -5.0577    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -5.8780    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0457  -10.8984    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7863  -12.2560    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6190   -9.6632    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7670   -9.6774    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -4.6476    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.3559   -5.0625    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7608  -10.5731    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.3924   -1.9747    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0696  -11.0209    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0415   -8.5131    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3481   -6.8632    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0632   -6.8773    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2079   -8.9280    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3308  -10.8984    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6331   -6.8538    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0696  -11.8411    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -8.9137    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   34.2049   -6.2787    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4821   -9.3380    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -6.2929    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0567   -9.6681    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.8331   -1.6966    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.4474  -10.9172    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7890   -5.8827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0646   -5.0436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.3924   -1.1499    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4821  -10.1630    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0567  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7721  -13.0763    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.3446   -9.7845    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9481   -0.9661    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.6073   -0.9801    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7674  -14.7263    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6221   -9.2579    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0616   -7.6693    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2051  -15.5558    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -5.8686    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -3.8274    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.9182   -5.8827    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.8331   -3.1203    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6127  -10.8937    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   19.7566   -9.6037    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7608  -11.3933    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6382  -14.7403    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3308  -11.7234    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3484  -12.2514    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7765   -4.6335    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0646   -5.8638    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7718   -6.2740    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -5.0436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   20.8535   -9.7981    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6382  -15.5606    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6759   -2.3850    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   21.4972  -10.3869    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   20.1602  -10.2467    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.7780   -8.0794    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   19.6978   -8.6509    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   18.9238   -9.9316    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   21.9578   -2.4038    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9642   -1.9795    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.6791   -1.9890    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3908   -2.3944    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2475   -2.3943    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.1074   -1.9843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.8192   -2.3944    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.5358   -1.9843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.3494   -1.8051    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.6791   -1.1476    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.6395   -7.7023    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.5932   -2.4085    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   29.9388   -2.3943    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   26.3465   -8.1125    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.3440   -3.1108    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   32.0599   -9.2673    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4934   -9.2909    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.9983   -8.6168    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.1737   -3.1108    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0599   -8.0983    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   32.7765   -7.6834    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -8.1031    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4915   -7.6928    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2146  -13.0858    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.3481   -7.6882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4981  -11.0350    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0632   -7.6976    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7909  -10.6155    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2079   -8.1077    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4886  -13.4911    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   31.1878   -1.6919    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   27.0443  -10.1583    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.3582   -1.6919    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   29.9292   -4.6429    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7909   -9.9273    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   35.6331   -7.6740    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4886  -14.3161    0.0000 C   0  0  2  0  0  0  0  0  0  0  0  0\n" +
			"   29.9245   -7.6882    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   34.9214   -8.0888    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2173   -5.0530    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -8.0936    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7749   -8.1125    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -5.0483    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9264  -13.5053    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.4981  -11.8507    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9292   -3.8226    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9245   -6.2835    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2051  -14.7309    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   28.5009   -4.6429    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3386   -9.6728    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9264  -14.3255    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   34.2049   -7.6788    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   25.6395   -6.2929    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   27.0443   -9.3333    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2146  -12.2608    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   34.1954  -10.4835    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7749   -8.9280    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.4228   -2.4085    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3529   -4.6335    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6190  -10.4835    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.1042   -2.3943    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.3559   -5.8827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3720  -10.1064    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3386  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7670  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3449   -9.2579    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3449   -8.0841    0.0000 C   0  0  1  0  0  0  0  0  0  0  0  0\n" +
			"   29.2173   -5.8733    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7890   -5.0577    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6363   -5.8780    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0457  -10.8984    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7863  -12.2560    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6190   -9.6632    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7670   -9.6774    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -4.6476    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.3559   -5.0625    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7608  -10.5731    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.3924   -1.9747    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0696  -11.0209    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0415   -8.5131    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.3481   -6.8632    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0632   -6.8773    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.2079   -8.9280    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3308  -10.8984    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   35.6331   -6.8538    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.0696  -11.8411    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -8.9137    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   34.2049   -6.2787    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4821   -9.3380    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -6.2929    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0567   -9.6681    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.8331   -1.6966    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.4474  -10.9172    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7890   -5.8827    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0646   -5.0436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.3924   -1.1499    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   28.4821  -10.1630    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0567  -10.4882    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7721  -13.0763    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.3446   -9.7845    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.9481   -0.9661    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   31.6073   -0.9801    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.7674  -14.7263    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6221   -9.2579    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.0616   -7.6693    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2051  -15.5558    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -5.8686    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.0725   -3.8274    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.9182   -5.8827    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.8331   -3.1203    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   30.6127  -10.8937    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   19.7566   -9.6037    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.7608  -11.3933    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6382  -14.7403    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   36.3308  -11.7234    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3484  -12.2514    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7765   -4.6335    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.0646   -5.8638    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   32.7718   -6.2740    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   33.4930   -5.0436    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   20.8535   -9.7981    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6382  -15.5606    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   27.6759   -2.3850    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   21.4972  -10.3869    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   20.1602  -10.2467    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   37.7780   -8.0794    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   19.6978   -8.6509    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   18.9238   -9.9316    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   21.9578   -2.4038    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.9642   -1.9795    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.6791   -1.9890    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   23.3908   -2.3944    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   26.2475   -2.3943    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.1074   -1.9843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   24.8192   -2.3944    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   25.5358   -1.9843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   29.3494   -1.8051    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   22.6791   -1.1476    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   43.7719   -9.0566    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   43.7719   -9.0566    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   43.7719   -9.0566    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   43.7719   -9.0566    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   43.7719   -9.0566    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   43.7719   -9.0566    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"   43.7719   -9.0566    0.0000 Cl  0  0  0  0  0  0  0  0  0  0  0  0\n" +
			"  2  9  1  0  0  0  0\n" +
			"  3  5  1  0  0  0  0\n" +
			"  1  4  1  6  0  0  0\n" +
			"  5 36  1  1  0  0  0\n" +
			"  6 10  1  0  0  0  0\n" +
			"  7  8  1  0  0  0  0\n" +
			"  8  1  1  0  0  0  0\n" +
			"  9  5  1  0  0  0  0\n" +
			" 10 15  1  0  0  0  0\n" +
			" 10 11  1  1  0  0  0\n" +
			" 28 12  1  1  0  0  0\n" +
			" 13 32  1  0  0  0  0\n" +
			" 14 45  1  1  0  0  0\n" +
			" 15 12  1  0  0  0  0\n" +
			" 16 18  2  0  0  0  0\n" +
			" 17  4  1  0  0  0  0\n" +
			" 18 25  1  0  0  0  0\n" +
			" 19 13  1  0  0  0  0\n" +
			" 20 14  1  0  0  0  0\n" +
			" 21 23  1  0  0  0  0\n" +
			" 22 16  1  0  0  0  0\n" +
			" 23  3  1  0  0  0  0\n" +
			" 24 30  1  0  0  0  0\n" +
			" 25  7  1  0  0  0  0\n" +
			" 26 29  1  0  0  0  0\n" +
			" 27 20  1  0  0  0  0\n" +
			" 28 19  1  0  0  0  0\n" +
			" 42 29  1  1  0  0  0\n" +
			" 30 39  1  0  0  0  0\n" +
			" 31 11  1  0  0  0  0\n" +
			" 32 17  1  6  0  0  0\n" +
			" 33 60  1  0  0  0  0\n" +
			" 34 14  1  0  0  0  0\n" +
			" 35 16  1  0  0  0  0\n" +
			" 36 24  1  0  0  0  0\n" +
			" 37 28  1  0  0  0  0\n" +
			" 38 27  1  0  0  0  0\n" +
			" 39 59  1  0  0  0  0\n" +
			" 40  6  2  0  0  0  0\n" +
			" 41 34  1  0  0  0  0\n" +
			" 42 31  1  0  0  0  0\n" +
			" 43  1  1  0  0  0  0\n" +
			" 44 47  2  0  0  0  0\n" +
			" 45 35  1  0  0  0  0\n" +
			" 46 55  1  0  0  0  0\n" +
			" 47 32  1  0  0  0  0\n" +
			"  2 48  1  1  0  0  0\n" +
			" 49 33  1  0  0  0  0\n" +
			" 50 46  1  0  0  0  0\n" +
			" 51  3  1  0  0  0  0\n" +
			" 52 43  1  0  0  0  0\n" +
			" 25 53  1  1  0  0  0\n" +
			" 54 40  1  0  0  0  0\n" +
			" 55 64  2  0  0  0  0\n" +
			" 56 57  1  0  0  0  0\n" +
			" 57 26  1  0  0  0  0\n" +
			" 58 37  1  0  0  0  0\n" +
			" 59 84  1  0  0  0  0\n" +
			" 60 37  2  0  0  0  0\n" +
			" 61 55  1  0  0  0  0\n" +
			" 62 76  1  0  0  0  0\n" +
			" 63 50  1  0  0  0  0\n" +
			" 64  6  1  0  0  0  0\n" +
			" 65 66  1  0  0  0  0\n" +
			" 66 52  2  0  0  0  0\n" +
			" 67 22  2  0  0  0  0\n" +
			" 68 51  1  0  0  0  0\n" +
			" 69 18  1  0  0  0  0\n" +
			" 70  8  2  0  0  0  0\n" +
			" 71 15  2  0  0  0  0\n" +
			" 72 17  2  0  0  0  0\n" +
			" 73 19  2  0  0  0  0\n" +
			" 74 50  2  0  0  0  0\n" +
			" 75 26  2  0  0  0  0\n" +
			" 76 69  2  0  0  0  0\n" +
			" 77 31  2  0  0  0  0\n" +
			" 78 42  1  0  0  0  0\n" +
			" 79 47  1  0  0  0  0\n" +
			" 80 52  1  0  0  0  0\n" +
			" 81 56  1  0  0  0  0\n" +
			" 82 48  2  0  0  0  0\n" +
			" 83 53  2  0  0  0  0\n" +
			" 84 80  2  0  0  0  0\n" +
			" 85 49  1  0  0  0  0\n" +
			" 86 68  2  0  0  0  0\n" +
			" 87 79  2  0  0  0  0\n" +
			" 88 74  1  0  0  0  0\n" +
			" 20 89  1  6  0  0  0\n" +
			" 90 53  1  0  0  0  0\n" +
			" 23 91  1  1  0  0  0\n" +
			" 21 92  1  6  0  0  0\n" +
			" 27 93  1  6  0  0  0\n" +
			" 94 40  1  0  0  0  0\n" +
			" 57 95  1  6  0  0  0\n" +
			" 38 96  1  1  0  0  0\n" +
			" 97 78  1  0  0  0  0\n" +
			" 98 65  1  0  0  0  0\n" +
			" 43 99  1  6  0  0  0\n" +
			"100 48  1  0  0  0  0\n" +
			"101 54  1  0  0  0  0\n" +
			"102115  1  0  0  0  0\n" +
			"103 67  1  0  0  0  0\n" +
			" 41104  1  6  0  0  0\n" +
			"105 74  1  0  0  0  0\n" +
			"106 76  1  0  0  0  0\n" +
			"107 85  1  0  0  0  0\n" +
			"108 85  2  0  0  0  0\n" +
			"109108  1  0  0  0  0\n" +
			"110107  2  0  0  0  0\n" +
			"111114  1  0  0  0  0\n" +
			"112104  1  0  0  0  0\n" +
			"113 68  1  0  0  0  0\n" +
			"114 90  1  0  0  0  0\n" +
			"115111  1  0  0  0  0\n" +
			"116 95  1  0  0  0  0\n" +
			"117102  1  0  0  0  0\n" +
			"118102  1  0  0  0  0\n" +
			"119121  1  0  0  0  0\n" +
			"120113  1  0  0  0  0\n" +
			"121122  1  0  0  0  0\n" +
			"122124  1  0  0  0  0\n" +
			"123120  1  0  0  0  0\n" +
			"124125  1  0  0  0  0\n" +
			"125126  1  0  0  0  0\n" +
			"126123  1  0  0  0  0\n" +
			"  3127  1  1  0  0  0\n" +
			" 65 59  2  0  0  0  0\n" +
			" 22 44  1  0  0  0  0\n" +
			" 87 67  1  0  0  0  0\n" +
			" 35 62  2  0  0  0  0\n" +
			" 58 30  2  0  0  0  0\n" +
			" 24 33  2  0  0  0  0\n" +
			" 41 38  1  0  0  0  0\n" +
			" 61 54  2  0  0  0  0\n" +
			"110 97  1  0  0  0  0\n" +
			" 97109  2  0  0  0  0\n" +
			"  2 21  1  0  0  0  0\n" +
			" 63 56  2  0  0  0  0\n" +
			" 81 88  2  0  0  0  0\n" +
			"121128  1  0  0  0  0\n" +
			"514517  1  6  0  0  0\n" +
			"521514  1  0  0  0  0\n" +
			"556514  1  0  0  0  0\n" +
			"515522  1  0  0  0  0\n" +
			"515561  1  1  0  0  0\n" +
			"515534  1  0  0  0  0\n" +
			"516518  1  0  0  0  0\n" +
			"536516  1  0  0  0  0\n" +
			"564516  1  0  0  0  0\n" +
			"516640  1  1  0  0  0\n" +
			"530517  1  0  0  0  0\n" +
			"518549  1  1  0  0  0\n" +
			"522518  1  0  0  0  0\n" +
			"519523  1  0  0  0  0\n" +
			"553519  2  0  0  0  0\n" +
			"577519  1  0  0  0  0\n" +
			"520521  1  0  0  0  0\n" +
			"538520  1  0  0  0  0\n" +
			"583521  2  0  0  0  0\n" +
			"523528  1  0  0  0  0\n" +
			"523524  1  1  0  0  0\n" +
			"544524  1  0  0  0  0\n" +
			"541525  1  1  0  0  0\n" +
			"528525  1  0  0  0  0\n" +
			"526545  1  0  0  0  0\n" +
			"532526  1  0  0  0  0\n" +
			"527558  1  1  0  0  0\n" +
			"533527  1  0  0  0  0\n" +
			"547527  1  0  0  0  0\n" +
			"584528  2  0  0  0  0\n" +
			"529531  2  0  0  0  0\n" +
			"535529  1  0  0  0  0\n" +
			"548529  1  0  0  0  0\n" +
			"545530  1  6  0  0  0\n" +
			"585530  2  0  0  0  0\n" +
			"531538  1  0  0  0  0\n" +
			"582531  1  0  0  0  0\n" +
			"541532  1  0  0  0  0\n" +
			"586532  2  0  0  0  0\n" +
			"540533  1  0  0  0  0\n" +
			"533602  1  6  0  0  0\n" +
			"534536  1  0  0  0  0\n" +
			"534605  1  6  0  0  0\n" +
			"580535  2  0  0  0  0\n" +
			"535557  1  0  0  0  0\n" +
			"536604  1  1  0  0  0\n" +
			"537543  1  0  0  0  0\n" +
			"549537  1  0  0  0  0\n" +
			"537546  2  0  0  0  0\n" +
			"538566  1  1  0  0  0\n" +
			"539542  1  0  0  0  0\n" +
			"570539  1  0  0  0  0\n" +
			"588539  2  0  0  0  0\n" +
			"551540  1  0  0  0  0\n" +
			"540606  1  6  0  0  0\n" +
			"550541  1  0  0  0  0\n" +
			"555542  1  1  0  0  0\n" +
			"543552  1  0  0  0  0\n" +
			"571543  2  0  0  0  0\n" +
			"555544  1  0  0  0  0\n" +
			"590544  2  0  0  0  0\n" +
			"560545  1  0  0  0  0\n" +
			"546573  1  0  0  0  0\n" +
			"562546  1  0  0  0  0\n" +
			"554547  1  0  0  0  0\n" +
			"558548  1  0  0  0  0\n" +
			"548575  2  0  0  0  0\n" +
			"571550  1  0  0  0  0\n" +
			"573550  2  0  0  0  0\n" +
			"551609  1  1  0  0  0\n" +
			"554551  1  0  0  0  0\n" +
			"552572  1  0  0  0  0\n" +
			"567553  1  0  0  0  0\n" +
			"607553  1  0  0  0  0\n" +
			"554617  1  6  0  0  0\n" +
			"591555  1  0  0  0  0\n" +
			"565556  1  0  0  0  0\n" +
			"556612  1  6  0  0  0\n" +
			"557560  2  0  0  0  0\n" +
			"559568  1  0  0  0  0\n" +
			"563559  1  0  0  0  0\n" +
			"592560  1  0  0  0  0\n" +
			"595561  2  0  0  0  0\n" +
			"613561  1  0  0  0  0\n" +
			"598562  1  0  0  0  0\n" +
			"576563  1  0  0  0  0\n" +
			"587563  2  0  0  0  0\n" +
			"581564  1  0  0  0  0\n" +
			"579565  2  0  0  0  0\n" +
			"593565  1  0  0  0  0\n" +
			"596566  2  0  0  0  0\n" +
			"603566  1  0  0  0  0\n" +
			"614567  1  0  0  0  0\n" +
			"574567  2  0  0  0  0\n" +
			"568577  2  0  0  0  0\n" +
			"574568  1  0  0  0  0\n" +
			"569570  1  0  0  0  0\n" +
			"594569  1  0  0  0  0\n" +
			"576569  2  0  0  0  0\n" +
			"570608  1  6  0  0  0\n" +
			"572597  1  0  0  0  0\n" +
			"578572  2  0  0  0  0\n" +
			"575589  1  0  0  0  0\n" +
			"578579  1  0  0  0  0\n" +
			"611578  1  0  0  0  0\n" +
			"616580  1  0  0  0  0\n" +
			"600580  1  0  0  0  0\n" +
			"599581  2  0  0  0  0\n" +
			"626581  1  0  0  0  0\n" +
			"589582  2  0  0  0  0\n" +
			"601587  1  0  0  0  0\n" +
			"618587  1  0  0  0  0\n" +
			"619589  1  0  0  0  0\n" +
			"610591  1  0  0  0  0\n" +
			"600592  2  0  0  0  0\n" +
			"597593  2  0  0  0  0\n" +
			"594601  2  0  0  0  0\n" +
			"620598  1  0  0  0  0\n" +
			"621598  2  0  0  0  0\n" +
			"627603  1  0  0  0  0\n" +
			"629608  1  0  0  0  0\n" +
			"623610  1  0  0  0  0\n" +
			"610622  2  0  0  0  0\n" +
			"615628  1  0  0  0  0\n" +
			"630615  1  0  0  0  0\n" +
			"631615  1  0  0  0  0\n" +
			"625617  1  0  0  0  0\n" +
			"623620  2  0  0  0  0\n" +
			"622621  1  0  0  0  0\n" +
			"624627  1  0  0  0  0\n" +
			"628624  1  0  0  0  0\n" +
			"633626  1  0  0  0  0\n" +
			"632634  1  0  0  0  0\n" +
			"636633  1  0  0  0  0\n" +
			"634635  1  0  0  0  0\n" +
			"634641  1  0  0  0  0\n" +
			"635637  1  0  0  0  0\n" +
			"639636  1  0  0  0  0\n" +
			"637638  1  0  0  0  0\n" +
			"638639  1  0  0  0  0\n" +
			"386389  1  6  0  0  0\n" +
			"393386  1  0  0  0  0\n" +
			"428386  1  0  0  0  0\n" +
			"387394  1  0  0  0  0\n" +
			"387433  1  1  0  0  0\n" +
			"387406  1  0  0  0  0\n" +
			"388390  1  0  0  0  0\n" +
			"408388  1  0  0  0  0\n" +
			"436388  1  0  0  0  0\n" +
			"388512  1  1  0  0  0\n" +
			"402389  1  0  0  0  0\n" +
			"390421  1  1  0  0  0\n" +
			"394390  1  0  0  0  0\n" +
			"391395  1  0  0  0  0\n" +
			"425391  2  0  0  0  0\n" +
			"449391  1  0  0  0  0\n" +
			"392393  1  0  0  0  0\n" +
			"410392  1  0  0  0  0\n" +
			"455393  2  0  0  0  0\n" +
			"395400  1  0  0  0  0\n" +
			"395396  1  1  0  0  0\n" +
			"416396  1  0  0  0  0\n" +
			"413397  1  1  0  0  0\n" +
			"400397  1  0  0  0  0\n" +
			"398417  1  0  0  0  0\n" +
			"404398  1  0  0  0  0\n" +
			"399430  1  1  0  0  0\n" +
			"405399  1  0  0  0  0\n" +
			"419399  1  0  0  0  0\n" +
			"456400  2  0  0  0  0\n" +
			"401403  2  0  0  0  0\n" +
			"407401  1  0  0  0  0\n" +
			"420401  1  0  0  0  0\n" +
			"417402  1  6  0  0  0\n" +
			"457402  2  0  0  0  0\n" +
			"403410  1  0  0  0  0\n" +
			"454403  1  0  0  0  0\n" +
			"413404  1  0  0  0  0\n" +
			"458404  2  0  0  0  0\n" +
			"412405  1  0  0  0  0\n" +
			"405474  1  6  0  0  0\n" +
			"406408  1  0  0  0  0\n" +
			"406477  1  6  0  0  0\n" +
			"452407  2  0  0  0  0\n" +
			"407429  1  0  0  0  0\n" +
			"408476  1  1  0  0  0\n" +
			"409415  1  0  0  0  0\n" +
			"421409  1  0  0  0  0\n" +
			"409418  2  0  0  0  0\n" +
			"410438  1  1  0  0  0\n" +
			"411414  1  0  0  0  0\n" +
			"442411  1  0  0  0  0\n" +
			"460411  2  0  0  0  0\n" +
			"423412  1  0  0  0  0\n" +
			"412478  1  6  0  0  0\n" +
			"422413  1  0  0  0  0\n" +
			"427414  1  1  0  0  0\n" +
			"415424  1  0  0  0  0\n" +
			"443415  2  0  0  0  0\n" +
			"427416  1  0  0  0  0\n" +
			"462416  2  0  0  0  0\n" +
			"432417  1  0  0  0  0\n" +
			"418445  1  0  0  0  0\n" +
			"434418  1  0  0  0  0\n" +
			"426419  1  0  0  0  0\n" +
			"430420  1  0  0  0  0\n" +
			"420447  2  0  0  0  0\n" +
			"443422  1  0  0  0  0\n" +
			"445422  2  0  0  0  0\n" +
			"423481  1  1  0  0  0\n" +
			"426423  1  0  0  0  0\n" +
			"424444  1  0  0  0  0\n" +
			"439425  1  0  0  0  0\n" +
			"479425  1  0  0  0  0\n" +
			"426489  1  6  0  0  0\n" +
			"463427  1  0  0  0  0\n" +
			"437428  1  0  0  0  0\n" +
			"428484  1  6  0  0  0\n" +
			"429432  2  0  0  0  0\n" +
			"431440  1  0  0  0  0\n" +
			"435431  1  0  0  0  0\n" +
			"464432  1  0  0  0  0\n" +
			"467433  2  0  0  0  0\n" +
			"485433  1  0  0  0  0\n" +
			"470434  1  0  0  0  0\n" +
			"448435  1  0  0  0  0\n" +
			"459435  2  0  0  0  0\n" +
			"453436  1  0  0  0  0\n" +
			"451437  2  0  0  0  0\n" +
			"465437  1  0  0  0  0\n" +
			"468438  2  0  0  0  0\n" +
			"475438  1  0  0  0  0\n" +
			"486439  1  0  0  0  0\n" +
			"446439  2  0  0  0  0\n" +
			"440449  2  0  0  0  0\n" +
			"446440  1  0  0  0  0\n" +
			"441442  1  0  0  0  0\n" +
			"466441  1  0  0  0  0\n" +
			"448441  2  0  0  0  0\n" +
			"442480  1  6  0  0  0\n" +
			"444469  1  0  0  0  0\n" +
			"450444  2  0  0  0  0\n" +
			"447461  1  0  0  0  0\n" +
			"450451  1  0  0  0  0\n" +
			"483450  1  0  0  0  0\n" +
			"488452  1  0  0  0  0\n" +
			"472452  1  0  0  0  0\n" +
			"471453  2  0  0  0  0\n" +
			"498453  1  0  0  0  0\n" +
			"461454  2  0  0  0  0\n" +
			"473459  1  0  0  0  0\n" +
			"490459  1  0  0  0  0\n" +
			"491461  1  0  0  0  0\n" +
			"482463  1  0  0  0  0\n" +
			"472464  2  0  0  0  0\n" +
			"469465  2  0  0  0  0\n" +
			"466473  2  0  0  0  0\n" +
			"492470  1  0  0  0  0\n" +
			"493470  2  0  0  0  0\n" +
			"499475  1  0  0  0  0\n" +
			"501480  1  0  0  0  0\n" +
			"495482  1  0  0  0  0\n" +
			"482494  2  0  0  0  0\n" +
			"487500  1  0  0  0  0\n" +
			"502487  1  0  0  0  0\n" +
			"503487  1  0  0  0  0\n" +
			"497489  1  0  0  0  0\n" +
			"495492  2  0  0  0  0\n" +
			"494493  1  0  0  0  0\n" +
			"496499  1  0  0  0  0\n" +
			"500496  1  0  0  0  0\n" +
			"505498  1  0  0  0  0\n" +
			"504506  1  0  0  0  0\n" +
			"508505  1  0  0  0  0\n" +
			"506507  1  0  0  0  0\n" +
			"506513  1  0  0  0  0\n" +
			"507509  1  0  0  0  0\n" +
			"511508  1  0  0  0  0\n" +
			"509510  1  0  0  0  0\n" +
			"510511  1  0  0  0  0\n" +
			"258261  1  6  0  0  0\n" +
			"265258  1  0  0  0  0\n" +
			"300258  1  0  0  0  0\n" +
			"259266  1  0  0  0  0\n" +
			"259305  1  1  0  0  0\n" +
			"259278  1  0  0  0  0\n" +
			"260262  1  0  0  0  0\n" +
			"280260  1  0  0  0  0\n" +
			"308260  1  0  0  0  0\n" +
			"260384  1  1  0  0  0\n" +
			"274261  1  0  0  0  0\n" +
			"262293  1  1  0  0  0\n" +
			"266262  1  0  0  0  0\n" +
			"263267  1  0  0  0  0\n" +
			"297263  2  0  0  0  0\n" +
			"321263  1  0  0  0  0\n" +
			"264265  1  0  0  0  0\n" +
			"282264  1  0  0  0  0\n" +
			"327265  2  0  0  0  0\n" +
			"267272  1  0  0  0  0\n" +
			"267268  1  1  0  0  0\n" +
			"288268  1  0  0  0  0\n" +
			"285269  1  1  0  0  0\n" +
			"272269  1  0  0  0  0\n" +
			"270289  1  0  0  0  0\n" +
			"276270  1  0  0  0  0\n" +
			"271302  1  1  0  0  0\n" +
			"277271  1  0  0  0  0\n" +
			"291271  1  0  0  0  0\n" +
			"328272  2  0  0  0  0\n" +
			"273275  2  0  0  0  0\n" +
			"279273  1  0  0  0  0\n" +
			"292273  1  0  0  0  0\n" +
			"289274  1  6  0  0  0\n" +
			"329274  2  0  0  0  0\n" +
			"275282  1  0  0  0  0\n" +
			"326275  1  0  0  0  0\n" +
			"285276  1  0  0  0  0\n" +
			"330276  2  0  0  0  0\n" +
			"284277  1  0  0  0  0\n" +
			"277346  1  6  0  0  0\n" +
			"278280  1  0  0  0  0\n" +
			"278349  1  6  0  0  0\n" +
			"324279  2  0  0  0  0\n" +
			"279301  1  0  0  0  0\n" +
			"280348  1  1  0  0  0\n" +
			"281287  1  0  0  0  0\n" +
			"293281  1  0  0  0  0\n" +
			"281290  2  0  0  0  0\n" +
			"282310  1  1  0  0  0\n" +
			"283286  1  0  0  0  0\n" +
			"314283  1  0  0  0  0\n" +
			"332283  2  0  0  0  0\n" +
			"295284  1  0  0  0  0\n" +
			"284350  1  6  0  0  0\n" +
			"294285  1  0  0  0  0\n" +
			"299286  1  1  0  0  0\n" +
			"287296  1  0  0  0  0\n" +
			"315287  2  0  0  0  0\n" +
			"299288  1  0  0  0  0\n" +
			"334288  2  0  0  0  0\n" +
			"304289  1  0  0  0  0\n" +
			"290317  1  0  0  0  0\n" +
			"306290  1  0  0  0  0\n" +
			"298291  1  0  0  0  0\n" +
			"302292  1  0  0  0  0\n" +
			"292319  2  0  0  0  0\n" +
			"315294  1  0  0  0  0\n" +
			"317294  2  0  0  0  0\n" +
			"295353  1  1  0  0  0\n" +
			"298295  1  0  0  0  0\n" +
			"296316  1  0  0  0  0\n" +
			"311297  1  0  0  0  0\n" +
			"351297  1  0  0  0  0\n" +
			"298361  1  6  0  0  0\n" +
			"335299  1  0  0  0  0\n" +
			"309300  1  0  0  0  0\n" +
			"300356  1  6  0  0  0\n" +
			"301304  2  0  0  0  0\n" +
			"303312  1  0  0  0  0\n" +
			"307303  1  0  0  0  0\n" +
			"336304  1  0  0  0  0\n" +
			"339305  2  0  0  0  0\n" +
			"357305  1  0  0  0  0\n" +
			"342306  1  0  0  0  0\n" +
			"320307  1  0  0  0  0\n" +
			"331307  2  0  0  0  0\n" +
			"325308  1  0  0  0  0\n" +
			"323309  2  0  0  0  0\n" +
			"337309  1  0  0  0  0\n" +
			"340310  2  0  0  0  0\n" +
			"347310  1  0  0  0  0\n" +
			"358311  1  0  0  0  0\n" +
			"318311  2  0  0  0  0\n" +
			"312321  2  0  0  0  0\n" +
			"318312  1  0  0  0  0\n" +
			"313314  1  0  0  0  0\n" +
			"338313  1  0  0  0  0\n" +
			"320313  2  0  0  0  0\n" +
			"314352  1  6  0  0  0\n" +
			"316341  1  0  0  0  0\n" +
			"322316  2  0  0  0  0\n" +
			"319333  1  0  0  0  0\n" +
			"322323  1  0  0  0  0\n" +
			"355322  1  0  0  0  0\n" +
			"360324  1  0  0  0  0\n" +
			"344324  1  0  0  0  0\n" +
			"343325  2  0  0  0  0\n" +
			"370325  1  0  0  0  0\n" +
			"333326  2  0  0  0  0\n" +
			"345331  1  0  0  0  0\n" +
			"362331  1  0  0  0  0\n" +
			"363333  1  0  0  0  0\n" +
			"354335  1  0  0  0  0\n" +
			"344336  2  0  0  0  0\n" +
			"341337  2  0  0  0  0\n" +
			"338345  2  0  0  0  0\n" +
			"364342  1  0  0  0  0\n" +
			"365342  2  0  0  0  0\n" +
			"371347  1  0  0  0  0\n" +
			"373352  1  0  0  0  0\n" +
			"367354  1  0  0  0  0\n" +
			"354366  2  0  0  0  0\n" +
			"359372  1  0  0  0  0\n" +
			"374359  1  0  0  0  0\n" +
			"375359  1  0  0  0  0\n" +
			"369361  1  0  0  0  0\n" +
			"367364  2  0  0  0  0\n" +
			"366365  1  0  0  0  0\n" +
			"368371  1  0  0  0  0\n" +
			"372368  1  0  0  0  0\n" +
			"377370  1  0  0  0  0\n" +
			"376378  1  0  0  0  0\n" +
			"380377  1  0  0  0  0\n" +
			"378379  1  0  0  0  0\n" +
			"378385  1  0  0  0  0\n" +
			"379381  1  0  0  0  0\n" +
			"383380  1  0  0  0  0\n" +
			"381382  1  0  0  0  0\n" +
			"382383  1  0  0  0  0\n" +
			"130133  1  6  0  0  0\n" +
			"137130  1  0  0  0  0\n" +
			"172130  1  0  0  0  0\n" +
			"131138  1  0  0  0  0\n" +
			"131177  1  1  0  0  0\n" +
			"131150  1  0  0  0  0\n" +
			"132134  1  0  0  0  0\n" +
			"152132  1  0  0  0  0\n" +
			"180132  1  0  0  0  0\n" +
			"132256  1  1  0  0  0\n" +
			"146133  1  0  0  0  0\n" +
			"134165  1  1  0  0  0\n" +
			"138134  1  0  0  0  0\n" +
			"135139  1  0  0  0  0\n" +
			"169135  2  0  0  0  0\n" +
			"193135  1  0  0  0  0\n" +
			"136137  1  0  0  0  0\n" +
			"154136  1  0  0  0  0\n" +
			"199137  2  0  0  0  0\n" +
			"139144  1  0  0  0  0\n" +
			"139140  1  1  0  0  0\n" +
			"160140  1  0  0  0  0\n" +
			"157141  1  1  0  0  0\n" +
			"144141  1  0  0  0  0\n" +
			"142161  1  0  0  0  0\n" +
			"148142  1  0  0  0  0\n" +
			"143174  1  1  0  0  0\n" +
			"149143  1  0  0  0  0\n" +
			"163143  1  0  0  0  0\n" +
			"200144  2  0  0  0  0\n" +
			"145147  2  0  0  0  0\n" +
			"151145  1  0  0  0  0\n" +
			"164145  1  0  0  0  0\n" +
			"161146  1  6  0  0  0\n" +
			"201146  2  0  0  0  0\n" +
			"147154  1  0  0  0  0\n" +
			"198147  1  0  0  0  0\n" +
			"157148  1  0  0  0  0\n" +
			"202148  2  0  0  0  0\n" +
			"156149  1  0  0  0  0\n" +
			"149218  1  6  0  0  0\n" +
			"150152  1  0  0  0  0\n" +
			"150221  1  6  0  0  0\n" +
			"196151  2  0  0  0  0\n" +
			"151173  1  0  0  0  0\n" +
			"152220  1  1  0  0  0\n" +
			"153159  1  0  0  0  0\n" +
			"165153  1  0  0  0  0\n" +
			"153162  2  0  0  0  0\n" +
			"154182  1  1  0  0  0\n" +
			"155158  1  0  0  0  0\n" +
			"186155  1  0  0  0  0\n" +
			"204155  2  0  0  0  0\n" +
			"167156  1  0  0  0  0\n" +
			"156222  1  6  0  0  0\n" +
			"166157  1  0  0  0  0\n" +
			"171158  1  1  0  0  0\n" +
			"159168  1  0  0  0  0\n" +
			"187159  2  0  0  0  0\n" +
			"171160  1  0  0  0  0\n" +
			"206160  2  0  0  0  0\n" +
			"176161  1  0  0  0  0\n" +
			"162189  1  0  0  0  0\n" +
			"178162  1  0  0  0  0\n" +
			"170163  1  0  0  0  0\n" +
			"174164  1  0  0  0  0\n" +
			"164191  2  0  0  0  0\n" +
			"187166  1  0  0  0  0\n" +
			"189166  2  0  0  0  0\n" +
			"167225  1  1  0  0  0\n" +
			"170167  1  0  0  0  0\n" +
			"168188  1  0  0  0  0\n" +
			"183169  1  0  0  0  0\n" +
			"223169  1  0  0  0  0\n" +
			"170233  1  6  0  0  0\n" +
			"207171  1  0  0  0  0\n" +
			"181172  1  0  0  0  0\n" +
			"172228  1  6  0  0  0\n" +
			"173176  2  0  0  0  0\n" +
			"175184  1  0  0  0  0\n" +
			"179175  1  0  0  0  0\n" +
			"208176  1  0  0  0  0\n" +
			"211177  2  0  0  0  0\n" +
			"229177  1  0  0  0  0\n" +
			"214178  1  0  0  0  0\n" +
			"192179  1  0  0  0  0\n" +
			"203179  2  0  0  0  0\n" +
			"197180  1  0  0  0  0\n" +
			"195181  2  0  0  0  0\n" +
			"209181  1  0  0  0  0\n" +
			"212182  2  0  0  0  0\n" +
			"219182  1  0  0  0  0\n" +
			"230183  1  0  0  0  0\n" +
			"190183  2  0  0  0  0\n" +
			"184193  2  0  0  0  0\n" +
			"190184  1  0  0  0  0\n" +
			"185186  1  0  0  0  0\n" +
			"210185  1  0  0  0  0\n" +
			"192185  2  0  0  0  0\n" +
			"186224  1  6  0  0  0\n" +
			"188213  1  0  0  0  0\n" +
			"194188  2  0  0  0  0\n" +
			"191205  1  0  0  0  0\n" +
			"194195  1  0  0  0  0\n" +
			"227194  1  0  0  0  0\n" +
			"232196  1  0  0  0  0\n" +
			"216196  1  0  0  0  0\n" +
			"215197  2  0  0  0  0\n" +
			"242197  1  0  0  0  0\n" +
			"205198  2  0  0  0  0\n" +
			"217203  1  0  0  0  0\n" +
			"234203  1  0  0  0  0\n" +
			"235205  1  0  0  0  0\n" +
			"226207  1  0  0  0  0\n" +
			"216208  2  0  0  0  0\n" +
			"213209  2  0  0  0  0\n" +
			"210217  2  0  0  0  0\n" +
			"236214  1  0  0  0  0\n" +
			"237214  2  0  0  0  0\n" +
			"243219  1  0  0  0  0\n" +
			"245224  1  0  0  0  0\n" +
			"239226  1  0  0  0  0\n" +
			"226238  2  0  0  0  0\n" +
			"231244  1  0  0  0  0\n" +
			"246231  1  0  0  0  0\n" +
			"247231  1  0  0  0  0\n" +
			"241233  1  0  0  0  0\n" +
			"239236  2  0  0  0  0\n" +
			"238237  1  0  0  0  0\n" +
			"240243  1  0  0  0  0\n" +
			"244240  1  0  0  0  0\n" +
			"249242  1  0  0  0  0\n" +
			"248250  1  0  0  0  0\n" +
			"252249  1  0  0  0  0\n" +
			"250251  1  0  0  0  0\n" +
			"250257  1  0  0  0  0\n" +
			"251253  1  0  0  0  0\n" +
			"255252  1  0  0  0  0\n" +
			"253254  1  0  0  0  0\n" +
			"254255  1  0  0  0  0\n" +
			"M  STY  2   1 MUL   2 MUL\n" +
			"M  SAL   1 15   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15\n" +
			"M  SAL   1 15  16  17  18  19  20  21  22  23  24  25  26  27  28  29  30\n" +
			"M  SAL   1 15  31  32  33  34  35  36  37  38  39  40  41  42  43  44  45\n" +
			"M  SAL   1 15  46  47  48  49  50  51  52  53  54  55  56  57  58  59  60\n" +
			"M  SAL   1 15  61  62  63  64  65  66  67  68  69  70  71  72  73  74  75\n" +
			"M  SAL   1 15  76  77  78  79  80  81  82  83  84  85  86  87  88  89  90\n" +
			"M  SAL   1 15  91  92  93  94  95  96  97  98  99 100 101 102 103 104 105\n" +
			"M  SAL   1 15 106 107 108 109 110 111 112 113 114 115 116 117 118 119 120\n" +
			"M  SAL   1 15 121 122 123 124 125 126 127 128 130 131 132 133 134 135 136\n" +
			"M  SAL   1 15 137 138 139 140 141 142 143 144 145 146 147 148 149 150 151\n" +
			"M  SAL   1 15 152 153 154 155 156 157 158 159 160 161 162 163 164 165 166\n" +
			"M  SAL   1 15 167 168 169 170 171 172 173 174 175 176 177 178 179 180 181\n" +
			"M  SAL   1 15 182 183 184 185 186 187 188 189 190 191 192 193 194 195 196\n" +
			"M  SAL   1 15 197 198 199 200 201 202 203 204 205 206 207 208 209 210 211\n" +
			"M  SAL   1 15 212 213 214 215 216 217 218 219 220 221 222 223 224 225 226\n" +
			"M  SAL   1 15 227 228 229 230 231 232 233 234 235 236 237 238 239 240 241\n" +
			"M  SAL   1 15 242 243 244 245 246 247 248 249 250 251 252 253 254 255 256\n" +
			"M  SAL   1 15 257 258 259 260 261 262 263 264 265 266 267 268 269 270 271\n" +
			"M  SAL   1 15 272 273 274 275 276 277 278 279 280 281 282 283 284 285 286\n" +
			"M  SAL   1 15 287 288 289 290 291 292 293 294 295 296 297 298 299 300 301\n" +
			"M  SAL   1 15 302 303 304 305 306 307 308 309 310 311 312 313 314 315 316\n" +
			"M  SAL   1 15 317 318 319 320 321 322 323 324 325 326 327 328 329 330 331\n" +
			"M  SAL   1 15 332 333 334 335 336 337 338 339 340 341 342 343 344 345 346\n" +
			"M  SAL   1 15 347 348 349 350 351 352 353 354 355 356 357 358 359 360 361\n" +
			"M  SAL   1 15 362 363 364 365 366 367 368 369 370 371 372 373 374 375 376\n" +
			"M  SAL   1 15 377 378 379 380 381 382 383 384 385 386 387 388 389 390 391\n" +
			"M  SAL   1 15 392 393 394 395 396 397 398 399 400 401 402 403 404 405 406\n" +
			"M  SAL   1 15 407 408 409 410 411 412 413 414 415 416 417 418 419 420 421\n" +
			"M  SAL   1 15 422 423 424 425 426 427 428 429 430 431 432 433 434 435 436\n" +
			"M  SAL   1 15 437 438 439 440 441 442 443 444 445 446 447 448 449 450 451\n" +
			"M  SAL   1 15 452 453 454 455 456 457 458 459 460 461 462 463 464 465 466\n" +
			"M  SAL   1 15 467 468 469 470 471 472 473 474 475 476 477 478 479 480 481\n" +
			"M  SAL   1 15 482 483 484 485 486 487 488 489 490 491 492 493 494 495 496\n" +
			"M  SAL   1 15 497 498 499 500 501 502 503 504 505 506 507 508 509 510 511\n" +
			"M  SAL   1 15 512 513 514 515 516 517 518 519 520 521 522 523 524 525 526\n" +
			"M  SAL   1 15 527 528 529 530 531 532 533 534 535 536 537 538 539 540 541\n" +
			"M  SAL   1 15 542 543 544 545 546 547 548 549 550 551 552 553 554 555 556\n" +
			"M  SAL   1 15 557 558 559 560 561 562 563 564 565 566 567 568 569 570 571\n" +
			"M  SAL   1 15 572 573 574 575 576 577 578 579 580 581 582 583 584 585 586\n" +
			"M  SAL   1 15 587 588 589 590 591 592 593 594 595 596 597 598 599 600 601\n" +
			"M  SAL   1 15 602 603 604 605 606 607 608 609 610 611 612 613 614 615 616\n" +
			"M  SAL   1 15 617 618 619 620 621 622 623 624 625 626 627 628 629 630 631\n" +
			"M  SAL   1 10 632 633 634 635 636 637 638 639 640 641\n" +
			"M  SPA   1 15   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15\n" +
			"M  SPA   1 15  16  17  18  19  20  21  22  23  24  25  26  27  28  29  30\n" +
			"M  SPA   1 15  31  32  33  34  35  36  37  38  39  40  41  42  43  44  45\n" +
			"M  SPA   1 15  46  47  48  49  50  51  52  53  54  55  56  57  58  59  60\n" +
			"M  SPA   1 15  61  62  63  64  65  66  67  68  69  70  71  72  73  74  75\n" +
			"M  SPA   1 15  76  77  78  79  80  81  82  83  84  85  86  87  88  89  90\n" +
			"M  SPA   1 15  91  92  93  94  95  96  97  98  99 100 101 102 103 104 105\n" +
			"M  SPA   1 15 106 107 108 109 110 111 112 113 114 115 116 117 118 119 120\n" +
			"M  SPA   1  8 121 122 123 124 125 126 127 128\n" +
			"M  SDI   1  4   18.5038  -15.9806   18.5038   -0.5461\n" +
			"M  SDI   1  4   38.1980   -0.5461   38.1980  -15.9806\n" +
			"M  SMT   1 5\n" +
			"M  SAL   2  8 129 642 643 644 645 646 647 648\n" +
			"M  SPA   2  1 129\n" +
			"M  SDI   2  4   43.3519   -9.4766   43.3519   -8.6366\n" +
			"M  SDI   2  4   44.1919   -8.6366   44.1919   -9.4766\n" +
			"M  SMT   2 8\n" +
			"M  END";
}
