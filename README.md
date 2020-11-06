# GSRS 3.0 

This is a GSRS implementation using Spring Boot 2

## Attempt at maintaining Backwards Compatibility

An attempt has been made to make sure that the REST API is close enough to
the GSRS 2.x codebase so that any REST clients don't have to change.  

### REST API Clients shouldn't have to change
API Routes for fetching entities by id, or searches should use the same URIs and return either identical JSON responses
or at least similar enough that the same JSON parsing code in the client does not have to change. 

### GSRS backend should be easy to port
While the backend GSRS is substantially different between version 2 and 3, customized
GSRS code should have an easy migration path thanks to Spring's Dependency Injection. 

## How to Run
This is a Spring Boot application and can be run using the provided maven wrapper like so:
```
./mvnw spring-boot:run
```

Or from inside your IDE, you can run the main method on the Application class in the gsrs package.
## Configuration File

To maintain backwards compatibility with previous version of GSRS,
The configuration file is in HOCON format and by default 
will look for `application.conf`.

Default configuration is in the `gsrs-core.conf` file so your `application.conf`
should start with:
```
include "gsrs-core.conf"

#put your customization and overrides here:
```

## Creating your GSRS Controller
GSRS uses a standardized API for fetching, updating, validating and loading entity data.

To make sure the routes are formatted correctly and that all entities follow the same API contract, the GSRS Controller that creates these standardized routes
is an abstract class, `AbstractGsrsEntityController`. You need to extend this class and implement the few abstract methods
to hook in your own entity.

### Indexing
To maintain backwards compatibility with GSRS 2, a special controller that follows the GSRS 2 Lucene based TextIndexer
can be used instead if you want to use this, extend `AbstractLegacyTextSearchGsrsEntityController` which is a subclass of 
`AbstractGsrsEntityController` that adds the text search routes.

### GSRS Controller Annotations

Your concrete controller class that extends the abstract GSRSEntityController must be annotated with
`@GsrsRestApiController` along with setting the fields for `context` and `idHelper`
The abstract controller constructor also takes the context as a String so a good practice 
is to make your context a public static final String so you can reference it in both places
so they always match.

```java
@GsrsRestApiController(context =CvController.CONTEXT,  idHelper = IdHelpers.NUMBER)
public class CvController extends AbstractLegacyTextSearchGsrsEntityController<ControlledVocabulary, Long> {
    public static final String  CONTEXT = "vocabularies";

    
    public CvController() {
        super(CONTEXT);
    }

    @Autowired
    private ControlledVocabularyRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

// ... implement abstract methods

}
```

### Entity Context
The root part of the entity routes is also `api/v1/` followed by a String GSRS calls the `context`.  The Context
is what will differentiate your entity routes from all the other entities. 

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

To add an implementation just annotate your class as a `@Component` so it gets picked up by the Spring component scan:
  
  ```java
@Component
public class MyEntityProcessor implements EntityProcessor<Foo>{
     ...
} 
```

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

