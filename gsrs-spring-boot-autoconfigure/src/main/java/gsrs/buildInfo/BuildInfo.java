package gsrs.buildInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BuildInfo {
        private String version;
        private String commit;
        private String buildDate;
        private String buildTime;
}
