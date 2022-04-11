package gsrs.imports;

import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.validator.ValidatorConfig;
import ix.ginas.utils.validation.ValidatorPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public interface GsrsImportAdapterFactory {

    ConfigBasedGsrsImportAdapterFactory newFactory(String context);
}