package gsrs.startertests;


import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class that autoregisters some GSRS Junit 5 Extensions (what Junit 4 called "Rules")
 * such as {@link ClearTextIndexerRule} and {@link ClearAuditorRule}.
 */
public abstract class AbstractGsrsJpaEntityJunit5Test {




    @Autowired
    @RegisterExtension
    protected ClearTextIndexerRule clearTextIndexerRule;

    @Autowired
    @RegisterExtension
    protected ClearAuditorRule clearAuditorRule;
}
