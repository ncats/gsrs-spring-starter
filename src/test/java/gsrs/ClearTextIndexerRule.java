package gsrs;

import gov.nih.ncats.common.io.IOUtil;
import ix.core.search.text.TextIndexerFactory;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;

import java.io.File;

@TestComponent
public class ClearTextIndexerRule implements BeforeEachCallback {
    @Value("${ix.home}")
    private String dir;

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        IOUtil.deleteRecursively(new File(dir));
    }
}
