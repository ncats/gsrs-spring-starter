package gsrs.imports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.repository.TextRepository;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.model.MatchedRecordSummary;
import gsrs.stagingarea.service.StagingAreaService;
import ix.core.models.Text;
import ix.core.util.EntityUtils;
import ix.ginas.exporters.SpecificExporterSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ImportUtilities {

    public static void enhanceWithMetadata(ObjectNode dataNode, ImportMetadata metadata, StagingAreaService service) {
        ObjectMapper mapper = new ObjectMapper();
        if (metadata != null) {
            String metadataAsString = null;
            try {
                EntityUtils.EntityInfo<ImportMetadata> eics= EntityUtils.getEntityInfoFor(ImportMetadata.class);
                if (metadata.validations == null || metadata.validations.isEmpty()) {
                    log.trace("going to fill in validations");
                    service.fillCollectionsForMetadata(metadata);
                }
                metadataAsString = mapper.writeValueAsString(metadata);
                ImportMetadata copy = eics.fromJson(metadataAsString);
                log.trace("starting filtering lambda. copy has {} kvms", copy.getKeyValueMappings().size());
                copy.setKeyValueMappings(metadata.getKeyValueMappings().stream().filter(kv->!kv.getRecordId().equals(metadata.getRecordId())).collect(Collectors.toList()));
                log.trace("completed filtering lambda. copy has {} kvms", copy.getKeyValueMappings().size());
                JsonNode metadataAsNode = mapper.readTree(mapper.writeValueAsString(copy));
                dataNode.set("_metadata", metadataAsNode);

                MatchedRecordSummary matchedRecordSummary = service.findMatches(metadata);
                log.trace("computed matches");
                JsonNode matchedRecordsAsNode = mapper.readTree(mapper.writeValueAsString(matchedRecordSummary));
                dataNode.set("_matches", matchedRecordsAsNode);

            } catch (ClassNotFoundException | IOException e) {
                log.error("Error processing metadata", e);
                throw new RuntimeException(e);
            }

        } else {
            dataNode.put("_metadata", "[not found]");
        }
    }

    public static List<AbstractImportSupportingGsrsEntityController.ImportTaskMetaData> getAllImportTasks(Class entityClass, TextRepository textRepository) {
        log.trace("getAllImportTasks");
        List<AbstractImportSupportingGsrsEntityController.ImportTaskMetaData> allImportConfigs = new ArrayList<>();
        Objects.requireNonNull(entityClass, "Must be able to resolve the entity class");

        String label = AbstractImportSupportingGsrsEntityController.ImportTaskMetaData.getEntityKeyFromClass(entityClass.getName());
        List<Text> configs = textRepository.findByLabel(label);
        log.trace("total configs: {}", configs.size());
        configs.forEach(c -> {
            AbstractImportSupportingGsrsEntityController.ImportTaskMetaData config;
            try {
                config = AbstractImportSupportingGsrsEntityController.ImportTaskMetaData.fromText(c);
                allImportConfigs.add(config);
            } catch (JsonProcessingException e) {
                log.error("Error in getAllImportTasks", e);
            }
        });
        return allImportConfigs;
    }

    public static String removeMetadataFromDomainObjectJson(String domainObjectJson) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode objectAsNode = mapper.readTree(domainObjectJson);
            if (objectAsNode.hasNonNull("_metadata") && objectAsNode.isObject()) {
                ((ObjectNode) objectAsNode).remove("_metadata");
            }
            if (objectAsNode.hasNonNull("_matches") && objectAsNode.isObject()) {
                ((ObjectNode) objectAsNode).remove("_matches");
            }
            return objectAsNode.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Sort parseSortFromOrderParam(String order) {
        //match Gsrs Play API
        if (order == null || order.trim().isEmpty()) {
            return Sort.sort(ImportMetadata.class);
        }
        char firstChar = order.charAt(0);
        if ('$' == firstChar) {
            return Sort.by(Sort.Direction.DESC, order.substring(1));
        }
        if ('^' == firstChar) {
            return Sort.by(Sort.Direction.ASC, order.substring(1));
        }
        return Sort.by(Sort.Direction.ASC, order);
    }

    public static boolean doesImporterKeyExist(String importerId, Class entityClass, TextRepository textRepository) {
        log.trace("doesImporterKeyExist");
        Objects.requireNonNull(entityClass, "Must be able to resolve the entity class");

        String label = AbstractImportSupportingGsrsEntityController.ImportTaskMetaData.getEntityKeyFromClass(entityClass.getName());
        List<Text> configs = textRepository.findByLabel(label);
        log.trace("total configs: {}", configs.size());
        return configs.stream().anyMatch(c -> {
            SpecificExporterSettings config = null;
            try {
                config = SpecificExporterSettings.fromText(c);
            } catch (JsonProcessingException e) {
                log.error("Error", e);
            }
            return config.getExporterKey() != null && config.getExporterKey().equalsIgnoreCase(importerId);
        });
    }

}
