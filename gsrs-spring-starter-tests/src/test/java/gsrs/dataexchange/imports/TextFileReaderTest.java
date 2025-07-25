package gsrs.dataexchange.imports;

import gsrs.importer.DefaultPropertyBasedRecordContext;
import ix.ginas.importers.TextFileReader;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
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
    public void testReadPubChemData1() throws IOException {
        String fileName = "text/pubchem_data_through_data_warrior.txt";
        String structureNumberProperty = "Structure No";
        File textFile = (new ClassPathResource(fileName)).getFile();
        Assertions.assertTrue(textFile.exists());
        FileInputStream fileInputStream = new FileInputStream(textFile);
        TextFileReader reader = new TextFileReader();
        Stream<DefaultPropertyBasedRecordContext> dataRecordContextStream =reader.readFile(fileInputStream, "\t",true, null);
        fileInputStream.close();
        long expectedRecordCount =50;
        List<DefaultPropertyBasedRecordContext> data = dataRecordContextStream.collect(Collectors.toList());
        long actual = data.size();
        Assertions.assertEquals(expectedRecordCount, actual);
        data.forEach( d->{
            if( !d.getProperty(structureNumberProperty).isPresent() || d.getProperty(structureNumberProperty).get().length() <= 0) {
                log.error("Error!  expected property not found for record {}", d.getProperty("PUBCHEM_COMPOUND_CID").get());
            }
        });
        Assertions.assertTrue(data.stream().allMatch(
                r->r.getProperty(structureNumberProperty).isPresent() && r.getProperty(structureNumberProperty).get().length()>0
                        && r.getProperty("PUBCHEM_COMPOUND_CID").get().length()>0));
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

    @Test
    public void testGetFieldsHandlingQuotes() throws IOException {
        String fileName = "text/TextFileWithSomeQuotes.txt";
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
    public void testRemoveQuotesFromStringWithBeforeAndAfter() {
        String input1 = "\"chemical name\"";
        String expected = "chemical name";
        String actual =TextFileReader.removeQuotes(input1);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testRemoveQuotesFromStringWithJustBefore() {
        String input1 = "\"chemical name";
        String expected = "chemical name";
        String actual =TextFileReader.removeQuotes(input1);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testRemoveQuotesFromStringWithJustAfter() {
        String input1 = "chemical name\"";
        String expected = "chemical name";
        String actual =TextFileReader.removeQuotes(input1);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testRemoveQuotesFromStringNeither() {
        String input1 = "chemical name";
        String expected = "chemical name";
        String actual =TextFileReader.removeQuotes(input1);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void isCharDoubleQuote(){
        char input1 = '"';
        boolean actual = TextFileReader.isQuote(input1);
        Assertions.assertTrue(actual);
    }

    @Test
    public void isCharSingleQuote(){
        char input1 = '\'';
        boolean actual = TextFileReader.isQuote(input1);
        Assertions.assertTrue(actual);
    }

    @Test
    public void isCharLetter(){
        char input1 = 'a';
        boolean actual = TextFileReader.isQuote(input1);
        Assertions.assertFalse(actual);
    }

    @Test
    public void isCharNumberer(){
        char input1 = '9';
        boolean actual = TextFileReader.isQuote(input1);
        Assertions.assertFalse(actual);
    }

    @Test
    public void isCharBackTick(){
        char input1 = '`';
        boolean actual = TextFileReader.isQuote(input1);
        Assertions.assertFalse(actual);
    }

}
