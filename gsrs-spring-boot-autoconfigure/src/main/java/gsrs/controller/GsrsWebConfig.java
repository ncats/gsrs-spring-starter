package gsrs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.hateoas.DefaultGsrsEntityToControllerMapper;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModelProcessor;
import ix.core.controllers.EntityFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.ReflectionUtils;

/**
 * Configuration that generates the GSRS Standard Rest API routes
 * by parsing the GSRS custom annotations {@link AbstractGsrsEntityController}
 * and all the custom GSRS route mapping annotations such as {@link GetGsrsRestApiMapping} etc.
 */
@Configuration
public class GsrsWebConfig {

    public abstract class ObjectMapperInterceptor implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            return ReflectionUtils.invokeMethod(invocation.getMethod(), getObject(), invocation.getArguments());
        }

        protected abstract ObjectMapper getObject();

    }
    @Bean
    public ObjectMapper objectMapper(ObjectMapperResolver objectMapperResolver) {
        ProxyFactory factory = new ProxyFactory();
        factory.setTargetClass(EntityFactory.EntityMapper.class);
        factory.addAdvice(new ObjectMapperInterceptor() {

            @Override
            protected ObjectMapper getObject() {
                return objectMapperResolver.getObjectMapper();
            }

        });

        return (ObjectMapper) factory.getProxy();
    }

    @Bean
    public GsrsUnwrappedEntityModelProcessor gsrsUnwrappedEntityModelProcessor(){
        return new GsrsUnwrappedEntityModelProcessor();
    }

//    @Bean
//    public MappingJackson2HttpMessageConverter MappingJackson2HttpMessageConverter(){
//        return new DynamicMappingJacksonHttpMessageConverter();
//    }

    @Bean
    public MappingJackson2HttpMessageConverter MappingJackson2HttpMessageConverter(ObjectMapper objectMapper){
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();

        jsonConverter.setObjectMapper(objectMapper);
        return jsonConverter;
    }
    @Bean
    public ObjectMapperResolver objectMapperResolver() {
        return new RequestMatchingEntityMapperResolver();
    }

    /**
     *
     * This is the code that creates all the api/v1/* mapping based on the custom GSRSApiController annotations.
     */

    @Bean
    public WebMvcRegistrations webMvcRegistrationsHandlerMapping(DefaultGsrsEntityToControllerMapper entityToControllerMapper) {
        return new GsrsWebMvcRegistrations(entityToControllerMapper);

    }

    @Bean
    public DefaultGsrsEntityToControllerMapper gsrsEntityToControllerMapper(){
        return new DefaultGsrsEntityToControllerMapper();
    }
}
