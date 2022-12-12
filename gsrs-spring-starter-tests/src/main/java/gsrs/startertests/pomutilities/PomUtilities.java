package gsrs.startertests.pomutilities;

import java.io.*;

import org.apache.maven.model.Dependency;
import 	org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PomUtilities {

    public static Properties readPomVersionProperties(String path) throws IOException {
        InputStream input = Files.newInputStream(Paths.get(path));
        Properties properties = new Properties();
        properties.load(input);
        return properties;
    }

    public static String readTextFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static Model readPomToModel(String pomPath) throws IOException, XmlPullParserException {
        Model model;
        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        Reader reader;
        File pomFile = new File(pomPath);
        reader = new FileReader(pomFile);
        model = mavenReader.read(reader);
        model.setPomFile(pomFile);
        return model;
    }

    public static String makeJarFilename(Dependency dependency)  {
        return String.format("%s-%s.jar",
        dependency.getArtifactId(),
        dependency.getVersion());
    }


}
