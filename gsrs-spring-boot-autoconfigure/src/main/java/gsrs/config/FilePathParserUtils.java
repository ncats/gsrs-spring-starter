package gsrs.config;

import java.io.File;
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
        
        public File getFile() {
            File f1= FilePathParserUtils.getFile(suppliedFilePath, defaultFilePath, dateFormatter, timeFormatter, absoluteRootPath);
            f1.mkdirs();
            return f1;
        }
    }

    

    public static FileParser.FileParserBuilder getFileParserBuilder(){
        return new FileParser.FileParserBuilder()
                             .absoluteRootPath(getDefaultRootDir());
    }
    
    /**
     * Returns the File used to output the report
     *
     * @return
     */
    public static File getFile(String outputPath, String defaultPath, DateTimeFormatter formatter, DateTimeFormatter formatterTime, File rootPath) {
        if(outputPath ==null) {
            outputPath= defaultPath;
        }
        String date = formatter.format(TimeUtil.getCurrentLocalDateTime());
        String time = formatterTime.format(TimeUtil.getCurrentLocalDateTime());

        String fpath = outputPath.replace("%DATE%", date)
                                 .replace("%TIME%", time);
        File ff = new File(rootPath, fpath);
        return ff;
    }

    
    //TODO: the concept of the "root" running directory should be more lower-level config than this
    //class specified. Though this could serve as a backup
    private static File getDefaultRootDir() {
        GsrsAdminLogConfiguration logConf = StaticContextAccessor.getBean(GsrsAdminLogConfiguration.class);
        return logConf.getRootPath();
    }

}
