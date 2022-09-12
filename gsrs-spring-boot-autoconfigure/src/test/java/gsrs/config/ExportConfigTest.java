package gsrs.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.ginas.exporters.DefaultExporterFactoryConfig;
import ix.ginas.exporters.ExporterSpecificExportSettings;
import ix.ginas.exporters.GeneralExportSettings;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

@Slf4j
public class ExportConfigTest {

    @Test
    public void testConfiguration1() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        ExporterSpecificExportSettings exporterSpecificExportSettings = ExporterSpecificExportSettings.builder()
                .columnNames(Arrays.asList("SMILES", "PT", "UNII", "UUID"))
                .includeRepeatingDataOnEveryRow(false)
                .build();
        JsonNode exporterSettings = objectMapper.valueToTree(exporterSpecificExportSettings);
        GeneralExportSettings generalExportSettings = GeneralExportSettings.builder()
                .approvalIdCodeSystem("Universal Approval Code")
                .copyApprovalIdToCode(true)
                .newAbstractUser("registrar")
                .removeApprovalId(true)
                .setAllAuditorsToAbstractUser(true)
                .build();
        JsonNode generalSettings = objectMapper.valueToTree(generalExportSettings);
        DefaultExporterFactoryConfig config =  DefaultExporterFactoryConfig.builder()
                .exporterKey("SDF Exporter")
                .exporterSettings(exporterSettings)
                .generalSettings(generalSettings)
                .configurationKey("Basic SDFiles")
                .configurationId("1892")
                .build();
        ObjectMapper mapper = new ObjectMapper();

        String configString =mapper.writeValueAsString(config);
        System.out.println(configString);

        Assertions.assertTrue(configString.length()>0);
    }
}
