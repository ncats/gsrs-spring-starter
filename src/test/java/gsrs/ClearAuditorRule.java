package gsrs;


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
public class ClearAuditorRule implements BeforeEachCallback {

    @Autowired
    private AuditorAware<Principal> auditor;



    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        ((AuditConfig.SecurityAuditor)auditor).clearCache();
    }
}
