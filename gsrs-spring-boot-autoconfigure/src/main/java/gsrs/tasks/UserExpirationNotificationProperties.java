package gsrs.tasks;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gsrs.user-expiration-notification")
@Data
public class UserExpirationNotificationProperties {
    private boolean enabled = true;
    private String from;
    private String subject = "Your GSRS account will expire soon";
    private String gsrsUrl;
    private int notifyAfterDays = 50;
    private int inactiveAfterDays = 60;
}
