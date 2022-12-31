package ix.ginas.exporters;

import gov.nih.ncats.common.io.IOUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.Date;
import java.util.Objects;

public class ExcelSpreadsheetReader implements Spreadsheet{

    private final Workbook workbook;

    private final Sheet sheet;


    private final InputStream in;

    private ExcelSpreadsheetReader(Workbook workbook, InputStream in) {
        this.workbook = workbook;
        this.in = in;
        //just first sheet
        sheet = workbook.createSheet();
    }


    @Override
    public Spreadsheet.SpreadsheetRow getRow(int i) {
        org.apache.poi.ss.usermodel.Row r = sheet.getRow(i);
        if (r == null) {
            r = sheet.createRow(i);
        }
        return new ExcelSpreadsheetReader.RowWrapper(r);
    }

    @Override
    public void close() throws IOException {
        try{
            //workbook.write(out);
        }finally{
            IOUtil.closeQuietly(in);

            workbook.close();
            //deletes tmp files
            if(workbook instanceof SXSSFWorkbook){
                ((SXSSFWorkbook)workbook).dispose();
            }
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
            if(cell ==null){
                cell = row.createCell(j);
            }
            return new ExcelSpreadsheetReader.CellWrapper(cell);
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
     * Builder class to configure and then construct the ExcelSpreadsheetReader.
     */
    public static class Builder{

        private InputStream in;

        private int maxInMemory =-1;


        /**
         * Create a new Builder that will write an Excel file to the given
         * output File.
         *
         * @param inputFile the File to write to; can not be null.  If the path to this file
         *                   does not exist, then it will be created.  If the file does not exist, then it will be created.
         *
         * @throws IOException if there is a problem creating this file or the path to this file.
         *
         * @throws NullPointerException if outputFile is null.
         */
        public Builder(File inputFile) throws IOException{
            File parentDir = inputFile.getParentFile();
            if(parentDir !=null){
                parentDir.mkdirs();
            }
            in = new FileInputStream(inputFile);
        }

        /**
         * Create a new Builder that will write an excel formatted data to the given
         * outputStream.
         *
         * @param in the inputStream to write to; cannot be null.
         *
         * @throws NullPointerException if out is null.
         *
         */
        public Builder(InputStream in){
            Objects.requireNonNull(in);

            this.in = in;
        }

        /**
         * Set the maximum number of rows in memory at any one time.
         * If we create more than the provided max rows, then the previous rows
         * will be flushed to the output and we can not go back and edit them.
         * This is useful when we are writing out a huge Excel file and know we will only
         * be writing one row at a time.
         * @param maxRowsInMemory the number of rows to keep in memory at any one time.  If this
         *                        value is set to {@code -1} ( the default), then all rows will be kept in memory.
         *                        This value can not be {@code 0}.
         * @return this.
         *
         * @throws IllegalArgumentException if maxRowsInMemory is 0.
         */
        public ExcelSpreadsheetReader.Builder maxRowsInMemory(Integer maxRowsInMemory){
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
            return new ExcelSpreadsheetReader(workbook, in);
        }
    }

}
