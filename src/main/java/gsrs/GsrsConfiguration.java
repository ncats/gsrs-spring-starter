package gsrs;

import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import gsrs.repository.GroupRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Group;
import ix.core.models.Principal;
import ix.ginas.models.serialization.GroupDeserializer;
import ix.ginas.models.serialization.GroupSerializer;
import ix.ginas.models.serialization.PrincipalDeserializer;
import ix.ginas.models.serialization.PrincipalSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
//@EnableConfigurationProperties(RegisteredFunctionProperties.class)
public class GsrsConfiguration {
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("akka-spring-demo");
        SpringExtension.SPRING_EXTENSION_PROVIDER.get(system).initialize(applicationContext);
        return system;
    }
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        return propertySourcesPlaceholderConfigurer;
    }
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AutowireHelper autowireHelper(){
        return new AutowireHelper();
    }

//    @Bean
//    public SubstanceValidatorFactory substanceValidatorFactory(){
//        return new DefaultSubstanceValidatorFactory();
//    }

//    @Bean
//    public IDGenerator<String> approvalIdGenerator(SubstanceRepository substanceRepository){
//        return new UNIIGenerator(substanceRepository);
//    }



//    @Bean
//    @Primary
//    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
//        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
//        objectMapper.enable(MapperFeature.DEFAULT_VIEW_INCLUSION);
//
//
//        return objectMapper;
//    }
    @Bean
    @Primary
    @Autowired
    public Module jacksonModule(GroupRepository groupRepository, PrincipalRepository principalRepository ){
        SimpleModule module = new SimpleModule();
        module.addSerializer(Principal.class, new PrincipalSerializer());

        module.addDeserializer(Principal.class, new PrincipalDeserializer(principalRepository));

        module.addSerializer(Group.class, new GroupSerializer());
        module.addDeserializer(Group.class, new GroupDeserializer(groupRepository));


        return module;
    }
}
