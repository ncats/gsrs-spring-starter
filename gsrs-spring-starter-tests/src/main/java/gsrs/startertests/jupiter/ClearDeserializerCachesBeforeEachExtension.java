package gsrs.startertests.jupiter;


import gsrs.AuditConfig;
import gsrs.services.GroupService;
import gsrs.services.GroupServiceImpl;
import gsrs.services.PrincipalService;
import gsrs.services.PrincipalServiceImpl;
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
 *  @Import(ClearDeserializerCachesBeforeEachExtension.class)
 *  public class MyTest {
 *
 *     @RegisterExtension
 *     @Autowired
 *     public ClearDeserializerCachesBeforeEachExtension clearDeserializerCachesBeforeEachExtension;
 *
 *
 *}
 *
 *
 *
 * </pre>
 */
@TestComponent
@Import( {PrincipalServiceImpl.class, GroupServiceImpl.class})
public class ClearDeserializerCachesBeforeEachExtension implements BeforeEachCallback {

    @Autowired
    private PrincipalService principalService;

    @Autowired
    private GroupService groupService;


    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        principalService.clearCache();
        groupService.clearCache();
    }
}
