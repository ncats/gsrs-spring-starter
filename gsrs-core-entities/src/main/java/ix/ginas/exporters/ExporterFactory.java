package ix.ginas.exporters;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Function;

/**
 * Factory interface for making an {@link Exporter}
 * for entities.  The Factory can return
 * different implementations depending on the {@link Parameters}
 * passed in.
 *
 *
 * Created by katzelda on 8/23/16.
 */
public interface ExporterFactory<T> {
    /**
     * Configuration Parameters to tell the factory
     * what export options to use.
     */
    interface Parameters{

        OutputFormat getFormat();

        default boolean shouldCompress(){
            return false;
        }

        default boolean publicOnly(){ return false;}

        default JsonNode detailedParameters() {
            return JsonNodeFactory.instance.objectNode();
        }
        /* new stuff 19 August*/
/*
        default RecordScrubber getScrubber() {
            //not clear what's requested
            return null;
        }
*/

    }

    /**
     * Can This factory make an Exporter that meets
     * these Parameter requirements.
     * @param params the {@link Parameters} to consider.
     * @return {@code true} if it does support those parameters;
     *      {@code false otherwise}.
     */
    boolean supports(Parameters params);

    /**
     * Get all the {@link OutputFormat}s that this factory
     * can support.
     * @return a Set of {@link OutputFormat}s; should never be null,
     * @return a Set of {@link OutputFormat}s; should never be null,
     * but could be empty.
     */
    Set<OutputFormat> getSupportedFormats();

    /**
     * Create a new {@link Exporter} using the given {@link Parameters} that will
     * write the export data to the given {@link OutputStream}.
     *
     * @param out the {@link OutputStream} to write to.
     * @param params the {@link Parameters} configuration to tune the Exporter.  These {@link Parameters}
     *               should always be supported.
     *
     * @return a new Exporter; should never be null.
     *
     * @throws IOException if there is a problem creating the Exporter.
     *
     * @see #supports(Parameters)
     */
    Exporter<T> createNewExporter(OutputStream out, Parameters params) throws IOException;

    default JsonNode getSchema(){
        return generateSchemaNode(this.getClass().getSimpleName(), JsonNodeFactory.instance.objectNode());
    }

    default String generateSchema(String exporterName, ObjectNode schemaNode) {
        StringBuilder returnBuilder = new StringBuilder();
        returnBuilder.append("{\n");
        returnBuilder.append("$schema: \"https://json-schema.org/draft/2020-12/schema\",\n");
        returnBuilder.append("\"$id\": \"https://gsrs.ncats.nih.gov/#/export.scrubber.schema.json\",\n");
        returnBuilder.append("\"title\": \"Scrubber Parameters\",\n");
        returnBuilder.append("\"description\":\"");
        returnBuilder.append(exporterName);
        returnBuilder.append("\",\n");
        returnBuilder.append("\"type\": \"object\",\n");
        returnBuilder.append("\"properties\": {\n");
        List<String> fieldNames = new ArrayList<>();
        schemaNode.fieldNames().forEachRemaining(fieldNames::add);
        for(int i=0; i<fieldNames.size(); i++) {
            returnBuilder.append("{ \"");
            returnBuilder.append(fieldNames.get(i));
            returnBuilder.append("\": {\n");
            returnBuilder.append(" \"type\": ");
            returnBuilder.append(schemaNode.get(fieldNames.get(i)));
            returnBuilder.append("\n  }\n");
            returnBuilder.append(" }");
            if(i<fieldNames.size()-1){
                returnBuilder.append(",");
            }
            returnBuilder.append("\n");
        }
        //returnBuilder.append("");
        returnBuilder.append("  }\n");
        returnBuilder.append("}");
        return returnBuilder.toString();
    }

    default JsonNode generateSchemaNode(String exporterName, ObjectNode schemaNode) {
        ObjectNode outputNode = JsonNodeFactory.instance.objectNode();
        outputNode.put("$schema",  "https://json-schema.org/draft/2020-12/schema");
        outputNode.put("$id", "https://gsrs.ncats.nih.gov/#/export.scrubber.schema.json");
        outputNode.put("title", "Scrubber Parameters");
        outputNode.put("description", exporterName);
        outputNode.put("type", "object");

        ArrayNode propertiesNode = JsonNodeFactory.instance.arrayNode();
        List<String> fieldNames = new ArrayList<>();
        schemaNode.fieldNames().forEachRemaining(fn->{
            ObjectNode singlePropertyNode = JsonNodeFactory.instance.objectNode();
            ObjectNode typeNode = JsonNodeFactory.instance.objectNode();
            typeNode.set("type", schemaNode.get(fn));
            singlePropertyNode.set(fn, typeNode);
            propertiesNode.add(singlePropertyNode);
        });
        outputNode.set("properties", propertiesNode);

        return outputNode;
/*
        returnBuilder.append("\"properties\": {\n");

        for(int i=0; i<fieldNames.size(); i++) {
            returnBuilder.append("{ \"");
            returnBuilder.append(fieldNames.get(i));
            returnBuilder.append("\": {\n");
            returnBuilder.append(" \"type\": ");
            returnBuilder.append(schemaNode.get(fieldNames.get(i)));
            returnBuilder.append("\n  }\n");
            returnBuilder.append(" }");
            if(i<fieldNames.size()-1){
                returnBuilder.append(",");
            }
            returnBuilder.append("\n");
        }
        //returnBuilder.append("");
        returnBuilder.append("  }\n");
        returnBuilder.append("}");
        return returnBuilder.toString();
*/
    }
}
