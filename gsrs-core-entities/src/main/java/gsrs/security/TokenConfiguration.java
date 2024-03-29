package gsrs.security;

import gov.nih.ncats.common.util.TimeUtil;
import ix.utils.Util;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gsrs.tokens")
@Data
public class TokenConfiguration {

    private long timeResolutionMS = 86400000; // 3600L*1000L*24L;

    public long getCanonicalCacheTimeStamp(){
        long TIMESTAMP= TimeUtil.getCurrentTimeMillis();
        return (long) Math.floor(TIMESTAMP/getTimeResolutionMS());
    }

    public String getComputedToken(String username, String key) {
        if(key==null)return null;
        String date = "" + getCanonicalCacheTimeStamp();
        return Util.sha1(date + username + key);
    }
}
