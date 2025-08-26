package gsrs.startertests.exporters;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.startertests.GsrsJpaTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gsrs.controller.AbstractExportSupportingGsrsEntityController;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.service.GsrsEntityService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.search.SearchResult;
import ix.ginas.exporters.ExporterSpecificExportSettings;
import ix.ginas.exporters.GeneralExportSettings;
import ix.ginas.exporters.SpecificExporterSettings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ActiveProfiles("test")
@GsrsJpaTest( classes = { GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class})
public class AbstractExportSupportingGsrsEntityControllerTest extends AbstractGsrsJpaEntityJunit5Test {

	// This works locally but does not work on Travis-CI due to a JVM start/stop issue where
	// the annotation service seems to be closed by the time it gets here. There are workarounds
	// we've used in the past, but we may have to implement these later.
	@Disabled
    @Test
    public void testdoesExporterKeyExist() throws JsonProcessingException {
        AbstractExportSupportingGsrsEntityController controller = new AbstractExportSupportingGsrsEntityController() {
            @Override
            protected LegacyGsrsSearchService getlegacyGsrsSearchService() {
                return null;
            }

            @Override
            protected Object createSearchResponse(List results, SearchResult result, HttpServletRequest request) {
                return null;
            }
            
            @SneakyThrows
            @Override
            protected GsrsEntityService getEntityService() {
               GsrsEntityService entityService = (GsrsEntityService) mock(GsrsEntityService.class);
                when(entityService.getEntityClass()).thenReturn(ix.core.models.Text.class);
                return entityService;
            }
        };
        controller = AutowireHelper.getInstance().autowireAndProxy(controller);
        //create 2 configurations with identical keys
        String madeUpKey = "Made Up Key";
        String config1 = createBogusConfig(madeUpKey);
        Map<String, String> params = new HashMap<>();
        ResponseEntity response = controller.handleExportConfigSave(config1, params);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private String createBogusConfig(String expConfKey){
        ObjectMapper objectMapper = new ObjectMapper();

        ExporterSpecificExportSettings exporterSpecificExportSettings = ExporterSpecificExportSettings.builder()
                .columnNames(Arrays.asList("molfile", "UNII", "PT", "CAS"))
                .includeRepeatingDataOnEveryRow(false)
                .build();
        JsonNode exporterSettings = objectMapper.valueToTree(exporterSpecificExportSettings);
        GeneralExportSettings generalExportSettings = GeneralExportSettings.builder()
                .approvalIdCodeSystem("Universal Approval Code")
                .copyApprovalIdToCode(true)
                .newAbstractUser("someone")
                .removeApprovalId(true)
                .setAllAuditorsToAbstractUser(true)
                .build();
        SpecificExporterSettings config =  SpecificExporterSettings.builder()
                .exporterKey(expConfKey)
                .exporterSettings(exporterSettings)
                .configurationKey("Advanced SDFiles")
                .build();
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Error creating test config", e);
        }
        return "";
    }
}
