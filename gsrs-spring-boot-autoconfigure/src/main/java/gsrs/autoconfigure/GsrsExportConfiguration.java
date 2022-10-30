package gsrs.autoconfigure;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Text;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.exporters.SpecificExporterSettings;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Configuration
@ConfigurationProperties("ix.ginas.export")
@Data
@Slf4j
public class GsrsExportConfiguration {

    private File path = new File("./exports");
    private File tmpDir=null;

    //Parsed from config
    
    private Map<String, List<Class>> factories;//legacy as of 3.0.3+
    private Map<String, List<ExporterFactoryConfig>> exporterFactories; //new/preferred for 3.0.3+
    
    private Map<String, Map<String,SpecificExporterSettings>> settingsPresets;
    
    private Map<String, ExpanderFactoryConfig> expanderFactory; //placeholder, not used yet
    private Map<String, ScrubberFactoryConfig> scrubberFactory; //placeholder, not used yet

    
    private Map<String, Map<String, OutputFormat>> extensionMap = new LinkedHashMap<>();
    
    
    //Used after parsed from config
    
    @JsonIgnore
    private Map<String, List<ExporterFactory>> exporters = new HashMap<>();

    @JsonIgnore
    private Map<String, List<Text>> settingsPresetsAsText = new HashMap<>();


    CachedSupplier initializer = CachedSupplier.ofInitializer( ()->{
        log.trace("inside initializer");
        //normally, classes are autowired by the calling class but we think this was put here to work around an
        //  issue.  May remove this in the future.
        AutowireHelper.getInstance().autowire(GsrsExportConfiguration.this);
        if(factories !=null) {
            //legacy stuff
            for (Map.Entry<String, List<Class>> entry : factories.entrySet()) {
                String context = entry.getKey();
                List<ExporterFactory> list = new ArrayList<>();
                for (Class c : entry.getValue()) {
                    try {
                        log.trace("going to instantiate {}", c.getName());
                        ExporterFactory factory = (ExporterFactory) c.getConstructor().newInstance();

                        list.add(AutowireHelper.getInstance().autowireAndProxy(factory));
                    } catch (Exception e) {
                        //todo: log the exception
                        log.error("Error parsing export configuration", e);
                    }
                }
                exporters.put(context, list);
            }
        }
        ObjectMapper mapper = new ObjectMapper();

        if( exporterFactories!=null) {
            log.trace("handling exporterFactories");
            for (Map.Entry<String, List<ExporterFactoryConfig>> entryFull : exporterFactories.entrySet()) {
                String context = entryFull.getKey();//this is the type
                log.trace("context: {}", context);

                List<ExporterFactory> expList = exporters.computeIfAbsent(context, k -> new ArrayList<>());
                entryFull.getValue().forEach(exConf -> {
                    log.trace("exConf.getExporterFactoryClass(): {}", exConf.getExporterFactoryClass().getName());
                    ExporterFactory exporterFac = null;
                    if (exConf.getParameters() != null) {
                        exporterFac = (ExporterFactory) mapper.convertValue(exConf.getParameters(), exConf.getExporterFactoryClass());
                    } else {
                        try {
                            exporterFac = (ExporterFactory) exConf.getExporterFactoryClass().getConstructor().newInstance();
                        } catch (Exception e) {
                            throw new IllegalStateException("error creating exporter for " + exConf, e);
                        }
                    }
                    if (exporterFac != null) {

                        log.trace("exporter factory instantiated fine");
                        exporterFac = AutowireHelper.getInstance().autowireAndProxy(exporterFac);
                        expList.add(exporterFac);
                    } else {
                        log.warn("exporter factory null!");
                    }
                });
            }
        } else {
            log.warn("exporterFactories null");
        }
        
        //
        if(settingsPresets!=null) {
        	
        	settingsPresets.forEach((cont,m)->{
        		long[] id = new long[] {0};
        		List<Text> items = new ArrayList<>();
        		m.forEach((presetName,setting)->{
        			setting.setExporterKey(presetName);
        			setting.setOwner("admin");
        			//TODO: we should probably set some other properties too, may need to set the class
        			
                    Text allItems;
					try {
						allItems = new Text("settings", mapper.writeValueAsString(setting));
	                    allItems.id=id[0]--;
	                    items.add(allItems);
					} catch (JsonProcessingException e) {
						log.warn("Trouble creating export settings preset", e);
					}
        		});

    			settingsPresetsAsText.put(cont, items);
        	});        	
        }
        
    });


    /**
     * Get all the Supported {@link OutputFormat}s
     * by this plugin.
     * @return a Set of {@link OutputFormat}s, may be an
     * empty set if no exporters are found.
     */
    public Set<OutputFormat> getAllSupportedFormats(String context){

        initializer.get();

        //This mess with reverse iterating and then reversing again
        //is because we want the exporters listed first in the config file
        //to have priority over later listed exporters.  So if an earlier
        //exporter has the same extension as a later exporter, the earlier one should
        //have priority.
        //
        //So we go through the list backwards so earlier exporter's extensions
        //overwrite later exporters
        //
        //But then we have to reverse the list again so
        //the final display order matches the input list order.


        List<OutputFormat> list = getAllOutputsAsList(exporters.get(context));
        //go in reverse order to prefer the factories listed first
        ListIterator<OutputFormat> iterator = list.listIterator(list.size());
        Set<OutputFormat> set = new LinkedHashSet<>();
        while(iterator.hasPrevious()){
            set.add(iterator.previous());
        }
        //reverse it again
        List<OutputFormat> resortList = new ArrayList<>(set.size());

        for(OutputFormat f : set){
            resortList.add(f);
        }
        Collections.reverse(resortList);


        return new LinkedHashSet<>(resortList);
    }

    private List<OutputFormat> getAllOutputsAsList(List<ExporterFactory> exporters) {

        List<OutputFormat> list = new ArrayList<>();
        if(exporters !=null) {
            for (ExporterFactory factory : exporters) {
                Set<OutputFormat> supportedFormats= factory.getSupportedFormats();
                log.trace("enhancing output formats");
                supportedFormats.forEach(f->f.setParameterSchema(factory.getSchema()));
                list.addAll(supportedFormats);
            }
        }
        return list;
    }

    /**
     * Get the {@link ExporterFactory} for the given {@link ix.ginas.exporters.ExporterFactory.Parameters}.
     * @param params the parameters to use to find the Exporter.
     * @return a {@link ExporterFactory} or {@code null} if no exporter is found that supports
     * those parameter configurations.
     *
     * @throws NullPointerException if params is null.
     */
    public ExporterFactory getExporterFor(String context, ExporterFactory.Parameters params){
        Objects.requireNonNull(params);
        initializer.get();
        List<ExporterFactory> contextExporters = exporters.get(context);
        if(contextExporters !=null) {
            for (ExporterFactory factory : contextExporters) {
                if (factory.supports(params)) {
                    return factory;
                }
            }
        }
        return null;
    }
    
    
    
    public List<Text> getHardcodedDefaultExportPresets(String context){
    	 initializer.get();
    	return settingsPresetsAsText.get(context);
    }


}
