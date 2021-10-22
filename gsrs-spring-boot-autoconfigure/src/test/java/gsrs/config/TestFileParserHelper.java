package gsrs.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import gsrs.config.FilePathParserUtils.FileParser;


public class TestFileParserHelper {

    

    @Test
    public void absoluteFilePathWithNullRootShouldBeAbsolute() throws IOException {
        FileParser.FileParserBuilder builder = FileParser.builder();
        File f = builder.absoluteRootPath(null)
        .defaultFilePath(null)
        .suppliedFilePath("/path/to/test")
        .build().getFile();
        
        assertEquals(new File("/path/to/test"), f);
    }
    

    @Test
    public void absoluteFilePathWithSetAbsoluteRootShouldBeAbsolute() throws IOException {
        FileParser.FileParserBuilder builder = FileParser.builder();
        File f = builder.absoluteRootPath(new File("/another"))
        .defaultFilePath(null)
        .suppliedFilePath("/path/to/test")
        .build().getFile();
        
        assertEquals(new File("/path/to/test"), f);
    }
    

    @Test
    public void absoluteFilePathWithSetRelativeRootShouldBeAbsolute() throws IOException {
        FileParser.FileParserBuilder builder = FileParser.builder();
        File f = builder.absoluteRootPath(new File("./another"))
        .defaultFilePath(null)
        .suppliedFilePath("/path/to/test")
        .build().getFile();
        
        assertEquals(new File("/path/to/test"), f);
    }
    
    
    @Test
    public void relativeFilePathWithNullRootShouldBeRelative() throws IOException {
        FileParser.FileParserBuilder builder = FileParser.builder();
        File f = builder.absoluteRootPath(null)
        .defaultFilePath(null)
        .suppliedFilePath("test")
        .build().getFile();
        
        assertEquals(new File("test"), f);
    }
    

    @Test
    public void relativeFilePathWithSetRootShouldBeAbsolute() throws IOException {
        FileParser.FileParserBuilder builder = FileParser.builder();
        File f = builder.absoluteRootPath(new File("/tmp"))
        .defaultFilePath(null)
        .suppliedFilePath("test")
        .build().getFile();
        
        assertEquals(new File("/tmp/test"), f);
    }
    

    @Test
    public void relativeFilePathWithSetRelativeRootShouldBeEffectivelyRelative() throws IOException {
        FileParser.FileParserBuilder builder = FileParser.builder();
        File f = builder.absoluteRootPath(new File("."))
        .defaultFilePath(null)
        .suppliedFilePath("test")
        .build().getFile();
        
        assertEquals(new File("test").toPath().normalize(), f.toPath().normalize());
    }
    


    @Test
    public void nullSuppliedGivenDefaultFilePathWithSetRelativeRootShouldBeEffectivelyRelative() throws IOException {
        FileParser.FileParserBuilder builder = FileParser.builder();
        File f = builder.absoluteRootPath(new File("."))
        .suppliedFilePath(null)
        .defaultFilePath("test")
        .build().getFile();
        
        assertEquals(new File("test").toPath().normalize(), f.toPath().normalize());
    }
    


    @Test
    public void nullSuppliedGivenDefaultRelativeWithSetRootShouldBeAbsolute() throws IOException {
        FileParser.FileParserBuilder builder = FileParser.builder();
        File f = builder.absoluteRootPath(new File("/tmp"))
        .suppliedFilePath(null)
        .defaultFilePath("test")
        .build().getFile();

        assertEquals(new File("/tmp/test"), f);
    }
    
    
}
