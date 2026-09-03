package ix.core.controllers;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.GeneratorSettings;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import gov.nih.ncats.common.functions.ThrowableConsumer;
import gov.nih.ncats.common.functions.ThrowableFunction;
import ix.core.models.BeanViews;
import ix.core.util.EntityUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Optional;


public class EntityFactory {
    private static final String RESPONSE_TYPE_PARAMETER = "type";

    @Slf4j
    static public class EntityMapper {
        private boolean keyOnly=false;

        private ObjectMapper objectMapperNew;

        private SerializationConfig _serializationConfig;
        private DeserializationConfig _deserializationConfig;

        private JsonMapper jsonMapper;
        /**
         * Default value
         */
        private static final long serialVersionUID = 1L;

        public static EntityMapper FULL_ENTITY_MAPPER(){
            EntityMapper mapper = new EntityMapper(BeanViews.Full.class);
            mapper.jsonMapper = JsonMapper.builder()
                    .addHandler(new DeserializationProblemHandler() {
                        @Override
                        public boolean handleUnknownProperty(
                                DeserializationContext context,
                                JsonParser parser,
                                ValueDeserializer<?> deserializer,
                                Object beanOrClass,
                                String propertyName) {

                            return this.handleUnknownProperty(
                                    context,
                                    parser,
                                    deserializer,
                                    beanOrClass,
                                    propertyName
                            );
                        }
                    })
                    //todo: add confi
                    .build();
            return mapper;
        }

        public static EntityMapper JSON_DIFF_ENTITY_MAPPER(){
            EntityMapper mapper= new EntityMapper(BeanViews.JsonDiff.class);
            mapper.jsonMapper = JsonMapper.builder()
                    //todo: add confi
                    .build();
            return mapper;
        }

        public static EntityMapper INTERNAL_ENTITY_MAPPER(){
            EntityMapper mapper = new EntityMapper(BeanViews.Internal.class);
            mapper.jsonMapper = JsonMapper.builder()
                    //todo: add confi
                    .build();
            return mapper;
        }

        public static EntityMapper COMPACT_ENTITY_MAPPER() {
            EntityMapper mapper = new EntityMapper(BeanViews.Compact.class);
            mapper.jsonMapper = JsonMapper.builder()
                    //todo: add confi
                    .build();
            return mapper;
        }

        public static EntityMapper KEY_ENTITY_MAPPER() {
            EntityMapper mapper = new EntityMapper(BeanViews.Key.class);
            mapper.jsonMapper = JsonMapper.builder()
                    //todo: add confi
                    .build();
            return mapper;
        }

        public static EntityMapper getByView(String view){
            if(view ==null){
                return COMPACT_ENTITY_MAPPER();
            }
            if(view.equalsIgnoreCase("full")){
                return FULL_ENTITY_MAPPER();
            }
            if(view.equalsIgnoreCase("internal")){
                return INTERNAL_ENTITY_MAPPER();
            }
            if(view.equalsIgnoreCase("key")){
                return KEY_ENTITY_MAPPER();
            }
            //JsonDiff
            if(view.equalsIgnoreCase("jsondiff")){
                return JSON_DIFF_ENTITY_MAPPER();
            }
            return COMPACT_ENTITY_MAPPER();
        }

        //@Override
        public EntityMapper copy() {
            Class<?> view = _serializationConfig.getActiveView();
            if(view ==null){
                return new EntityMapper();
            }
            return new EntityMapper(view);
        }

        public EntityMapper (Class<?>... views) {
            EntityMapper mapper = new EntityMapper();
            mapper.jsonMapper = JsonMapper.builder()
                    .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                    .changeDefaultPropertyInclusion(inclusion ->
                            inclusion.withContentInclusion(JsonInclude.Include.NON_NULL)
                    )
                    .build();

            //configure (MapperFeature.DEFAULT_VIEW_INCLUSION, true);
            //configure (SerializationFeature.WRITE_NULL_MAP_VALUES, false);
            /*this.setSerializationInclusion(JsonInclude.Include.NON_NULL);*/
            _serializationConfig = mapper.jsonMapper.serializationConfig();

            for (Class<?> v : views) {
                if(v.equals(BeanViews.Key.class)){
                    keyOnly=true;
                }
                _serializationConfig = _serializationConfig.withView(v);
            }

            SimpleModule module = new SimpleModule();

        }


        public EntityMapper () {
        }

        public boolean _handleUnknownProperty
                (DeserializationContext ctx, JsonParser parser,
                 JsonDeserializer deser, Object bean, String property) {
            try {
                /*
            	Logger.warn("Unknown property \""
                            +property+"\" (token="
                            +parser.getCurrentToken()
                            +") while parsing "
                            +bean+"; skipping it..");
                            */
                parser.skipChildren();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                log.error
                        ("Unable to handle unknown property!", ex);
                return false;
            }
            return true;
        }

        protected ObjectWriter _newWriter(SerializationConfig config) {
            if(this.keyOnly){
                return new KeyOnlyObjectWriter(this, config);
            }
            return super._newWriter(config);
        }


        public String toJson (Object obj) {
            return toJson (obj, false);
        }

        public String toJson (Object obj, boolean pretty) {
            if(this.keyOnly){
                Optional<EntityUtils.Key> optKey= EntityUtils.EntityWrapper.of(obj).getOptionalKey();
                if(optKey.isPresent()) {
                    EntityUtils.Key k = optKey.get();
                    try {
                        return pretty
                                ? writerWithDefaultPrettyPrinter().writeValueAsString(k)
                                : writeValueAsString(k);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        log.trace("Can't write Json", ex);
                    }
                }
            }


            try {
                return pretty
                        ? writerWithDefaultPrettyPrinter().writeValueAsString(obj)
                        : writeValueAsString (obj);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                log.trace("Can't write Json", ex);
            }
            return null;
        }

        public JsonNode toJsonNode(Object obj) {
            if(this.keyOnly){
                Optional<EntityUtils.Key> optKey= EntityUtils.EntityWrapper.of(obj).getOptionalKey();
                if(optKey.isPresent()) {
                    EntityUtils.Key k = optKey.get();
                    try {
                        return this.jsonMapper.valueToTree(k);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        log.trace("Can't write Json", ex);
                    }
                }
            }


            try {
                return this.jsonMapper.valueToTree(obj);
            }catch (Exception ex) {
                ex.printStackTrace();
                log.trace("Can't write Json", ex);
            }
            return null;

        }

        private class KeyOnlyObjectWriter {

            private JsonMapper mapper;

            public KeyOnlyObjectWriter(ObjectMapper mapper, SerializationConfig config, JavaType rootType, PrettyPrinter pp) {
                mapper = JsonMapper.builder();
                        .configure(config.gets)
                super(mapper, config, rootType, pp);
            }

            public KeyOnlyObjectWriter(ObjectMapper mapper, SerializationConfig config) {
                super(mapper, config);
            }

            public KeyOnlyObjectWriter(ObjectMapper mapper, SerializationConfig config, FormatSchema s) {
                super(mapper, config, s);
            }

            public KeyOnlyObjectWriter(ObjectWriter base, SerializationConfig config, GeneratorSettings genSettings, Prefetch prefetch) {
                super(base, config, genSettings, prefetch);
            }

            public KeyOnlyObjectWriter(ObjectWriter base, SerializationConfig config) {
                super(base, config);
            }

            public KeyOnlyObjectWriter(ObjectWriter base, JsonFactory f) {
                super(base, f);
            }

            public KeyOnlyObjectWriter() {
                super(EntityMapper.this, EntityMapper.this._serializationConfig);

            }

            @Override
            protected ObjectWriter _new(ObjectWriter base, JsonFactory f) {
                return new KeyOnlyObjectWriter(base, f);
            }

            @Override
            protected ObjectWriter _new(ObjectWriter base, SerializationConfig config) {
                return new KeyOnlyObjectWriter(base, config);
            }

            @Override
            protected ObjectWriter _new(GeneratorSettings genSettings, Prefetch prefetch) {

                return new KeyOnlyObjectWriter(this, this._config, genSettings, prefetch);
            }

            @Override
            protected SequenceWriter _newSequenceWriter(boolean wrapInArray, JsonGenerator gen, boolean managedInput) throws IOException {
                return super._newSequenceWriter(wrapInArray, gen, managedInput);
            }



            public void writeValue(File resultFile, Object value) throws IOException, JsonGenerationException, JsonMappingException {
                writeValueConsumer( v->super.writeValue(resultFile, v), value);
            }

            private <E extends Throwable> void writeValueConsumer(ThrowableConsumer<Object, E> consumer, Object value) throws E{
                if(EntityMapper.this.keyOnly){
                    EntityUtils.EntityWrapper<Object> ew = EntityUtils.EntityWrapper.of(value);
                    if(ew.hasIdField() && ew.getEntityInfo().isCollapsibleInKeyView()) {
                        Optional<EntityUtils.Key> opt = ew.getOptionalKey();
                        //we could be serializing an entity without an id set because it wasn't saved
                        //so this checks for that if there isn't a key it will fallback to serializing whole thing
                        if(opt.isPresent()) {
                            consumer.accept(opt.get());
                            return;
                        }
                    }
                }
               consumer.accept(value);
            }
            private <T, E extends Throwable> T writeValueFunction(ThrowableFunction<Object, T, E> consumer, Object value) throws E{
                if(EntityMapper.this.keyOnly){
                    EntityUtils.EntityWrapper<Object> ew = EntityUtils.EntityWrapper.of(value);
                    if(ew.hasIdField()) {
                        return consumer.apply(ew.getKey());

                    }
                }
                return consumer.apply(value);
            }
            @Override
            public void writeValue(JsonGenerator g, Object value) throws IOException {
                writeValueConsumer( v->super.writeValue(g, v), value);

            }

            public void writeValue(OutputStream out, Object value) throws IOException, JsonGenerationException, JsonMappingException {
                writeValueConsumer( v->super.writeValue(out, v), value);
            }

            public void writeValue(Writer w, Object value) throws IOException, JsonGenerationException, JsonMappingException {
                writeValueConsumer( v->super.writeValue(w, v), value);
            }

            public void writeValue(DataOutput out, Object value) throws IOException {
                writeValueConsumer( v->super.writeValue(out, v), value);
            }

            public String writeValueAsString(Object value) throws JsonProcessingException {
                return writeValueFunction( v-> super.writeValueAsString(v), value);

            }
        }
    }


    public static enum RESPONSE_TYPE{
    	FULL,
    	MESSAGES
    }



}
 
