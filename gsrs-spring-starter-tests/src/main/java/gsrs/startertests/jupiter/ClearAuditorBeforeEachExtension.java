package gsrs.startertests.jupiter;


import gsrs.AuditConfig;
import ix.core.models.Principal;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;

/**
 * Junit 5 Extension that before each tests will clear the cache
 * of the auditor Bean.
 *
 * Use like this in your class:
 *
 * <pre>
 *{@code
 *
 *  @Import(ClearAuditorBeforeEachExtension.class)
 *  public class MyTest {
 *
 *     @RegisterExtension
 *     @Autowired
 *     public ClearAuditorBeforeEachExtension clearAuditorBeforeEachExtension;
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
public class ClearAuditorBeforeEachExtension implements BeforeEachCallback {

    @Autowired
    private AuditorAware<Principal> auditor;



    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        //TODO: this isn't always of this type ... there may have been
        // a change since the time this was originally written?
        if(auditor instanceof AuditConfig.SecurityAuditor) {
            ((AuditConfig.SecurityAuditor)auditor).clearCache();
        }
    }
}
