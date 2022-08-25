package gsrs.startertests.exporters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.ginas.exporters.ExporterFactoryConfig;
import ix.ginas.exporters.ExporterSpecificExportSettings;
import ix.ginas.exporters.GeneralExportSettings;
import ix.ginas.exporters.ScrubberExportSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class ExporterConfigTests {

    @Test
    public void testBasicConfig1() throws JsonProcessingException {
        ExporterFactoryConfig config = new BasicExporterConfig();
        config.setRecordScrubber( BasicScrubber.class);
        config.setFileName("d:\\temp\\basic.sdf");
        ExporterSpecificExportSettings exportSettings = new ExporterSpecificExportSettings();
        List<String> columns = Arrays.asList("PT","UUID", "SMILES");
        exportSettings.setColumnNames(columns);
        Map<String, Object> innerSettings = new HashMap<>();
        innerSettings.put("version", "V2000");
        innerSettings.put("maxCharge", 4);
        config.setExporterSettings(exportSettings);
        exportSettings.setParameters(innerSettings);

        GeneralExportSettings generalExportSettings = new GeneralExportSettings();
        generalExportSettings.setApprovalIdCodeSystem("UNII");
        generalExportSettings.setCopyApprovalIdToCode(true);
        generalExportSettings.setRemoveUuids(false);
        generalExportSettings.setGenerateNewUuids(false);
        config.setGeneralSettings(generalExportSettings);

        ScrubberExportSettings scrubberExportSettings = new ScrubberExportSettings();
        scrubberExportSettings.setOnlyPublic(false);
        scrubberExportSettings.setProhibitedGroups(Arrays.asList("top-secret"));
        config.setScrubberSettings(scrubberExportSettings);
        config.getSupportedFileExtensions();

        ObjectMapper om = new ObjectMapper();
        String configurationString= om.writeValueAsString(config);
        System.out.println(configurationString);
        Assertions.assertTrue(configurationString.length()>0);
    }
}
