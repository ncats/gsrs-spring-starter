package gsrs;


import gsrs.dataexchange.services.ImportMetadataReindexer;
import gsrs.repository.UserProfileRepository;
import gsrs.security.AdminService;
import gsrs.services.*;
import ix.core.EbeanLikeImplicitNamingStategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import gsrs.autoconfigure.GsrsRabbitMqConfiguration;
import gsrs.repository.GroupRepository;
import gsrs.repository.PrincipalRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Configuration(proxyBeanMethods = false)

//TODO: Discuss the meaning of this in the context of
// explicit datasource configurations and lack of
// automatic JPA repositories
@AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)
@Import({StarterEntityRegistrar.class, GsrsRabbitMqConfiguration.class})
public class GsrsEntitiesConfiguration {

    @Autowired
    private GsrsRabbitMqConfiguration gsrsRabbitMqConfiguration;

    @Bean
    public ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public PrincipalService principalService(PrincipalRepository principalRepository, @Qualifier(DefaultDataSourceConfig.NAME_ENTITY_MANAGER) EntityManager entityManager){
        return new PrincipalServiceImpl(principalRepository, entityManager);
    }
    @Bean
    public AdminService adminService(){
        return new AdminService();
    }
    //May 2021: Gsrs 2.x used an old version of JPA to generate the schema
    //to be backwards compatible we have to tell hibernate to use Legacy Jpa (1.0)
    //and not JPA 2 naming strategies.  This mostly affects join table names and columns
    @Bean
    public PhysicalNamingStrategy physical() {
        return new PhysicalNamingStrategyStandardImpl();
    }

    @Bean
    public ImplicitNamingStrategy implicit() {
        return new EbeanLikeImplicitNamingStategy();
    }
    @Bean
    @ConditionalOnMissingBean
    public EditEventService EditEventService(){
        return new EditEventService();
    }
    @Bean
    @ConditionalOnMissingBean
    public GroupService groupService(GroupRepository groupRepository,  @Qualifier(DefaultDataSourceConfig.NAME_ENTITY_MANAGER) EntityManager entityManager){
        return new GroupServiceImpl(groupRepository, entityManager);
    }
    @Bean
    @ConditionalOnMissingBean
    public TextServiceImpl textService(){
        return new TextServiceImpl();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public UserProfileService userProfileService(GroupService groupService, UserProfileRepository userProfileRepository,
                                                 GroupRepository groupRepository, @Qualifier(DefaultDataSourceConfig.NAME_ENTITY_MANAGER) EntityManager entityManager){
        return new UserProfileService( userProfileRepository, groupService, groupRepository, entityManager);
    }
    @Bean
    public TopicExchange substanceExchange(){
        return new TopicExchange(gsrsRabbitMqConfiguration.getExchange());
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerJackson2JsonMessageConverter());

        return rabbitTemplate;
    }

    /**
     * This serializes our RabbitMQ messages to JSON using Jackson instead
     * of default java serialization which would tightly couple our
     * bounded context domain java classes.
     * @return a new message converter that uses Jackson to write our pojos as JSON.
     */
    @Bean
    public Jackson2JsonMessageConverter producerJackson2JsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public ImportMetadataReindexer importMetadataReindexer() {
        return new ImportMetadataReindexer();
    }
}
