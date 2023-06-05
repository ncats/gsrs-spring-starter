package gsrs.dataexchange.imports;

import gsrs.importer.DefaultPropertyBasedRecordContext;
import ix.ginas.importers.ExcelSpreadsheetReader;
import ix.ginas.importers.InputFieldStatistics;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class ExcelSpreadsheetReaderTest {

    @Test
    public void basicReadTest1() throws IOException {
        AtomicBoolean readData= new AtomicBoolean(false);
        String fileName ="excel/simple-wb.xlsx";
        ClassPathResource resource = new ClassPathResource(fileName);
        File inputFile = resource.getFile();
        FileInputStream fis = new FileInputStream(inputFile);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet datatypeSheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = datatypeSheet.rowIterator();
        rowIterator.forEachRemaining(r->{
               Iterator<Cell> cellIterator= r.cellIterator();
               cellIterator.forEachRemaining(c->{
                   String stringValue;
                   switch (c.getCellType()) {
                       case BLANK:
                           stringValue = "cell is blank";
                           break;
                       case BOOLEAN:
                           stringValue= c.getBooleanCellValue() ? "true" : "false or null";
                           readData.set(true);
                           break;
                       case FORMULA:
                           stringValue= c.getCellFormula() + " [formula]";
                           readData.set(true);
                           break;
                       case NUMERIC:
                           stringValue=String.format("%.3f [number - %s]", c.getNumericCellValue(), c.getCellStyle().getDataFormatString());

                           readData.set(true);
                           break;
                       case STRING:
                           stringValue=c.getStringCellValue();
                           readData.set(true);
                           break;
                       case ERROR:
                           stringValue= c.getErrorCellValue() + " [error]";
                           break;
                       default:
                           stringValue="[unrecognized]";

                   }
                   System.out.println(c.getAddress().toString() + " " + stringValue);
               });

        });
        System.out.println("file contents complete");
        Assertions.assertTrue(readData.get());
    }

    @Test
    public void getFileFieldsTest() throws IOException {
        String filePathName = "excel/Sample1.xlsx";
        File inputFile= (new ClassPathResource(filePathName)).getFile();
        FileInputStream fis = new FileInputStream(inputFile);
        ExcelSpreadsheetReader reader= new ExcelSpreadsheetReader(fis);
        List<String> fieldNames= reader.getFileFields("Names",4);
        fis.close();
        System.out.println("field names:");
        fieldNames.forEach(System.out::println);
        List<String> expectedFields = Arrays.asList("UUID", "APPROVAL_ID", "DISPLAY_NAME", "RN", "EC", "NCIT", "RXCUI", "PUBCHEM",
                "ITIS", "NCBI", "PLANTS", "GRIN", "MPNS", "INN_ID", "USAN_ID", "MF", "INCHIKEY", "SMILES", "INGREDIENT_TYPE",
                "UTF8_DISPLAY_NAME", "SUBSTANCE_TYPE", "PROTEIN_SEQUENCE", "NUCLEIC_ACID_SEQUENCE", "RECORD_ACCESS_GROUPS");

        Assertions.assertTrue(fieldNames.containsAll(expectedFields));
    }

    @Test
    public void getFileFieldsTestNone() throws IOException {
        String filePathName = "excel/Sample1.xlsx";
        File inputFile= (new ClassPathResource(filePathName)).getFile();
        FileInputStream fis = new FileInputStream(inputFile);
        ExcelSpreadsheetReader reader= new ExcelSpreadsheetReader(fis);
        List<String> fieldNames= reader.getFileFields("Names",6);
        fis.close();
        System.out.println("field names:");
        fieldNames.forEach(System.out::println);
        Assertions.assertEquals(0, fieldNames.size());
    }
    @Test
    public void getFileFieldsTest2() throws IOException {
        String fileName ="excel/simple-wb.xlsx";
        ClassPathResource resource = new ClassPathResource(fileName);
        File inputFile = resource.getFile();
        FileInputStream fis = new FileInputStream(inputFile);
        ExcelSpreadsheetReader reader= new ExcelSpreadsheetReader(fis);
        List<String> fieldNames= reader.getFileFields(0, 0);
        fis.close();

        System.out.println("field names:");
        fieldNames.forEach(System.out::println);
        Assertions.assertEquals(4, fieldNames.size());
    }

    @Test
    public void readFileTest() throws IOException {
        String filePathName = "excel/export-29-12-2022_17-40-13.xlsx";
        File inputFile= (new ClassPathResource(filePathName)).getFile();
        FileInputStream fis = new FileInputStream(inputFile);
        ExcelSpreadsheetReader reader= new ExcelSpreadsheetReader(fis);
        List<DefaultPropertyBasedRecordContext> records= reader.readSheet("Sheet0",null, 0).collect(Collectors.toList());
        fis.close();
        System.out.println("data:");
        List<String> expectedFields = Arrays.asList("UUID", "APPROVAL_ID", "DISPLAY_NAME", "RN", "EC", "NCIT", "RXCUI", "PUBCHEM",
                "ITIS", "NCBI", "PLANTS", "GRIN", "MPNS", "INN_ID", "USAN_ID", "MF", "INCHIKEY", "SMILES", "INGREDIENT_TYPE",
                "UTF8_DISPLAY_NAME", "SUBSTANCE_TYPE", "PROTEIN_SEQUENCE", "NUCLEIC_ACID_SEQUENCE", "RECORD_ACCESS_GROUPS");


        List<String> actualFields= records.stream().map(DefaultPropertyBasedRecordContext::getProperties).flatMap(List::stream).distinct().collect(Collectors.toList());
        Assertions.assertTrue(actualFields.containsAll(expectedFields));

        //check a couple of fields on one row
        DefaultPropertyBasedRecordContext recordData= records.get(10);
        String values= "cddf76a0-bd3b-402d-8f6b-f450abefcc85\t\t1H-PYRROLE-1-HEPTANOIC ACID, 2-(4-FLUOROPHENYL)-.BETA.,.DELTA.-DIHYDROXY-5-(1-METHYLETHYL)-3-PHENYL-4-((PHENYLAMINO)CARBONYL)-, (.BETA.R,.DELTA.R)-\t\t\t\t\t\t\t\t\t\t\t\t\tC33H35FN2O5\tXUKUURHRXDUEBC-KAYWLYCHSA-N\tCC(C)c1c(c(-c2ccccc2)c(-c3ccc(cc3)F)n1CC[C@]([H])(C[C@]([H])(CC(=O)O)O)O)C(=Nc4ccccc4)O\tINGREDIENT SUBSTANCE\t1H-Pyrrole-1-heptanoic acid, 2-(4-fluorophenyl)-β,δ-dihydroxy-5-(1-methylethyl)-3-phenyl-4-[(phenylamino)carbonyl]-, (βR,δR)-real again 1\tchemical\t\t\tadmin";
        List<String> testValues=Arrays.asList(values.split("\t"));
        Assertions.assertTrue(recordData.getProperties().stream().allMatch(r->{
            if((recordData.getProperty(r).isPresent())) {
                int index = expectedFields.indexOf(r);
                System.out.printf("field %d %s = %s\n", index, r, recordData.getProperty(r).get());
                return testValues.get(index).equals(recordData.getProperty(r).get());
            }
            return true;
        }));

    }

    @Test
    public void readFileTest2() throws IOException {
        String filePathName = "excel/export-03-01-2023_9-29-45.xlsx";
        File inputFile= (new ClassPathResource(filePathName)).getFile();
        FileInputStream fis = new FileInputStream(inputFile);
        ExcelSpreadsheetReader reader= new ExcelSpreadsheetReader(fis);
        List<String> fields = Arrays.asList("UUID", "APPROVAL_ID", "DISPLAY_NAME", "RN", "EC", "NCIT", "RXCUI", "PUBCHEM",
                "ITIS", "NCBI", "PLANTS", "GRIN", "MPNS", "INN_ID", "USAN_ID", "MF", "INCHIKEY", "SMILES", "INGREDIENT_TYPE",
                "UTF8_DISPLAY_NAME", "SUBSTANCE_TYPE", "PROTEIN_SEQUENCE", "NUCLEIC_ACID_SEQUENCE", "RECORD_ACCESS_GROUPS");

        List<DefaultPropertyBasedRecordContext> records= reader.readSheet("Sheet0", fields, -1).collect(Collectors.toList());
        fis.close();
        System.out.println("data:");

        List<String> actualFields= records.stream().map(DefaultPropertyBasedRecordContext::getProperties).flatMap(List::stream).distinct().collect(Collectors.toList());

        //check a couple of fields on one row
        DefaultPropertyBasedRecordContext recordData= records.get(54);
        String values= "294de533-af70-45cc-af7a-f307c37cf00e\t3W58ITX06Q\tVINYLBITAL\t2430-49-1\t219-395-8\tC74377\t19928\t72135\t\t\t\t\t\t1153\t\tC11H16N2O3\tKGKJZEKQJQQOTD-UHFFFAOYSA-N\tCCCC(C)C1(C=C)C(=O)NC(=O)NC1=O\tINGREDIENT SUBSTANCE\tVINYLBITAL\tchemical\t\t";
        List<String> testValues=Arrays.asList(values.split("\t"));
        Assertions.assertTrue(recordData.getProperties().stream().allMatch(r->{
            if((recordData.getProperty(r).isPresent() && recordData.getProperty(r).get().length()>0)) {
                int index = fields.indexOf(r);
                System.out.printf("field %d %s = %s\n", index, r, recordData.getProperty(r).get());
                return testValues.get(index).equals(recordData.getProperty(r).get());
            }
            return true;
        }));

    }

    @Test
    public void getFileStatisticsTest() throws IOException {
        String filePathName = "excel/export-29-12-2022_17-40-13.xlsx";
        File inputFile= (new ClassPathResource(filePathName)).getFile();
        FileInputStream fis = new FileInputStream(inputFile);
        ExcelSpreadsheetReader reader= new ExcelSpreadsheetReader(fis);
        Map<String, InputFieldStatistics>statisticsMap= reader.getFileStatistics("Sheet0", null, 4, 0);
        List<String> expectedFields = Arrays.asList("UUID", "APPROVAL_ID", "DISPLAY_NAME", "RN", "EC", "NCIT", "RXCUI", "PUBCHEM",
                "ITIS", "NCBI", "PLANTS", "GRIN", "MPNS", "INN_ID", "USAN_ID", "MF", "INCHIKEY", "SMILES", "INGREDIENT_TYPE",
                "UTF8_DISPLAY_NAME", "SUBSTANCE_TYPE", "PROTEIN_SEQUENCE", "NUCLEIC_ACID_SEQUENCE", "RECORD_ACCESS_GROUPS");
        List<String> actualFields= statisticsMap.keySet().stream().collect(Collectors.toList());
        Assertions.assertTrue(expectedFields.containsAll(actualFields));
        statisticsMap.keySet().forEach(k->{
            InputFieldStatistics statistics= statisticsMap.get(k);
            log.trace("field: {} - examples: {}",k, String.join("|", statistics.getExamples()));
        });
    }
    /*@Test
    public void listPoi() {
        ClassLoader classloader =
                org.apache.poi.poifs.filesystem.POIFSFileSystem.class.getClassLoader();
        URL res = classloader.getResource(
                "org/apache/poi/poifs/filesystem/POIFSFileSystem.class");
        String path = res.getPath();
        System.out.println("POI Core came from " + path);

        classloader = org.apache.poi.ooxml.POIXMLDocument.class.getClassLoader();
        res = classloader.getResource("org/apache/poi/ooxml/POIXMLDocument.class");
        path = res.getPath();
        System.out.println("POI OOXML came from " + path);

        classloader = org.apache.poi.hslf.usermodel.HSLFSlideShow.class.getClassLoader();
        res = classloader.getResource("org/apache/poi/hslf/usermodel/HSLFSlideShow.class");
        path = res.getPath();
        System.out.println("POI Scratchpad came from " + path);
        Assertions.assertNotNull(path);
    }*/
}
