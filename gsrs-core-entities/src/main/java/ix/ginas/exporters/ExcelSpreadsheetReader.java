package ix.ginas.exporters;

import gov.nih.ncats.common.io.IOUtil;
import gsrs.importer.DefaultPropertyBasedRecordContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class ExcelSpreadsheetReader {

    private final InputStream in;

    public ExcelSpreadsheetReader(InputStream inputStream) {
        this.in = inputStream;
        try {
            workbook = new XSSFWorkbook(inputStream);
        } catch (IOException e) {
            log.error("Error initializing ExcelSpreadsheetReader", e);
            throw new RuntimeException(e);
        }
    }

    private final Workbook workbook;


    public void close() throws IOException {
        IOUtil.closeQuietly(in);

        workbook.close();
        //deletes tmp files
        if (workbook instanceof SXSSFWorkbook) {
            ((SXSSFWorkbook) workbook).dispose();
        }
    }

    private static class RowWrapper implements Spreadsheet.SpreadsheetRow {
        private final org.apache.poi.ss.usermodel.Row row;

        public RowWrapper(org.apache.poi.ss.usermodel.Row r) {
            this.row = r;
        }


        @Override
        public SpreadsheetCell getCell(int j) {
            Cell cell = row.getCell(j);
            if (cell == null) {
                cell = row.createCell(j);
            }
            return new ExcelSpreadsheetReader.CellWrapper(cell);
        }
    }

    private static class CellWrapper implements SpreadsheetCell {
        private final Cell cell;

        public CellWrapper(Cell cell) {
            this.cell = cell;
        }

        @Override
        public void writeInteger(int i) {
            cell.setCellValue(i);
        }

        @Override
        public void writeDate(Date date) {
            cell.setCellValue(date);
        }

        @Override
        public void writeString(String s) {
            cell.setCellValue(s);
        }
    }


    private List<String> getValuesFromRow(Row row) {
        List<String> fields = new ArrayList<>();
        if (row == null) {
            log.warn("Excel row is null!");
            return fields;
        }
        row.cellIterator().forEachRemaining(c -> {
            String fieldName = getCellValueAsString(c);
            if (fieldName != null && fieldName.trim().length() > 0) {
                fields.add(fieldName);
            }
        });
        return fields;
    }

    public List<String> getFileFields(String sheetName, int row) {
        Sheet fieldNameSheet = workbook.getSheet(sheetName);
        return getFileFields(fieldNameSheet, row);
    }

    public List<String> getFileFields(Sheet fieldNameSheet, int row) {
        Row fieldRow = fieldNameSheet.getRow(row);
        return getValuesFromRow(fieldRow);
    }

    public List<String> getFileFields(int sheet, int row) {
        Sheet fieldNameSheet = workbook.getSheetAt(sheet);
        return getFileFields(fieldNameSheet, row);
    }

    public Stream<DefaultPropertyBasedRecordContext> readSheet(String sheetName, List<String> inputFields, int rowWithFields){

        Sheet dataSheet = workbook.getSheet(sheetName);
        List<String> fields;
        if (inputFields != null && inputFields.size() > 0) {
            fields = new ArrayList<>(inputFields);
        } else {
            fields = getFileFields(dataSheet, rowWithFields);
        }
        Stream.Builder<DefaultPropertyBasedRecordContext> builder = Stream.builder();
        Iterator<Row> iterator = dataSheet.rowIterator();
        iterator.forEachRemaining(r -> {
            if (r.getRowNum() != rowWithFields) {
                DefaultPropertyBasedRecordContext record = new DefaultPropertyBasedRecordContext();
                Map<String, String> lineValues = new HashMap<>();
                for (int i = 0; i < fields.size(); i++) {
                    if (r.getCell(i) != null) {
                        try {
                            String currentValue = getCellValueAsString(r.getCell(i));
                            lineValues.put(fields.get(i), currentValue);
                        } catch (IndexOutOfBoundsException | NullPointerException e) {
                            log.warn("error reading field '{}' from row '{}'", fields.get(i), r.getRowNum());
                        }
                    }
                }
                record.setAllProperties(lineValues);
                builder.add(record);
            }
        });
        return builder.build();
    }


    public String getCellValueAsString(Cell cell) {
        String stringValue;
        switch (cell.getCellType()) {
            case BLANK:
                stringValue = "";
                break;
            case BOOLEAN:
                stringValue = cell.getBooleanCellValue() ? "true" : "false or null";
                break;
            case FORMULA:
                stringValue = cell.getStringCellValue();// cell.getCellFormula() + " [formula]";
                break;
            case NUMERIC:
                stringValue = Double.toString(cell.getNumericCellValue());
                break;
            case STRING:
                stringValue = cell.getStringCellValue();
                break;
            case ERROR:
                stringValue = Byte.toString(cell.getErrorCellValue());
                break;
            default:
                stringValue = "[unrecognized]";
        }
        return stringValue;
    }


}
