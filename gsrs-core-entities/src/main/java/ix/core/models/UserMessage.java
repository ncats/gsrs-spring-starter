package ix.core.models;

import java.util.Objects;

public record UserMessage(
        String username,
        String email,
        String subject,
        String body
) {
    public UserMessage {
        Objects.requireNonNull(username, "username is required");
        Objects.requireNonNull(email, "email is required");
        Objects.requireNonNull(subject, "subject is required");
        Objects.requireNonNull(body, "body is required");
    }
}
