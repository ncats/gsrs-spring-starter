package gsrs.dataexchange.imports;

import gsrs.importer.DefaultPropertyBasedRecordContext;
import ix.ginas.importers.TextFileReader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TextFileReaderTest {

    @Test
    public void testRead1() throws IOException {
        String fileName = "text/export-INN_MIXTURES_PLUS.txt";
        File textFile = (new ClassPathResource(fileName)).getFile();
        Assertions.assertTrue(textFile.exists());
        FileInputStream fileInputStream = new FileInputStream(textFile);
        TextFileReader reader = new TextFileReader();
        Stream<DefaultPropertyBasedRecordContext> dataRecordContextStream =reader.readFile(fileInputStream, "\t",false, null);
        fileInputStream.close();
        long expectedRecordCount =35;
        List<DefaultPropertyBasedRecordContext> data = dataRecordContextStream.collect(Collectors.toList());
        long actual = data.size();
        Assertions.assertEquals(expectedRecordCount, actual);
        String uuid="84d0336c-d9a6-4394-8d42-c2afdbcd93b5";
        String expectedApprovalId="7HMD7M29RI";
        DefaultPropertyBasedRecordContext selectedDataItem = data.stream().filter(d->d.getProperty("UUID").get().equals(uuid)).findFirst().get();
        Assertions.assertEquals(expectedApprovalId, selectedDataItem.getProperty("APPROVAL_ID").get());
    }

    @Test
    public void testRead2() throws IOException {
        String fileName = "text/export-inn-proteins-plus.csv";
        File textFile = (new ClassPathResource(fileName)).getFile();
        Assertions.assertTrue(textFile.exists());
        FileInputStream fileInputStream = new FileInputStream(textFile);
        TextFileReader reader = new TextFileReader();
        Stream<DefaultPropertyBasedRecordContext> dataRecordContextStream =reader.readFile(fileInputStream, ",", true, null);
        fileInputStream.close();
        long expectedRecordCount =125;
        List<DefaultPropertyBasedRecordContext> data = dataRecordContextStream.collect(Collectors.toList());
        long actual = data.size();
        Assertions.assertEquals(expectedRecordCount, actual);
        String uuid="543a3b27-f51e-477b-8a87-dabf509517ed";
        String expectedRn="1186098-83-8";
        DefaultPropertyBasedRecordContext selectedDataItem = data.stream().filter(d->d.getProperty("UUID").get().equals(uuid)).findFirst().get();
        Assertions.assertEquals(expectedRn, selectedDataItem.getProperty("RN").get());
    }

    @Test
    public void testGetFields() throws IOException {
        String fileName = "text/export-inn-proteins-plus.csv";
        List<String> expectedFields = Arrays.asList("UUID","APPROVAL_ID","DISPLAY_NAME","RN","EC","NCIT","RXCUI","PUBCHEM","ITIS","NCBI","PLANTS","GRIN","MPNS","INN_ID","USAN_ID","MF","INCHIKEY","SMILES","INGREDIENT_TYPE","UTF8_DISPLAY_NAME","SUBSTANCE_TYPE","PROTEIN_SEQUENCE","NUCLEIC_ACID_SEQUENCE","RECORD_ACCESS_GROUPS");
        File textFile = (new ClassPathResource(fileName)).getFile();
        Assertions.assertTrue(textFile.exists());
        FileInputStream fileInputStream = new FileInputStream(textFile);
        TextFileReader reader = new TextFileReader();
        List<String> actualFields =reader.getFileFields(fileInputStream, ",", true);
        fileInputStream.close();

        Assertions.assertEquals(expectedFields, actualFields);
    }


    @Test
    public void testGetFieldsAndRead() throws IOException {
        String fileName = "text/export-inn-proteins-plus.csv";
        File textFile = (new ClassPathResource(fileName)).getFile();
        Assertions.assertTrue(textFile.exists());
        FileInputStream fileInputStream = new FileInputStream(textFile);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        bufferedInputStream.mark(30000);

        TextFileReader reader = new TextFileReader();
        List<String> actualFields =reader.getFileFields(bufferedInputStream, ",", true);
        if( bufferedInputStream.markSupported()) {
            bufferedInputStream.reset();
        }
        Stream<DefaultPropertyBasedRecordContext> dataRecordContextStream =reader.readFile2(bufferedInputStream, ",", true, actualFields, 1);
        bufferedInputStream.close();
        long expectedRecordCount =125;
        List<DefaultPropertyBasedRecordContext> data = dataRecordContextStream.collect(Collectors.toList());
        long actual = data.size();
        Assertions.assertEquals(expectedRecordCount, actual);


    }
}
