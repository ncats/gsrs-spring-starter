# GSRS 3.0 

This is a GSRS API implementation using a Spring Boot 2 starter package.

## GSRS Modules
The GSRS Spring Starter also works with a few other GSRS modules.  Each of the listed modules builds
upon the ones listed before it.  Clients do not need to use all of these modules.

* `gsrs-spring-boot-starter` The main starter package that autoconfigures GSRS related spring configuration.
* `gsrs-spring-legacy-indexer` Code to support the Legacy GSRS indexer code
* `gsrs-core-entities` GSRS data model  and JPA entities  of the common oft-used classes 
        that can be used by many microservices that aren't specific to one microservice.
* `gsrs-spring-starter-tests` Test helper classes 

## Attempt at maintaining Backwards Compatibility

An attempt has been made to make sure that the REST API is close enough to
the GSRS 2.x codebase so that any REST clients don't have to change.  

### REST API Clients shouldn't have to change
API Routes for fetching entities by id, or searches should use the same URIs and return either identical JSON responses
or at least similar enough that the same JSON parsing code in the client does not have to change. 

### GSRS backend should be easy to port
While the backend GSRS is substantially different between version 2 and 3, customized
GSRS code should have an easy migration path thanks to Spring's Dependency Injection. 

## Configuration File

To maintain backwards compatibility with previous version of GSRS,
The configuration file is in HOCON format and by default 
will look for `application.conf`.
### gsrs-core.conf
Default configuration is in the `gsrs-core.conf` file which is inside the starter so your `application.conf`
should start with:
```
include "gsrs-core.conf"

#put your customization and overrides here:
```

### How to tell SpringBoot to use HOCON
To tell Spring Boot to automatically look for your `application.conf` you can add to your `META-INF/spring.factories`
file this line:

```
org.springframework.boot.env.PropertySourceLoader=com.github.zeldigas.spring.env.HoconPropertySourceLoader
```

which will tell Spring-Boot on start up to look for `application.conf` in HOCON format.
This is recommended because otherwise all `ConfigurationProperties` will have to override the property factory.

## How to Use Starter Package
To use this starter package to make your Spring Boot Application Entity GSRS API compatible
is to add the starter dependency to your maven pom like this:

```xml
<dependency>
    <groupId>gov.nih.ncats</groupId>
    <artifactId>gsrs-spring-boot-starter</artifactId>
    <version>${gsrs.version}</version>
</dependency>

```


### Enabling GSRS API
Add the annotation `@EnableGsrsApi` to your spring boot Application class  This will add all the configurations
needed to get the standard GSRS REST API routes set up.  This annotation has some fields that can be overridden
and will be explained below.

### GSRS REST API Controller
GSRS uses a standardized API for fetching, updating, validating and loading entity data.

To make sure the routes are formatted correctly and that all entities follow the same API contract, the GSRS Controller that creates these standardized routes
is an abstract class, `gsrs.controllerAbstractGsrsEntityController<T,I>` Where the generic type `T` 
is the entity type and the generic type `I` is the type for that entity's Id.. You need to extend this class and implement the few abstract methods
to hook in your own entity.

Please note that there are several subclasses of `AbstractGsrsEntityController` which add even more route methods
so you will probably not be extending this class directly but you can.



In order to take advantage of the GSRS standard API, your controller needs to both extend this class
and add the annotation `gsrs.controller.GsrsRestApiController`.  The  `@GsrsRestApiController` annotation
requires 2 fields to be set: `context` which is the part of the API path pattern that will
 map to this controller and `idHelper`. The `AbstractGsrsEntityService` class (see more about it below)
  currently also needs these fields
 and should be passed via the constructor.  Future revisions may remove this duplication. 
 
```java
@GsrsRestApiController(context = MyController.CONTEXT, idHelper = IdHelpers.NUMBER)
public class MyController extends AbstractGsrsEntityController<MyEntity, Long> {
 
  public static final String CONTEXT = "myContext";
  //.. implement methods here
}
```

#### Context
  The `context` variable is used to uniquely define your entity.  
  The Standard GSRS API path will use the pattern `api/v1/$context` to map routes to your controller. 
  Other GSRS classes will also use the context to help map configuration properties.
  
  In addition to supplying the context in the annotation, you currently also need to provide the context String
  to the `AbstractGsrsEntityController` constructor. It is recommended that you make a public static final field
  for your context and reference that field in both the annotation and the constructor to avoid the risk of typos or
  getting the 2 places out of sync.
  
#### IdHelper
  Different entities have different types for the ID. `IdHelper` is an interface to help `GsrsRestApiController`
  figure out how to make the route patterns for your particular ID type. Common ID types are 
  provided for you in the enum `IdHelpers` but it is also possible to make a custom implementation.
   
### GsrsRestApiRequestMapping
GSRS includes custom RequestMapping annotations that will automatically add the `api/v$version/$context` prefixes
to controller routes.  There are annotations for each of the HTTP verbs such as `@GetGsrsRestApiMapping` and
`@PostGsrsRestApiMapping` etc.

### Indexing
GSRS can leverage text indexing of entity fields to provide additional search capabilities.  Three steps are required
to add Text Indexing from the GSRS starter:
 1. Specify the IndexingType in the `@EanableGsrsApi` annotation to add the appropriate configurations.
 1. Extend the appropriate subclass of `AbstractGsrsEntityController` to add the extra GSRS routes to use the text search functions.
 1. Add the appropriate annotations to your entity POJOs and/or write additional component classes to tell spring how to 
 populate your index.  Each IndexType will be different and are explained below:
 
#### Legacy Text Indexing
To maintain backwards compatibility with GSRS 2, a special controller that follows the GSRS 2 Lucene based TextIndexer
is `AbstractLegacyTextSearchGsrsEntityController` which is a subclass of 
`AbstractGsrsEntityController` that adds the text search routes.

By default, `EnableGsrsApi` uses this indexerType but you can also explicitly set it:
```java
@EnableGsrsApi(indexerType = EnableGsrsApi.IndexerType.LEGACY)
```

You will also need to add an additional dependency to your maven pom:

```xml
<dependency>
    <groupId>gov.nih.ncats</groupId>
    <artifactId>gsrs-spring-legacy-indexer</artifactId>
    <version>${gsrs.version}</version>
</dependency>
```

#### TextIndexer @Indexable annotations
The legacy TextIndexer can use reflection to automatically index entities using the `@Indexable` annotation on an entity's
fields and/or public getters. By default, if a field is public it will be indexed.  If a field
is not public, then the field must be annotated with either `@Indexable` or `@javax.persistance.Id`
for it to be indexed.  If the entity has any superclasses, any inherited fields that meet
these indexing criteria are also indexed.

#### IndexValueMaker
The `ix.core.search.text.IndexableValue<T>` interface is used to add additional data to the index document in abstract
way.  The primary method for this interface is 
```java
void createIndexableValues(T entity, Consumer<IndexableValue> consumer) ;
``` 

Any new items to add to the index should be given to the consumer.  There are multiple `IndexableValue` 
implementations to make it easier to make many different basic types of indexed records.

Right now IndexValueMaker classes will automatically be added to Spring if you add the `@Component` annotation to
 your implementation class but this will probably change in the near future to a list in the conf file
 so users could enable/disable indexValueMakers more easily.
 
 
#### ReflectingIndexerAware interface
Your entity or an object embedded in your entity may also implement the `ReflectingIndexerAware` interface which
is defined as 
```java
public interface ReflectingIndexerAware {
    void index(PathStack currentPathStack, Consumer<IndexableValue> consumer);
    String getEmbeddedIndexFieldName();
}
``` 
The built in Entity Indexer using reflection will call those method as well to add any other built in IndexableValues.


#### Adding TextIndexing Related REST API Methods
To start leveraging this TextIndexing in your API you need to implement a `LegacyGsrsSearchService<T>` for your entity
which helps ties the index to the entity repository. Make sure it is annotated as a `@Service` so it can be
dependency injected.

```java
@Service
public class MyLegacySearcher extends LegacyGsrsSearchService<MyEntity> {

    @Autowired
    public LegacyBookSearcher(MyEntityRepository repository) {
        super(MyEntity.class, repository);
    }
}
```
#### Search Results as GSRS ETags
If you are using `gsrs-core-entities`, then change your Controller to use the `AbstractGsrsEntityController` subclass `EtagLegacySearchEntityController`
which not only adds all the search related REST calls but also integrates with the GSRS data model.

#### No Indexing
To not use any indexing in GSRS, set the IndexType to `IndexerType.NONE` like this:
```java
@EnableGsrsApi(indexerType = EnableGsrsApi.IndexerType.NONE)
```

Your GSRS controllers should then only extend the basic `AbstractGsrsEntityController` class so no text indexing
routes are added.


### GSRS Controller Annotations

Your concrete controller class that extends the abstract GSRSEntityController must be annotated with
`@GsrsRestApiController` along with setting the fields for `context` and `idHelper`
The `AbstractEntityService` constructor (see more about it below) also takes the the same parameters so a good practice 
is to make your context a public static final String so you can reference it in both places
so they always match.

```java
@GsrsRestApiController(context =CvController.CONTEXT,  idHelper = IdHelpers.NUMBER)
public class CvController extends AbstractLegacyTextSearchGsrsEntityController<ControlledVocabulary, Long> {
    public static final String  CONTEXT = "vocabularies";

// ... implement abstract methods

}
```

### Entity Context
The root part of the entity routes is also `api/v1/` followed by a String GSRS calls the `context`.  The Context
is what will differentiate your entity routes from all the other entities. 

#### GsrsEntityService
The Controller accepts the GSRS standard REST API calls and then delegates to a `GsrsEntityService` which handles all the business logic,
 validation and interaction with the entity repositories.  To help you, there is an `AbstractGsrsEntityService` abstract class 
 that handles most of the boilerplate code for you, you just have to implement a few methods to hook into your repository.
 
 ```java 
@Service
public class ControlledVocabularyEntityService extends AbstractGsrsEntityService<ControlledVocabulary, Long> {
    public static final String  CONTEXT = "vocabularies";


    public ControlledVocabularyEntityService() {
        super(CONTEXT,  IdHelpers.NUMBER);
    } 
```
The constructor takes the same context and idHelper as described in the controller above.

Note that your EntityService concrete class should be annotated as a `@Service` so it can be autowired into the controller.

 The advantage of splitting the work into separate controller and service classes is that
 the Service class decouples the business logic from the controller. Therefore, we can change the 
 controller without touching the business logic, allow for multiple ways software can interact with a `GSRSEntityService`
 and finally to ease testing by being able to test the business logic without the need for standing up a full
 server/controller and to test the controller with a mock service.
 
## Customizations:

### Custom IndexValueMakers
  The `ix.core.search.text.IndexValueMaker<T>` interface is a way to generate custom lucene indexed fields
  to the Document representing an entity of Type T.
  
  To add an implementation just annotate your class as a `@Component` so it gets picked up by the Spring component scan:
  
  ```java
@Component
public class MyIndexValueMaker implements IndexValueMaker<Foo>{
     ...
} 
```

### EntityProcessor
GSRS uses the `ix.core.EntityProcessor<T>` interface to provide hooks for
JPA pre and post hooks when an Entity's data is changed in the database.

#### Detecting EntityProcessors
There are multiple ways that EntityProcessors can be detected by GSRS. Each one has different pros and cons.
To choose which detector to use, set the field `entityProcessorDetector` in the `@EnableGsrsApi` annotation.

##### CONF
This is the current default Detector.  Only Entity Processors that are explicitly listed in the application.conf file
with the property `gsrs.entiryprocessors` will be used.

```
gsrs.entityprocessors = [
   {
        "class" = "com.example.domain.MyEntity",
        "processor" = "com.example.entityProcessor.MyEntityProcessor"
   },
   {
        "class" = "com.example.domain.MyEntity",
        "processor" = "com.example.entityProcessor.MyEntityProcessor2"
   },
   {
        "class" = "com.example.domain.MyOtherEntity",
        "processor" = "com.example.entityProcessor.CompletelyDifferentProcessor"
   },
]
```

and in your java Spring Application class:

```java
@EnableGsrsApi(indexerType = ...,
                entityProcessorDetector = EntityProcessorDetector.CONF)
```

Listing the processors has several advantages including having an easy way to see exactly which processors are to be used
and allows for different configurations to turn on or off EntityProcessors by adding or removing items from the list.
You may also list an entityProcessor class multiple times with different parameters (see next section).

###### Customized parameters of EntityProcessors

To keep backwards compatibility with GSRS 2.x EntityProcessors, the config option allows for an optional
`with` field of a JSON Map and an constructor that takes a `Map` of additional parameters 
if the entityProcessor supports customized instances.

##### COMPONENT_SCAN

Another option is to use Spring's component scan mechanism to find all EntityProcessor implementations.

```java
@EnableGsrsApi(indexerType = ...,
                entityProcessorDetector = EnableGsrsApi.EntityProcessorDetector.COMPONENT_SCAN)
```

Then to add an implementation just annotate your EntityProcessor class as a `@Component` so it gets picked up by the Spring component scan:
  
  ```java
@Component
public class MyEntityProcessor implements EntityProcessor<Foo>{
     ...
} 
```

This is easier to quickly add new EntityProcessors but the downside is you can't disable EntityProcessors
without removing the class from component scan or removing the `@Component` annotation.  
Another downside is each commponent can only be instantiated once.

##### CUSTOM EntityProcessorFactory
  By selecting `CUSTOM`, you must provide your own `EntityProcessorFactory` Bean definition in your Spring Configuration.

### Custom Validators
Entities can have multiple custom validators.  

#### Validator interface

Your custom validator should implement the interface `ix.ginas.utils.validation.ValidatorPlugin`
which has 2 methods that need to implemented :
```java
 public void validate(T newValue, T oldValue, ValidatorCallback callback);
   

```

which actually is where you do your validation, and any validation errors or warnings should be passed through the callback parameter.

The other method to implement is :

```java
  public boolean supports(T newValue, T oldValue, ValidatorFactoryService.ValidatorConfig.METHOD_TYPE methodType) {
      
```

where `METHOD_TYPE` is an enum for which type of action is being done: and UPDATE, NEW, BATCH etc.  
When GSRS validates an entity, it will first call the supports method and then only if that method returns `true` will it call the validate() method.

In both methods if this is a new entity (opposed to an update) then the parameter `oldValue` will be null.

#### Dependency Injection is Allowed

GSRS will create new instances of your validator using reflection and the empty constructor  
and then will inject dependencies into the validator so you are able to annotate your fields with `@Autowired`

```java
public class MyValidator implements ValidatorPlugin<MyEntity> {
    @Autowired
    private MyRepository repository;

    @Override
    public void validate(MyEntity newValue, MyEntity oldValue, ValidatorCallback callback) {
        //... use the repository field to validate my object
    }
 //...
}
```

#### Adding your custom Validator
  Once you have your custom `ValidatorPlugin` add it to your conf file in `gsrs.validators.<your-context>` as a list
  of `ValidationConfig` objects where the object for `ValidationConfig` looks like:
  ```java
class ValidatorConfig{


        private Class validatorClass;
        /**
         * Additional parameters to initialize in your instance returned by
         * {@link #getValidatorClass()}.
         */
        private Map<String, Object> parameters;
        private Class newObjClass;
        private METHOD_TYPE methodType;
}
```
  
  For example, if your context is `vocabularies`, and you have validator that checks for Duplicates your conf would look like:
  
  ```
gsrs.validators.vocabularies = [
    {
        "validatorClass" = "gsrs.vocab.DuplicateDomainValidator",
         "newObjClass" = "ix.ginas.models.v1.ControlledVocabulary",
    }
]
```

### Gsrs-core-entities
To add support for GSRS core entities you need to have these gsrs maven dependencies:
```xml
 <dependency>
    <groupId>gov.nih.ncats</groupId>
    <artifactId>gsrs-spring-boot-starter</artifactId>
    <version>${gsrs.version}</version>
</dependency>
<dependency>
    <groupId>gov.nih.ncats</groupId>
    <artifactId>gsrs-spring-legacy-indexer</artifactId>
    <version>${gsrs.version}</version>
</dependency>
<dependency>
    <groupId>gov.nih.ncats</groupId>
    <artifactId>gsrs-core-entities</artifactId>
    <version>${gsrs.version}</version>
</dependency>
``` 
Then in your main `@SpringBootApplication` annotated class, in addition to `EnableGsrsApi` as described above,
add these lines:
```java
@EnableGsrsJpaEntities
@EntityScan(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableJpaRepositories(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
```

The EntityScan and EnableJpaRepositories need to list all the base packages to scan. 
 The packages listed here are the ones to scan from the starter.  Hopefully this is a temporarly solution
 until those packages can be autoscanned by the starter.
 
 Please also add your own packages to those lists.
 
 ## Security
 GSRS uses Spring Secuity for authentication and authorization.
 
 
 ### Authorization
 GSRS has built in User Roles for Authorization.
* Query,
* DataEntry,
* SuperDataEntry,
* Updater,
* SuperUpdate,
* Approver,
* Admin
 
 Certain API routes are only allowed to be executed by users who have specific roles.
 For example, in order to update a GSRS Entity, you need to have the `Updater` Role.
 
  The GSRS Starter has helper annotations to make this more clear
 
 * `@hasAdminRole`
 * `@hasApproverRole`
 * `@hasDataEntryRole`
 * `@hasSuperDataEntryRole`
 * `@hasSuperUpdaterRole`
 * `@hasUpdateRole`
 
 You can also use the standard Spring `@PreAuthorize()`/ `@PostAuthorize()` annotations with these roles as well.
 
 It doesn't matter what Authentication mechanism you use as long as your users have these defined Roles.
 
 ### Authentication
  
 
 #### Legacy Authentication
  The GSRS Starter supports the legacy GSRS 2 authentication mechanisms such as the GSRS User Profile table and 
  checking specific headers in REST Requests for authentication information.  To Turn this on,
  use `@EnableLegacyGsrsAuthentication`.

These are the legacy config options that can be set
```
# SSO HTTP proxy authentication settings
ix.authentication.trustheader=true
ix.authentication.usernameheader="OAM_REMOTE_USER"
ix.authentication.useremailheader="AUTHENTICATION_HEADER_NAME_EMAIL"

# set this "false" to only allow authenticated users to see the application
ix.authentication.allownonauthenticated=true

# set this "true" to allow any user that authenticates to be registered
# as a user automatically
ix.authentication.autoregister=true

#Set this to "true" to allow autoregistered users to be active as well
ix.authentication.autoregisteractive=true

```
##### TrustHeader
This option assumes that the GSRS system is sitting behind a Single Sign On (SSO) System that performs authentication
and will write specific headers to each request as it passes through the SSO gateway.
 

#### Username and Password in Header
this should only be used in https situations. Legacy GSRS lets you include the GSRS credentials to be put in HTTP headers
as `auth-username` and `auth-password` headers.

## GSRS Common REST API Patterns
### Paged Requests

### Fetching By ID

### Flexible Fetching

### Fetching Partial Records by Field

### JSON Views
Adding a URL parameter `view` with one of the values below will change the returned JSON response to limit what parts 
of the entities are returned.

* full - return everything this might cause performance bottlenecks fetching all the data.

* compact - any fields that are collections will only return with a URL for how to fetch those records

* key - return only the Entity id and class.  This is mostly used internally for fast fetching to be refecthed from a datastore later.

### Change Response Code
Sometimes, consumers of the GSRS API are not able to handle standard REST status codes.  For example, a bad request
will usually return some kind of 400 level status code.  Some GSRS consumers can't handle such status codes and require
the API only return specfic status codes such as 500 for any error. If your consumer is like that you can add
the additional URL parameter `error_response` to set the status code to a particlar int value if there is a problem.
Only valid error codes such as something in the 400s or 500s are allowed; any value outside that range will be ignored.

For example, if you try to record by making a POST or PUT and your credentials have an insufficient Role
so that you are unauthorized to make that update, the API will normally return a status code of 401 unauthorized.
However if you made the same request with the same insufficient credentials but this time added the url parameter `error_response=500`
the API will return a status code of 500 instead.

The parameter name is configurable if you change the value of `gsrs.api.errorCodeParameter` in your property file (or conf file).
By default, it is set as `gsrs.api.errorCodeParameter=error_response`

## GSRS Entity
GSRS uses JPA annotations

### EntityInfo

### EntityMapper

### EntityMapperOptions

## Testing 
   There is a test module called `gsrs-spring-starter-tests` please add this to your maven pom as a test depdendency
   
   ```xml
<dependency>
      <groupId>gov.nih.ncats</groupId>
      <artifactId>gsrs-spring-starter-tests</artifactId>
      <version>${gsrs.version}</version>
      <scope>test</scope>
  </dependency>
   ```

This module contains helper classes and annotations to work with the GSRS Spring Boot Starter.
### JUnit Abstract Test Classes
The Test Starter comes with abstract JUnit test classes to fill in common boiler plate test set up for GSRS Data Tests.
There are versions of each one and helper classes for both JUnit 4 in the `gsrs.startertests.junit4` package
and JUnit 5 in `gsrs.startertests.jupiter` package.
#### JUnit 4 Support
There are several built in JUnit Rules and abstract test classes that use at least some of them:
##### AbstractGsrsJpaEntityJunit4Test
`AbstractGsrsJpaEntityJunit4Test` is an abstract Junit 4 class that registers
 JUnit 4 Rules to clear out any audit information and clean up the text indexer.
 
##### JUnit 4 Rules
GSRS uses the `ncats-common` library which has support for resetting initialization routines 
which is sometimes needed in tests.  Several JUnit 4 Rules have been written
to reset only certain parts of the GSRS codebase to allow client code fine grain control
of what to re-initialize and when.
All the following JUnit Rule classes can be created in as a member field and annotated
 with the `@Rule` JUnit 4 annotation to be run before each test, or it can be created as a static field
 and annotated with `@ClassRule` annotation to be run once before any of the  tests have been run.
  
##### ResetAllCacheSuppliersRule
This is the Reset everything and should be used with care or if you are not sure what needs to be reset.
##### ResetAllEntityProcessorsRule
This will reset only EntityProcessorFactory and if used in conjunction with AbstractGsrsJpaEntityJunit4Test
or a Configuration that creates the `TestEntityProcessorFactory` Bean, will reset that so the next test can change
which EntityProcessors will get picked up.
 
##### ResetAllEntityServicesRule
This will reset only classes that extend `AbstractGsrsEntityService` class 
and will reset that so the next test can change
which how the entityService registers things like Validators.

##### ResetIndexValueMakerFactoryRule
This will reset only IndexValueMakerFactory and if used in conjunction 
with AbstractGsrsJpaEntityJunit4Test
or a Configuration that creates the `TestIndexValueMakerFactory` Bean, 
will reset that so the next test can change
which IndexValueMakers will get picked up.

#### JUnit 5 Support
JUnit 5 helper classes are located in the package `gsrs.startertests.jupiter`.

##### AbstractGsrsJpaEntityJunit5Test
`AbstractGsrsJpaEntityJunit5Test` is an abstract Junit 5 class that registers
 JUnit 5 extensions (in JUnit 4 jargon, "Rules") to clear out any audit information and clean up the text indexer.
 
##### JUnit 5 GSRS Extensions
 Unlike Junit 4 Rules which could be applied either before each test or on all the test in a test class,
 JUnit 5 Extensions must implement different interfaces for each step in the lifecycle, so there are multiple versions
 of each "Reseter" Extension.  For example, the extensions to reset all the CacheSuppliers include:
 `ResetAllCacheSupplierBeforeAllExtension` and `ResetAllCacheSupplierBeforeEachExtension`. 
 Other Extensions follow similar naming patterns.
 
 ##### ResetAllCacheSuppliersBeforeXXXExtension
 This is the Reset everything and should be used with care or if you are not sure what needs to be reset.
 ##### ResetAllEntityProcessorsBeforeXXXExtension
 This will reset only EntityProcessorFactory and if used in conjunction with AbstractGsrsJpaEntityJunit5Test
 or a Configuration that creates the `TestEntityProcessorFactory` Bean, will reset that so the next test can change
 which EntityProcessors will get picked up.
 Note that if you call `TestEntityProcessors#addEntityProcessor()` 
 or `TestEntityProcessors#setEntityProcessors()` in either your test or in a `@BeforeEach` method
 then the processor will reset itself so you don't need use this extension.
  
 ##### ResetAllEntityServicesBeforeXXXExtension
 This will reset only classes that extend `AbstractGsrsEntityService` class 
 and will reset that so the next test can change
 which how the entityService registers things like Validators.
 
 ##### ResetIndexValueMakerFactoryBeforeXXXExtension
 This will reset only IndexValueMakerFactory and if used in conjunction 
 with AbstractGsrsJpaEntityJunit5Test
 or a Configuration that creates the `TestIndexValueMakerFactory` Bean, 
 will reset that so the next test can change
 which IndexValueMakers will get picked up.
 
### GsrsJpaTest
The `@GsrsJpaTest` annotation is like Spring Boot's `@DataJpaTest` except it adds support for
GSRS related configuration and classes.


#### classes field
The `classes` field should be used to add your configuration classes such as your SpringApplication class an any additional
test configuration classes you need for your test to work.

#### dirtyMode
This will set the DirtiesContext.ClassMode which is used by JPA tests to know when
a database should be rebuilt.  This should be preferred over truncating the database or calling
`repository.clear()` because those options don't reset generated sequence counts like autoincrement ids.

```java
@GsrsJpaTest(dirtyMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MyTest extends AbstractGsrsJpaEntityJunit5Test{
  //... test code goes here
}
```

### Tests with Custom IndexValueMakers
The test classes and annotations disucssed in this section are in the `gsrs-spring-starter-tests` module
```xml
<dependency>
    <groupId>gov.nih.ncats</groupId>
    <artifactId>gsrs-spring-starter-tests</artifactId>
    <version>${gsrs.version}</version>
    <scope>test</scope>
</dependency>
```
#### AbstractGsrsJpaEntityJunit5Test
`AbstractGsrsJpaEntityJunit5Test` is an abstract Test class that autoregisters some
 GSRS Junit 5 Extensions (what Junit 4 called "Rules")
to automatically reset the legacy text indexer and JPA Audit information.  This also changes
the property for `ix.home` which is used by the LegacyTextIndexer to
make the TextIndexer write the index to a temporary folder for each test instead of the 
location specified in the config file.

#### JPA Data Tests with @GsrsJpaTest
`@GsrsJpaTest` is an extension of `@JpaDataTest` that adds common GSRS Test Configurations for support
for EntityProcessors and TextIndexers etc. 
##### Tests with Custom IndexValueMakers
By default, `@GsrsJpaTest` will replace the usual code that finds your IndexValueMakers,
the `IndexValueMakerFactory` implementation with a test version, `TestIndexValueMakerFactory`.
If you don't override this Bean, it will not find any IndexValueMakers.  You can use a custom Configuration to add your
own TestIndexValueMakerFactory instance which passes along the IndexValueMakers to use in the test:

```java

@GsrsJpaTest(classes =MySpringApplication.class)
@Import(IndexValueMakerFactoryTest.MyConfig.class)
public class IndexValueMakerFactoryTest {

    @TestConfiguration
    static class MyConfig{
        @Bean
        @Primary
        public IndexValueMakerFactory indexValueMakerFactory(){
            return new TestIndexValueMakerFactory(new MyIndexValueMaker());
        }
    }

  // ... tests that test myIndexValueMaker works as expected
```

Note that the IndexValueMakerFactory Bean is annotated with `@Primary` this is in case 
the configuration accidentally loads the default bean first it will prefer your factory implementation
when injecting dependencies. 

##### Tests with Custom EntityProcessors
By default, `@GsrsJpaTest` will replace the usual code that finds your EntityProcessors,
the `EntityProcessorFactory` implementation with a test version, `TestEntityProcessorFactory`.
If you don't override this Bean, it will not find any EntityProcessors.  

There are two ways to add your own EntityProcessors to get picked up by your test:
1. You can Inject the instance use the add/clear methods on `TestEntityProcessorFactory` to add the ones you want for each particular test:
```java
    @Autowired
    private TestEntityProcessorFactory entityProcessorFactory;

    @BeforeEach
    public void initialzeProcessors(){
        entityProcessorFactory.setEntityProcessors(new MyEntityProcessor());

    }

```
2. You can use a custom Configuration to add your
own TestEntityProcessorFactory instance which passes along the EntityProcessors to use in the test:

```java

@GsrsJpaTest(classes =GsrsSpringApplication.class)
@ActiveProfiles("test")
@Import(EntityProcessorTest.MyConfig.class)
public class EntityProcessorTest  extends AbstractGsrsJpaEntityJunit5Test {

    @TestConfiguration
     static class MyConfig {
        @Bean
        @Primary
        public EntityProcessorFactory entityProcessorFactory() {
            return new TestEntityProcessorFactory(new MyEntityProcessor());
        }
    }

  // ... tests that test myIndexValueMaker works as expected
```

Note that the EntityProcessorFactory Bean is annotated with `@Primary` this is in case 
the configuration accidentally loads the default bean first it will prefer your factory implementation
when injecting dependencies. 

### GSRS Hamcrest Matchers
GSRS test module contains some helper Hamcrest Matchers

#### MatchesExample
 Compares the given Example object with the Object under Test but only compares the getter methods
 that return non-null values.  This lets you create intent revealing example objects setting only the
 fields that matter for the test.
 
##### MatchingIgnore
the `@MatchingIgnore` annotation can be put on a getter method so that the MatchesExample matcher will ignore
the field even if it doesn't return a non-null value. This is often used to annotate transient or jsonIgnoreable
fields.

##### Explicitly Ignore a Field
The MatchesExample matcher has a `ignoreField(String)` method to explicitly tell the matcher to ignore specific fields.

