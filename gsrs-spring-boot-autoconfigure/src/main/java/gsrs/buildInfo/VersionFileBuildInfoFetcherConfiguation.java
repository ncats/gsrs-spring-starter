package gsrs.buildInfo;

import gov.nih.ncats.common.io.IOUtil;
import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.common.util.TimeUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
@ConfigurationProperties("build")
@PropertySources({
        @PropertySource("classpath:gsrs.starter.version.txt"),
        @PropertySource(value = "classpath:version.txt", ignoreResourceNotFound = true)
})

@Data
public class VersionFileBuildInfoFetcherConfiguation {

    private String version;

    private String commit;

    private String time;


    public BuildInfo getBuildInfo() {
            LocalDateTime dateTime = LocalDateTime.parse(time,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz YYYY");
            return BuildInfo.builder()
                    .version(version)
                    .commit(commit)
                    .buildDate(DateTimeFormatter.ISO_DATE.format(dateTime))
                    //Mon Mar 01 17:11:08 EST 2021"
                    .buildTime(fmt.format(dateTime.atZone(ZoneId.systemDefault())))
                    .build();
        }
    }
