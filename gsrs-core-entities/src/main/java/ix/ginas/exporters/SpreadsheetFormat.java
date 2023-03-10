package ix.ginas.exporters;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Function;

public abstract class SpreadsheetFormat extends OutputFormat {

    public SpreadsheetFormat(String extension, String displayname) {
        super(extension, displayname);
    }

    public abstract Spreadsheet createSpreadsheet(OutputStream out);

    public SpreadsheetFormat withInfo(Function<StringBuilder, String> extension, Function<StringBuilder, String> displayName){
        Objects.requireNonNull(extension);
        Objects.requireNonNull(displayName);

        return newSubclass(this, extension.apply(new StringBuilder(this.getExtension())), displayName.apply(new StringBuilder(this.getDisplayName())));
    }

    private SpreadsheetFormat newSubclass(SpreadsheetFormat parentClass, String ext, String display){
        return new SpreadsheetFormat(ext, display) {
            @Override
            public Spreadsheet createSpreadsheet(OutputStream out) {
                return parentClass.createSpreadsheet(out);
            }

            //@Override
            public Spreadsheet createSpreadsheet(InputStream out) {
                return null;
            }
        };
    }

    public static final SpreadsheetFormat CSV = new SpreadsheetFormat("csv", "Comma-delimited (.csv)"){

        @Override
        public Spreadsheet createSpreadsheet(OutputStream out) {
            return  new CsvSpreadsheetBuilder(out)
                    .quoteCells(true)
                    .maxRowsInMemory(100)
                    .build();
        }

        //@Override
        public Spreadsheet createSpreadsheet(InputStream out) {
            return null;
        }


    };

    public static final SpreadsheetFormat TSV = new SpreadsheetFormat("txt", "Tab-delimited (.txt)"){
        @Override
        public Spreadsheet createSpreadsheet(OutputStream out) {
            return  new CsvSpreadsheetBuilder(out)
                    .delimiter('\t')
                    .quoteCells(false)
                    .maxRowsInMemory(100)
                    .build();
        }

    };

    public static final SpreadsheetFormat XLSX = new SpreadsheetFormat("xlsx", "Excel (.xslx)"){
        @Override
        public Spreadsheet createSpreadsheet(OutputStream out) {

            return new ExcelSpreadsheet.Builder(out)
                    .maxRowsInMemory(100)
                    .build();

        }
    };
}
