package gsrs.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("hasAnyRole('SuperDataEntry','Updater','SuperUpdate','Approver','Admin')")
public @interface hasSuperDataEntryRole {
}
