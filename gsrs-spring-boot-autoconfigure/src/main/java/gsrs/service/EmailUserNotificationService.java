package gsrs.service;

import gsrs.tasks.UserExpirationNotificationProperties;
import ix.core.models.UserMessage;
import ix.core.models.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailUserNotificationService implements UserNotificationService {
    private final JavaMailSender mailSender;
    private final UserExpirationNotificationProperties properties;

    @Override
    public void sendUserMessage(UserMessage userMessage) {
        if (userMessage.email().isBlank()) {
            log.warn("Cannot notify user {}: no email address",
                    userMessage.username());
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(properties.getFrom());
        mail.setTo(userMessage.email());
        mail.setSubject(userMessage.subject());
        mail.setText(userMessage.body());

        mailSender.send(mail);
    }

}
