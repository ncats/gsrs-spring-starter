package gsrs.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.controller.GsrsAdminLogConfiguration;
import gsrs.springUtils.StaticContextAccessor;
import lombok.Builder;
import lombok.Data;

public class FilePathParserUtils {    
    
    @Data
    @Builder
    public static class FileParser{
        private String suppliedFilePath;
        private String defaultFilePath; 
        
        @Builder.Default
        private DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        
        @Builder.Default
        private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmmss");
        
        private File absoluteRootPath;
        
        public File getFile(){
            File f1= FilePathParserUtils.getFile(suppliedFilePath, defaultFilePath, dateFormatter, timeFormatter, absoluteRootPath);
            File parent = f1 ==null? null: f1.getParentFile();
            //don't check for IOException to keep API interface backwards compatible
            if(parent !=null){
                parent.mkdirs();
            }

            return f1;
        }
    }

    

    public static FileParser.FileParserBuilder getFileParserBuilder(){
        return new FileParser.FileParserBuilder()
                             .absoluteRootPath(getDefaultRootDir());
    }
    

    public static File getFile(String outputPath, String defaultPath, DateTimeFormatter formatter, DateTimeFormatter formatterTime, File rootPath) {
        if(outputPath ==null) {
            outputPath= defaultPath;
        }
        String date = formatter.format(TimeUtil.getCurrentLocalDateTime());
        String time = formatterTime.format(TimeUtil.getCurrentLocalDateTime());

        String fpath = outputPath.replace("%DATE%", date)
                                 .replace("%TIME%", time);
        File rf = new File(fpath);
        if(rf.isAbsolute()) {
           return rf; 
        }else {
           return new File(rootPath, fpath);
        }
    }

    
    //TODO: the concept of the "root" running directory should be more lower-level config than this
    //class specified. Though this could serve as a backup
    private static File getDefaultRootDir() {
        GsrsAdminLogConfiguration logConf = StaticContextAccessor.getBean(GsrsAdminLogConfiguration.class);
        File f = logConf.getRootPath();
        if(f!=null && !f.isAbsolute()) {
            f= f.getAbsoluteFile();
        }
        if(f!=null) {
            return f.toPath().normalize().toFile();
        }
        return null;
    }

    public static void failOnBadPathResolution(String basePath, String passedPath)
    throws InvalidPathException, IOException {
        Path basePathObj;
        Path resolvedPathObj;
        try {
            basePathObj = Paths.get(basePath);
            resolvedPathObj = basePathObj.resolve(passedPath);
//            System.out.println("getPath       : " + resolvedPathObj.toFile().getPath());
//            System.out.println("getAbsPath    : " + resolvedPathObj.toFile().getAbsolutePath());
//            System.out.println("getNormAbsPath: " + resolvedPathObj.normalize().toAbsolutePath());
        } catch (InvalidPathException ipe) {
            // Don't want to provide input path as it might get flagged.
            throw new InvalidPathException("Relating to passedPath", "Exception while resolving path");
        }
        if (Paths.get(passedPath).isAbsolute()) {
            throw new IOException("Absolute path not allowed.");
        }
        if (!resolvedPathObj.normalize().startsWith(basePathObj)) {
            throw new IOException("Unexpected start of path.");
        }
    }

}
