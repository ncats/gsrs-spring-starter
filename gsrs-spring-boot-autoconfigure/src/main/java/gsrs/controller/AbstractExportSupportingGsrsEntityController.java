package gsrs.controller;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.repository.TextRepository;
import gsrs.security.hasAdminRole;
import ix.core.models.Text;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Objects;

@Slf4j
public abstract class AbstractExportSupportingGsrsEntityController<C extends AbstractExportSupportingGsrsEntityController, T, I>
        extends AbstractLegacyTextSearchGsrsEntityController<C, T, I>{

    @Autowired
    private TextRepository textRepository;

    @Autowired
    protected PlatformTransactionManager transactionManager;


    //ExporterFactoryConfig
    @PostGsrsRestApiMapping("/export/setup")
    public ResponseEntity<Object> handleExportConfigSave(@RequestParam("exportConfig") String exportConfigJson,
                                                         @RequestParam Map<String, String> queryParameters) {
        log.trace("starting in handleExportConfigSave");
        Objects.requireNonNull(queryParameters.get("configurationName"), "input parameters must include a configurationName!");
        Text textObject = new Text(queryParameters.get("configurationName"), exportConfigJson);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Text savedText = transactionTemplate.execute(t-> textRepository.saveAndFlush(textObject));
        log.trace("completed save of Text");

        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        resultNode.put("Newly created configuration", savedText.id);
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.OK);
    }

}
