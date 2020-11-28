package gsrs.startertests.junit4;

import gsrs.AuditConfig;
import ix.core.models.Principal;
import org.junit.rules.ExternalResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
/**
 * Junit 4 Rule that before each tests will clear the cache
 * of the auditor Bean.
 *
 * Use like this in your class:
 *
 * <pre>
 *{@code
 *
 *  @Import(ClearAuditorRule.class)
 *  public class MyTest {
 *
 *     @Rule
 *     @Autowired
 *     public ClearAuditorRule clearAuditorRule;
 *
 *
 *}
 *
 *
 *
 * </pre>
 */
@TestComponent
@Import( AuditConfig.class)
public class ClearAuditorRule extends ExternalResource {
    @Autowired
    private AuditorAware<Principal> auditor;

    @Override
    protected void before() throws Throwable {
        ((AuditConfig.SecurityAuditor)auditor).clearCache();
    }
}
