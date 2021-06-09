package gsrs.startertests.junit4;

import gov.nih.ncats.common.io.IOUtil;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;

import java.io.File;

@TestComponent
public class ClearTextIndexerRule extends ExternalResource {
    @Value("${ix.home}")
    private String dir;

    @Override
    public void before() throws Exception {
        IOUtil.deleteRecursively(new File(dir));
    }
}
