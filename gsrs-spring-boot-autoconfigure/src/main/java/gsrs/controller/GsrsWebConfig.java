package gsrs.controller;

import gsrs.controller.hateoas.DefaultGsrsEntityToControllerMapper;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModelProcessor;
import ix.core.controllers.EntityFactory;
import ix.core.interfaces.GsrsJsonMapper;
import ix.core.interfaces.GsrsJsonMapperResolver;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.util.ReflectionUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuration that generates the GSRS Standard Rest API routes
 * by parsing the GSRS custom annotations {@link AbstractGsrsEntityController}
 * and all the custom GSRS route mapping annotations such as {@link GetGsrsRestApiMapping} etc.
 */
@Configuration
public class GsrsWebConfig {

    public abstract class GsrsJsonMapperInterceptor implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            return ReflectionUtils.invokeMethod(invocation.getMethod(), getObject(), invocation.getArguments());
        }

        protected abstract GsrsJsonMapper getObject();

    }

    @Bean
    public GsrsJsonMapper gsrsJsonMapper(GsrsJsonMapperResolver objectMapperResolver) {
        ProxyFactory factory = new ProxyFactory();
        factory.setInterfaces(GsrsJsonMapper.class);
        factory.setTargetClass(EntityFactory.EntityMapper.class);
        factory.addAdvice(new GsrsJsonMapperInterceptor() {

            @Override
            protected GsrsJsonMapper getObject() {
                return objectMapperResolver.getMapper();
            }

        });

        return (GsrsJsonMapper) factory.getProxy();
    }

    @Bean
    public GsrsUnwrappedEntityModelProcessor gsrsUnwrappedEntityModelProcessor(){
        return new GsrsUnwrappedEntityModelProcessor();
    }

    @Bean
    public tools.jackson.databind.json.JsonMapper jacksonJsonMapper() {
        return EntityFactory.EntityMapper.COMPACT_ENTITY_MAPPER().getJsonMapper();
    }

    @Bean
    public GsrsJsonMapperResolver  objectMapperResolver() {
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

    @Bean
    public JacksonJsonHttpMessageConverter mappingJacksonHttpMessageConverter(JsonMapper jacksonJsonMapper) {
        return new JacksonJsonHttpMessageConverter(jacksonJsonMapper);
    }

}
