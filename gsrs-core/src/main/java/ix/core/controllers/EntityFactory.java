package ix.core.controllers;


import com.fasterxml.jackson.annotation.JsonInclude;
import ix.core.interfaces.GsrsJsonMapper;
import tools.jackson.core.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import ix.core.models.BeanViews;
import ix.core.util.EntityUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Objects;


public class EntityFactory {
    private static final String RESPONSE_TYPE_PARAMETER = "type";

    @Slf4j
    static public class EntityMapper implements GsrsJsonMapper, Serializable {
        /**
         * Default value
         */
        private static final long serialVersionUID = 1L;

        private static final DeserializationProblemHandler UNKNOWN_PROPERTY_HANDLER =
                new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(
                            DeserializationContext context,
                            JsonParser parser,
                            ValueDeserializer<?> deserializer,
                            Object beanOrClass,
                            String propertyName) {

                        return skipUnknownProperty(parser, propertyName);
                    }
                };

        private static final JsonMapper DEFAULT_JSON_MAPPER = newDefaultJsonMapper();
        private static final JsonMapper JSON_DIFF_JSON_MAPPER = newJsonDiffJsonMapper();

        private final JsonMapper jsonMapper;
        private final Class<?> activeView;
        private final boolean keyOnly;

        public static EntityMapper FULL_ENTITY_MAPPER(){
            return new EntityMapper(DEFAULT_JSON_MAPPER, BeanViews.Full.class);
        }

        public static EntityMapper JSON_DIFF_ENTITY_MAPPER(){
            return new EntityMapper(JSON_DIFF_JSON_MAPPER, BeanViews.JsonDiff.class);
        }

        public static EntityMapper INTERNAL_ENTITY_MAPPER(){
            return new EntityMapper(DEFAULT_JSON_MAPPER, BeanViews.Internal.class);
        }

        public static EntityMapper COMPACT_ENTITY_MAPPER() {
            return new EntityMapper(DEFAULT_JSON_MAPPER, BeanViews.Compact.class);
        }

        public static EntityMapper KEY_ENTITY_MAPPER() {
            return new EntityMapper(DEFAULT_JSON_MAPPER, BeanViews.Key.class);
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

        public EntityMapper copy() {
            return new EntityMapper(jsonMapper, activeView, keyOnly);
        }

        public EntityMapper (Class<?>... views) {
            this(DEFAULT_JSON_MAPPER, views);
        }


        public EntityMapper () {
            this(DEFAULT_JSON_MAPPER, (Class<?>) null);
        }

        private EntityMapper(JsonMapper jsonMapper, Class<?>... views) {
            this(jsonMapper, resolveActiveView(views), hasKeyView(views));
        }

        private EntityMapper(JsonMapper jsonMapper, Class<?> activeView, boolean keyOnly) {
            this.jsonMapper = Objects.requireNonNull(jsonMapper);
            this.activeView = activeView;
            this.keyOnly = keyOnly;
        }

        public JsonMapper jsonMapper() {
            return jsonMapper;
        }

        public JsonMapper getJsonMapper() {
            return jsonMapper;
        }

        public Class<?> getActiveView() {
            return activeView;
        }

        public boolean isKeyOnly() {
            return keyOnly;
        }

        public boolean _handleUnknownProperty
                (DeserializationContext ctx, JsonParser parser,
                 ValueDeserializer<?> deser, Object bean, String property) {
            return skipUnknownProperty(parser, property);
        }


        public String toJson (Object obj) {
            return toJson (obj, false);
        }

        public String toJson (Object obj, boolean pretty) {
            try {
                return writer(pretty).writeValueAsString(obj);
            }
            catch (Exception ex) {
                log.trace("Can't write Json", ex);
            }
            return null;
        }

        public JsonNode toJsonNode(Object obj) {
            try {
                return valueToTree(obj);
            }catch (Exception ex) {
                log.trace("Can't write Json", ex);
            }
            return null;

        }

        public JsonNode valueToTree(Object value) {
            return rawWriter(false).valueToTree(valueForSerialization(value));
        }

        public <T> T treeToValue(JsonNode value, Class<T> valueType) {
            return readerFor(valueType).readValue(value);
        }

        public <T> T treeToValue(JsonNode value, TypeReference<T> valueTypeRef) {
            return readerFor(valueTypeRef).readValue(value);
        }

        public <T> T readValue(String content, Class<T> valueType) {
            return readerFor(valueType).readValue(content);
        }

        public <T> T readValue(String content, TypeReference<T> valueTypeRef) {
            return readerFor(valueTypeRef).readValue(content);
        }

        public <T> T readValue(byte[] src, Class<T> valueType) {
            return readerFor(valueType).readValue(src);
        }

        public <T> T readValue(byte[] src, TypeReference<T> valueTypeRef) {
            return readerFor(valueTypeRef).readValue(src);
        }

        public <T> T readValue(File src, Class<T> valueType) {
            return readerFor(valueType).readValue(src);
        }

        public <T> T readValue(File src, TypeReference<T> valueTypeRef) {
            return readerFor(valueTypeRef).readValue(src);
        }

        public <T> T readValue(Path src, Class<T> valueType) {
            return readerFor(valueType).readValue(src);
        }

        public <T> T readValue(InputStream src, Class<T> valueType) {
            return readerFor(valueType).readValue(src);
        }

        public <T> T readValue(Reader src, Class<T> valueType) {
            return readerFor(valueType).readValue(src);
        }

        public <T> T convertValue(Object fromValue, Class<T> toValueType) {
            if (fromValue instanceof JsonNode) {
                return readerFor(toValueType).readValue((JsonNode) fromValue);
            }
            return readerFor(toValueType).readValue(valueToTree(fromValue));
        }

        public <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
            if (fromValue instanceof JsonNode) {
                return readerFor(toValueTypeRef).readValue((JsonNode) fromValue);
            }
            return readerFor(toValueTypeRef).readValue(valueToTree(fromValue));
        }

        public void writeValue(File resultFile, Object value) {
            writer(false).writeValue(resultFile, value);
        }

        public void writeValue(Path resultFile, Object value) {
            writer(false).writeValue(resultFile, value);
        }

        public void writeValue(JsonGenerator generator, Object value) {
            writer(false).writeValue(generator, value);
        }

        public void writeValue(OutputStream out, Object value) {
            writer(false).writeValue(out, value);
        }

        public void writeValue(Writer out, Object value) {
            writer(false).writeValue(out, value);
        }

        public void writeValue(DataOutput out, Object value) {
            writer(false).writeValue(out, value);
        }

        public String writeValueAsString(Object value) {
            return writer(false).writeValueAsString(value);
        }

        public byte[] writeValueAsBytes(Object value) {
            return writer(false).writeValueAsBytes(value);
        }

        public JsonNode readTree(String content) {
            return reader().readTree(content);
        }

        public JsonNode readTree(byte[] content) {
            return reader().readTree(content);
        }

        public JsonNode readTree(File source) {
            return jsonMapper.readTree(source);
        }

        public JsonNode readTree(InputStream source) {
            return reader().readTree(source);
        }

        public JavaType constructType(java.lang.reflect.Type type) {
            return jsonMapper.constructType(type);
        }

        public JavaType constructType(TypeReference<?> typeReference) {
            return jsonMapper.constructType(typeReference);
        }

        public EntityWriter writer() {
            return new EntityWriter(false);
        }

        public EntityWriter writerWithDefaultPrettyPrinter() {
            return new EntityWriter(true);
        }

        public ObjectWriter jacksonWriter() {
            return rawWriter(false);
        }

        public ObjectWriter jacksonWriterWithDefaultPrettyPrinter() {
            return rawWriter(true);
        }

        private ObjectReader reader() {
            ObjectReader reader = jsonMapper.reader();
            if (activeView != null) {
                reader = reader.withView(activeView);
            }
            return reader;
        }

        private ObjectReader readerFor(Class<?> valueType) {
            ObjectReader reader = jsonMapper.readerFor(valueType);
            if (activeView != null) {
                reader = reader.withView(activeView);
            }
            return reader;
        }

        private ObjectReader readerFor(TypeReference<?> valueTypeRef) {
            ObjectReader reader = jsonMapper.readerFor(valueTypeRef);
            if (activeView != null) {
                reader = reader.withView(activeView);
            }
            return reader;
        }

        private ObjectWriter rawWriter(boolean pretty) {
            ObjectWriter writer = activeView == null
                    ? jsonMapper.writer()
                    : jsonMapper.writerWithView(activeView);
            if (pretty) {
                writer = writer.withDefaultPrettyPrinter();
            }
            return writer;
        }

        private EntityWriter writer(boolean pretty) {
            return new EntityWriter(pretty);
        }

        private Object valueForSerialization(Object value) {
            if (!keyOnly || value == null) {
                return value;
            }

            try {
                EntityUtils.EntityWrapper<Object> ew = EntityUtils.EntityWrapper.of(value);
                if (ew.hasIdField() && ew.getEntityInfo().isCollapsibleInKeyView()) {
                    Optional<EntityUtils.Key> opt = ew.getOptionalKey();
                    if (opt.isPresent()) {
                        return opt.get();
                    }
                }
            } catch (Exception e) {
                log.trace("Unable to collapse value for key-only serialization", e);
            }

            return value;
        }

        private static JsonMapper newDefaultJsonMapper() {
            SimpleModule module = new SimpleModule();
            //TODO katzelda October 2020: add Amount Serializer back when we do substances
//            module.setSerializerModifier(new AmountSerializerModifier());
            return JsonMapper.builderWithJackson2Defaults()
                    .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                    .changeDefaultPropertyInclusion(inclusion ->
                            JsonInclude.Value.construct(
                                    JsonInclude.Include.NON_NULL,
                                    JsonInclude.Include.NON_NULL
                            )
                    )
                    .addModule(module)
                    .addHandler(UNKNOWN_PROPERTY_HANDLER)
                    .build();
        }

        private static JsonMapper newJsonDiffJsonMapper() {
            return newDefaultJsonMapper();
        }

        private static Class<?> resolveActiveView(Class<?>... views) {
            Class<?> active = null;
            if (views != null) {
                for (Class<?> view : views) {
                    if (view != null) {
                        active = view;
                    }
                }
            }
            return active;
        }

        private static boolean hasKeyView(Class<?>... views) {
            if (views == null) {
                return false;
            }
            for (Class<?> view : views) {
                if (BeanViews.Key.class.equals(view)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean skipUnknownProperty(JsonParser parser, String propertyName) {
            try {
                /*
                Logger.warn("Unknown property \""
                            +propertyName+"\" (token="
                            +parser.getCurrentToken()
                            +") while parsing; skipping it..");
                            */
                parser.skipChildren();
            }
            catch (Exception ex) {
                log.error("Unable to handle unknown property: " + propertyName, ex);
                return false;
            }
            return true;
        }

        public class EntityWriter implements Serializable {
            private static final long serialVersionUID = 1L;

            private final boolean pretty;

            private EntityWriter(boolean pretty) {
                this.pretty = pretty;
            }

            public void writeValue(File resultFile, Object value) {
                rawWriter(pretty).writeValue(resultFile, valueForSerialization(value));
            }

            public void writeValue(Path resultFile, Object value) {
                rawWriter(pretty).writeValue(resultFile, valueForSerialization(value));
            }

            public void writeValue(JsonGenerator generator, Object value) {
                rawWriter(pretty).writeValue(generator, valueForSerialization(value));
            }

            public void writeValue(OutputStream out, Object value) {
                rawWriter(pretty).writeValue(out, valueForSerialization(value));
            }

            public void writeValue(Writer out, Object value) {
                rawWriter(pretty).writeValue(out, valueForSerialization(value));
            }

            public void writeValue(DataOutput out, Object value) {
                rawWriter(pretty).writeValue(out, valueForSerialization(value));
            }

            public String writeValueAsString(Object value) {
                return rawWriter(pretty).writeValueAsString(valueForSerialization(value));
            }

            public byte[] writeValueAsBytes(Object value) {
                return rawWriter(pretty).writeValueAsBytes(valueForSerialization(value));
            }
        }
    }


    public static enum RESPONSE_TYPE{
    	FULL,
    	MESSAGES
    }



}
 
