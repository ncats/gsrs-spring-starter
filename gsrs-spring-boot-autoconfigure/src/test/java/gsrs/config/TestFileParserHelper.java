package gsrs.config;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import gsrs.config.FilePathParserUtils.FileParser;

import javax.swing.plaf.synth.SynthTextAreaUI;

import static org.junit.jupiter.api.Assertions.*;


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
        File myFile = new File("path/to/test");
        FileParser.FileParserBuilder builder = FileParser.builder();
        File f = builder.absoluteRootPath(new File("another").getAbsoluteFile())
        .defaultFilePath(null)
        .suppliedFilePath(myFile.getAbsolutePath())
        .build().getFile();
        
        assertEquals( myFile.getAbsolutePath(), f.getAbsolutePath());
    }
    

    @Test
    public void absoluteFilePathWithSetRelativeRootShouldBeAbsolute() throws IOException {
        File myFile = new File("path/to/test");
        FileParser.FileParserBuilder builder = FileParser.builder();
        File f = builder.absoluteRootPath(new File("another"))
                .defaultFilePath(null)
                .suppliedFilePath(myFile.getAbsolutePath())
                .build().getFile();

        assertEquals( myFile.getAbsolutePath(), f.getAbsolutePath());
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

    @Test
    public void testFailOnBadPathResolutionNormal() throws IOException {
        boolean hasException = false;
        try {
            // Should succeed since normalized path ok
            FilePathParserUtils.failOnBadPathResolution("/tmp", "my-file.txt");
        } catch(Exception e) {
            System.out.println(e.getMessage());
            hasException = true;
        }
        assertFalse(hasException);
    }

    @Test
    public void testFailOnBadPathResolutionSingleDot1() throws IOException {
        boolean hasException = false;
        try {
            // Should succeed since path resolves to /tmp/my-file.txt
            FilePathParserUtils.failOnBadPathResolution("/tmp", "./my-file.txt");
        } catch(Exception e) {
            System.out.println(e.getMessage());
            hasException = true;
        }
        assertFalse(hasException);
    }

    @Test
    public void testFailOnBadPathResolutionDotDot1() throws IOException {
        boolean hasException = false;
        try {
            // Should succeed since path resolves to /tmp/my-file.txt
            FilePathParserUtils.failOnBadPathResolution("/tmp", "../tmp/my-file.txt");
        } catch(Exception e) {
            System.out.println(e.getMessage());
            hasException = true;
        }
        assertFalse(hasException);
    }

    @Test
    public void testFailOnBadPathResolutionDotDot2() throws IOException {
        boolean hasException = false;
        try {
            // Should fail since path resolves to /etc/my-file.txt
            FilePathParserUtils.failOnBadPathResolution("/tmp", "../etc/my-file.txt");
        } catch(Exception e) {
            System.out.println(e.getMessage());
            hasException = true;
        }
        assertTrue(hasException);
    }

    @Test
    public void testFailOnBadPathResolutionAbsolutePathException() throws IOException {
        boolean hasException = false;
        try {
            // Should fail due to absolute path
            FilePathParserUtils.failOnBadPathResolution("/tmp", "/my/abs/path");
        } catch(Exception e) {
            System.out.println(e.getMessage());
            hasException = true;
        }
        assertTrue(hasException);
    }

}
