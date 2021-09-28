package gsrs.controller;

import gov.nih.ncats.common.util.CachedSupplier;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;

@Configuration
@Data
public class GsrsAdminLogConfiguration {
    @Value("${admin.panel.download.folderBlackList:}")
    private Set<String> folderBlackList = new HashSet<>();

    private File rootPath = new File(".");

    private String logPath = "logs";

    public List<LogFileInfo> getAllFiles() throws IOException{
        return getLogFilesFor(rootPath.toPath());
    }
    public List<LogFileInfo> getLogFilesFor(String relativeDir) throws IOException{
        return getLogFilesFor(new File(rootPath, relativeDir).toPath());
    }
    public List<LogFileInfo> getLogFilesFor(Path directory) throws IOException {
        Predicate<Path> shouldInclude = p->{
            String relativePath = rootPath.toPath().toAbsolutePath().relativize(p.normalize().toAbsolutePath()).toString();
            return !folderBlackList.contains(relativePath);
        };
        LogFileWalker visitor = new LogFileWalker(directory, shouldInclude);
        Files.walkFileTree(directory, visitor);

        Collections.sort(visitor.fileInfoList);
        return visitor.fileInfoList;
    }

    public static class LogFileInfo implements Comparable<LogFileInfo>{
        public String id;
        public String parent;
        public String text;
        public boolean isDir;

        @Override
        public int compareTo(LogFileInfo o) {
            if(isDir && !o.isDir){
                return -1;
            }
            if(o.isDir && !isDir){
                return 1;
            }

            return  id.compareTo(o.id);

        }
    }
    private static class LogFileWalker extends SimpleFileVisitor<Path> {


        private final Path directory;

        List<LogFileInfo> fileInfoList = new ArrayList<>();

        private final Predicate<Path> filter;

        public LogFileWalker(Path directory, Predicate<Path> filter) {
            this.directory = directory;
            this.filter= filter;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

            if(filter.test(dir)) {
                addToFileInfoList(dir);
                return FileVisitResult.CONTINUE;
            }
            return FileVisitResult.SKIP_SUBTREE;
        }

        public void addToFileInfoList(Path p) {
            File f = p.toFile();
            String relativePath = directory.relativize(p).toString();

            if(relativePath.isEmpty()){
                return;
            }
            LogFileInfo info = new LogFileInfo();
            info.id = relativePath;
            info.isDir = f.isDirectory();
            if(f.isDirectory()) {
                info.text = relativePath;
            }else{
                info.text = relativePath + " ( " + f.length() + " B)";
            }

            String parentRelativePath = directory.relativize(p.getParent()).toString();

            info.parent = parentRelativePath.isEmpty()? "#" : parentRelativePath;

            fileInfoList.add(info);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            addToFileInfoList(file);
            return FileVisitResult.CONTINUE;
        }


    }

}
