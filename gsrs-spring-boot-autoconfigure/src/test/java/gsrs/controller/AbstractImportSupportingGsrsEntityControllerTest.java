package gsrs.controller;

import gsrs.imports.DummyImportAdapterFactory;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.service.GsrsEntityService;
import ix.core.models.Payload;
import ix.core.search.SearchResult;
import ix.ginas.models.GinasCommonData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AbstractImportSupportingGsrsEntityControllerTest {

    AbstractImportSupportingGsrsEntityController controller = new AbstractImportSupportingGsrsEntityController() {
        @Override
        protected LegacyGsrsSearchService getlegacyGsrsSearchService() {
            return null;
        }

        @Override
        protected Object createSearchResponse(List results, SearchResult result, HttpServletRequest request) {
            return null;
        }

        @Override
        protected GsrsEntityService getEntityService() {
            return null;
        }
    };

    @Test
    void execute() {
        AbstractImportSupportingGsrsEntityController.ImportTaskMetaData<GinasCommonData> task = new AbstractImportSupportingGsrsEntityController.ImportTaskMetaData<>();
        task.setFileEncoding("UTF-9");
        task.setPayloadID(UUID.randomUUID());
        Map<String, String> settingsMap = new HashMap<>();
        AbstractImportSupportingGsrsEntityController controller1 = mock(AbstractImportSupportingGsrsEntityController.class);
        when(((AbstractImportSupportingGsrsEntityController<?, GinasCommonData, GinasCommonData>) controller1).fetchAdapterFactory(task)).thenReturn(new DummyImportAdapterFactory());
        settingsMap.put("")

    }

    @Test
    void from() {
        Payload p = new Payload();
        p.id= UUID.randomUUID();
        p.size=10001l;
        p.mimeType="text/plain";
        p.name  ="data1.txt";
        AbstractImportSupportingGsrsEntityController.ImportTaskMetaData metaData= controller.from(p);
        Assertions.assertEquals(metaData.getPayloadID(), p.id);
        Assertions.assertEquals(metaData.getMimeType(), p.mimeType);
        Assertions.assertEquals(metaData.getSize(), p.size);
        Assertions.assertEquals(metaData.getFilename(), p.name);
    }

    @Test
    void getImportAdapters() {
    }

    @Test
    void getImportAdapterNames() {
    }

    @Test
    void getConfiguredImportAdapters() {
    }

    @Test
    void getImportAdapterFactory() {
    }

    @Test
    void testGetImportAdapters() {
    }

    @Test
    void getSpecificImportAdapter() {
    }

    @Test
    void handleImport() {
    }

    @Test
    void getImport() {
    }

    @Test
    void getImportPredict() {
    }

    @Test
    void updateImport() {
    }

    @Test
    void executePreview() {
    }

    @Test
    void executeValidate() {
    }

    @Test
    void findMatches() {
    }

    @Test
    void deleteRecord() {
    }

    @Test
    void retrieveRecord() {
    }

    @Test
    void getInstances() {
    }

    @Test
    void retrieveInstanceData() {
    }

    @Test
    void updateImportData() {
    }

    @Test
    void executeImport() {
    }
}