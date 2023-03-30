package ix.ginas.exporters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import gov.nih.ncats.common.stream.StreamUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import gov.nih.ncats.common.io.IOUtil;

/**
 * {@link Spreadsheet} implementation that writes out
 * Excel files.  Currently only the newer xlsx is supported because
 * the older xls format doesn't support enough rows and the 3rd party
 * libraries we use don't have as nice streaming support when handling large data files.
 *
 * Created by katzelda on 8/24/16.
 */
public class ExcelSpreadsheet implements Spreadsheet {

    private final Workbook workbook;

    private final Sheet sheet;

    private final boolean AUTO_SPILL_OVER=true;
    private final long SPILL_OVER_AFTER=1000000;
    private final boolean AUTO_SPILL_HEADER_REPEAT=true;

    private List<String> headers;

    private final OutputStream out;

    private ExcelSpreadsheet(Workbook workbook, OutputStream out) {
        this.workbook = workbook;
        this.out = out;
        //just first sheet
        sheet = workbook.createSheet();
        
    }

    @Override
    public SpreadsheetRow getRow(int i) {
        org.apache.poi.ss.usermodel.Row r = sheet.getRow(i);
        int sheetIndex = 0;
        int newRowIndex= i;
        if(AUTO_SPILL_OVER) {
            sheetIndex= (int) (i/SPILL_OVER_AFTER);
            newRowIndex = (int) (i%SPILL_OVER_AFTER);

            sheetIndex= (int) (i/SPILL_OVER_AFTER);
            newRowIndex = (int) (i%SPILL_OVER_AFTER);
            if(sheetIndex>0) {
                if(AUTO_SPILL_HEADER_REPEAT) {
                    newRowIndex++; //add a row for header
                }
            }

            while(workbook.getNumberOfSheets()<sheetIndex+1) {
                Sheet sheetnew= workbook.createSheet();
                if(AUTO_SPILL_HEADER_REPEAT) {
                    if(headers!=null) {
                        Row rheaderNew = sheetnew.createRow(0);
                        for(int c=0;c<headers.size();c++) {
                            Cell cellNew=rheaderNew.createCell(c);
                            cellNew.setCellValue(headers.get(c));
                        }
                    }
                }
            }
        }
        if(sheetIndex==0 && newRowIndex==1) { //second row
            headers= StreamUtil.forIterable(workbook.getSheetAt(0).getRow(0))
                    .map(c->c.getStringCellValue())
                    .collect(Collectors.toList());
        }

        if (r == null) {
            r = workbook.getSheetAt(sheetIndex).createRow(newRowIndex);
        }
        return new RowWrapper(r);
    }

    @Override
    public void close() throws IOException {
        try{
            workbook.write(out);
        }finally{
            IOUtil.closeQuietly(out);

            workbook.close();
            //deletes tmp files
            if(workbook instanceof  SXSSFWorkbook){
                ((SXSSFWorkbook)workbook).dispose();
            }
        }

    }

    private static class RowWrapper implements SpreadsheetRow {
        private final org.apache.poi.ss.usermodel.Row row;

        public RowWrapper(org.apache.poi.ss.usermodel.Row r) {
            this.row = r;
        }


        @Override
        public SpreadsheetCell getCell(int j) {
            Cell cell = row.getCell(j);
            if(cell ==null){
                cell = row.createCell(j);
            }
            return new CellWrapper(cell);
        }
    }

    private static class CellWrapper implements SpreadsheetCell{
        private final Cell cell;
        public CellWrapper(Cell cell) {
            this.cell =cell;
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

    /**
     * Builder class to configure and then construct the ExcelSpreadsheet.
     */
    public static class Builder{

        private OutputStream out;

        private int maxInMemory =-1;


        /**
         * Create a new Builder that will write an excel file to the given
         * output File.
         *
         * @param outputFile the File to write to; can not be null.  If the path to this file
         *                   does not exist, then it will be created.  If the file does not exist, then it will be created.
         *
         * @throws IOException if there is a problem creating this file or the path to this file.
         *
         * @throws NullPointerException if outputFile is null.
         */
        public Builder(File outputFile) throws IOException{
            File parentDir = outputFile.getParentFile();
            if(parentDir !=null){
                parentDir.mkdirs();
            }
            out = new FileOutputStream(outputFile);
        }

        /**
         * Create a new Builder that will write an excel formatted data to the given
         * outputStream.
         *
         * @param out the outputStream to write to; can not be null.
         *
         * @throws NullPointerException if out is null.
         *
         */
        public Builder(OutputStream out){
            Objects.requireNonNull(out);

            this.out = out;
        }

        /**
         * Set the maximum number of rows in memory at any one time.
         * If we create more than the provided max rows, then the previous rows
         * will be flushed to the output and we can not go back and edit them.
         * This is useful when we are writing out a huge excel file and know we will only
         * be writing one row at a time.
         * @param maxRowsInMemory the number of rows to keep in memory at any one time.  If this
         *                        value is set to {@code -1} ( the default), then all rows will be kept in memory.
         *                        This value can not be {@code 0}.
         * @return this.
         *
         * @throws IllegalArgumentException if maxRowsInMemory is 0.
         */
        public Builder maxRowsInMemory(Integer maxRowsInMemory){
            if(maxRowsInMemory ==null){
                maxInMemory = -1;
            }else{
                if(maxRowsInMemory.intValue() == 0){
                    throw new IllegalArgumentException("max rows in memory can not be zero");
                }
                maxInMemory = maxRowsInMemory.intValue();
            }
            return this;
        }

        /**
         * Create a new {@link Spreadsheet} object that will write an Excel file
         * using the current builder configuration.
         * @return a new {@link Spreadsheet}; will never be null.
         */
        public Spreadsheet build(){

            Workbook workbook ;
            if(maxInMemory ==-1){
                workbook = new XSSFWorkbook();
            }else{
                workbook = new SXSSFWorkbook(maxInMemory);
            }
            return new ExcelSpreadsheet(workbook, out);
        }
    }
}
