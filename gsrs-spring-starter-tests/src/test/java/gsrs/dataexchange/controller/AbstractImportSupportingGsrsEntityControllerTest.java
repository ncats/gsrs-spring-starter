package gsrs.dataexchange.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.imports.ClientFriendlyImportAdapterConfig;
import gsrs.imports.ImportAdapterFactory;
import gsrs.imports.ImportUtilities;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.repository.PayloadRepository;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.controller.MyEntityService;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.Payload;
import ix.core.models.Text;
import ix.core.search.SearchResult;
import ix.ginas.models.GinasCommonData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.amqp.rabbit.transaction.RabbitTransactionManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@ActiveProfiles("test")
@GsrsJpaTest(classes = {GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class})
class AbstractImportSupportingGsrsEntityControllerTest extends AbstractGsrsJpaEntityJunit5Test {

    private final UUID uuid = UUID.randomUUID();

    private String oneTaskId;

    private final String originalFileName= "basicvalues.txt";
    @InjectMocks
    AbstractImportSupportingGsrsEntityController controller = new AbstractImportSupportingGsrsEntityController() {
        @Mock(name = "payloadService")
        public PayloadService payloadService = mock(PayloadService.class);

        @Mock(name="payloadRepository")
        public PayloadRepository payloadRepository = mock(PayloadRepository.class);

        private boolean ranSetup = false;

        @PostConstruct
        public void checkSetup() {
            log.trace("in checkSetup");
            if (ranSetup) return;
            try {
                when(payloadService.getPayloadAsInputStream(uuid)).thenReturn(Optional.of(new ByteArrayInputStream(dataInput.getBytes(Charset.defaultCharset()))));
            } catch (IOException ex) {
                log.error("Error setting up unit tests", ex);
            }
            AutowireHelper.getInstance().autowireAndProxy(controller);

            PlatformTransactionManager platformTransactionManager = new RabbitTransactionManager();
            //force the AbstractImportSupportingGsrsEntityController class to use a mock for payloadService
            super.payloadService = this.payloadService;
            Payload p = new Payload();
            p.id = UUID.randomUUID();

            try {
                log.trace("going to set up return of stream");
                when(super.payloadService.getPayloadAsInputStream(uuid)).thenReturn(Optional.of(new ByteArrayInputStream(dataInput.getBytes(Charset.defaultCharset()))));
                when( super.payloadService.createPayload( originalFileName, "text/plain", dataInput.getBytes(Charset.defaultCharset()), PayloadService.PayloadPersistType.TEMP))
                        .thenReturn( p);

            } catch (IOException e) {
                log.error("error mocking behavior for payload service");
                throw new RuntimeException(e);
            }
            ImportTaskMetaData task = createDummyTask();
            importTaskCache.put(UUID.fromString(task.getId()), task);
            oneTaskId = task.getId();
            ranSetup = true;
        }

        private ImportTaskMetaData createDummyTask() {
            ImportTaskMetaData task = new ImportTaskMetaData();
            task.setId(UUID.randomUUID().toString());
            task.setAdapter(DummyImportAdapterFactory.ADAPTER_NAME);
            return task;
        }

        final String dataInput = "chemical CCCCCC";

        @Override
        protected LegacyGsrsSearchService getlegacyGsrsSearchService() {
            checkSetup();
            return null;
        }

        @Override
        protected Object createSearchResponse(List results, SearchResult result, HttpServletRequest request) {
            checkSetup();
            return null;
        }

        @Override
        protected GsrsEntityService getEntityService() {
            checkSetup();
            return new MyEntityService();
        }

        @Override
        protected ImportAdapterFactory<GinasCommonData> fetchAdapterFactory(ImportTaskMetaData task) {
            checkSetup();
            return new DummyImportAdapterFactory();
        }

        @Override
        public List<String> getImportAdapterNames() {
            checkSetup();
            return Collections.singletonList(DummyImportAdapterFactory.ADAPTER_NAME);
        }

        public String getAdapterName() {
            checkSetup();
            return DummyImportAdapterFactory.ADAPTER_NAME;
        }

        public List<ImportAdapterFactory> getImportAdapters(){
            checkSetup();
            return Collections.singletonList( new DummyImportAdapterFactory());
        }

        @Override
        public List<ClientFriendlyImportAdapterConfig> getConfiguredImportAdapters() {
            ClientFriendlyImportAdapterConfig config = new ClientFriendlyImportAdapterConfig();
            config.setAdapterName(DummyImportAdapterFactory.ADAPTER_NAME);
            config.setAdapterKey("dummy");
            config.setFileExtensions(Collections.singletonList("txt"));
            config.setDescription("Dummy Adapter for Tests");
            return Collections.singletonList(config);
        }
    };

    @Test
    void executeTest() throws Exception {
        AbstractImportSupportingGsrsEntityController.ImportTaskMetaData<GinasCommonData> task = new AbstractImportSupportingGsrsEntityController.ImportTaskMetaData<>();
        task.setFileEncoding("UTF-9");
        task.setPayloadID(uuid);
        task.setAdapter("GSRS Object Adapter");
        Map<String, String> settingsMap = new HashMap<>();

        Stream<GinasCommonData> commonDataStream = controller.generateObjects(task, settingsMap);
        Assertions.assertEquals(DummyImportAdapter.getExpectedStreamSize(), commonDataStream.count());
    }

    @Test
    void fromTest() {
        Payload p = new Payload();
        p.id = UUID.randomUUID();
        p.size = 10001L;
        p.mimeType = "text/plain";
        p.name = "data1.txt";
        AbstractImportSupportingGsrsEntityController.ImportTaskMetaData metaData = controller.from(p);
        Assertions.assertEquals(metaData.getPayloadID(), p.id);
        Assertions.assertEquals(metaData.getMimeType(), p.mimeType);
        Assertions.assertEquals(metaData.getSize(), p.size);
        Assertions.assertEquals(metaData.getFilename(), p.name);
    }

    @Test
    void getImportAdapterFactoryTest1() {
        Optional<ImportAdapterFactory> factory= controller.getImportAdapterFactory("dummy");
        Assertions.assertTrue(factory.isPresent());
    }
    @Test
    void getImportAdapterFactoryTest2() {
        Optional<ImportAdapterFactory> factory= controller.getImportAdapterFactory("Dummy Import Adapter");
        Assertions.assertTrue(factory.isPresent());
    }

    @Test
    void getImportAdapterFactoryTest3() {
        Optional<ImportAdapterFactory> factory= controller.getImportAdapterFactory("Another Import Adapter");
        Assertions.assertFalse(factory.isPresent());
    }

    @Test
    void getSpecificImportAdapterTest1() throws IOException {
        Map<String, String> parameters = new HashMap<>();
        ResponseEntity<Object> responseEntity = controller.getSpecificImportAdapter("dummy", parameters);
        Assertions.assertNotNull(responseEntity.getBody());
        Assertions.assertEquals(ArrayList.class, ((GsrsUnwrappedEntityModel) responseEntity.getBody()).getObj().getClass() );
        log.trace("entity type: {}", responseEntity.getBody().getClass().getName());
    }

    @Test
    void getSpecificImportAdapterTest2() throws IOException {
        Map<String, String> parameters = new HashMap<>();
        String keyWithoutItem="unusual key";
        ResponseEntity<Object> responseEntity = controller.getSpecificImportAdapter(keyWithoutItem, parameters);
        ObjectNode messageNode = (ObjectNode) ((GsrsUnwrappedEntityModel) responseEntity.getBody()).getObj();
        Assertions.assertEquals("No adapter found with key "+keyWithoutItem, messageNode.get("message").asText());
        log.trace("entity type: {}", responseEntity.getBody().getClass().getName());
    }

/*
    @Test
    void handleImportTest() throws Exception {
        //hack to force autowiring
        controller.getImportAdapterNames();
        final MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn(originalFileName);
        String fileName = "text/basicvalues.txt";
        File textFile = (new ClassPathResource(fileName)).getFile();
        FileInputStream fileInputStream = new FileInputStream(textFile);
        BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
        byte[] data = new byte[(int) textFile.length()];
        fileInputStream.read(data);
        when(mockFile.getBytes()).thenReturn(data);

        Payload p = new Payload();
        p.id = UUID.randomUUID();
        try {
            log.trace("going to set up return of stream");
            //"chemical CCCCCC".getBytes(Charset.defaultCharset())
            when( controller.payloadService.createPayload( originalFileName, "text/plain", data, PayloadService.PayloadPersistType.TEMP))
                    .thenReturn( p);
            when(controller.payloadRepository.findById(p.id)).thenReturn(Optional.of(p));
        } catch (IOException e) {
            log.error("error mocking behavior for payload service");
            throw new RuntimeException(e);
        }


        Map<String, String> parameters = new HashMap<>();
        parameters.put("adapter", DummyImportAdapterFactory.ADAPTER_NAME);
        parameters.put("fileEncoding", "UTF-8");
        parameters.put("entityType", GinasCommonData.class.getName());
        ResponseEntity<Object> responseEntity = controller.handleImport(mockFile, parameters);
        Assertions.assertEquals(AbstractImportSupportingGsrsEntityController.ImportTaskMetaData.class,
                ((GsrsUnwrappedEntityModel) responseEntity.getBody()).getObj().getClass());
        AbstractImportSupportingGsrsEntityController.ImportTaskMetaData task = (AbstractImportSupportingGsrsEntityController.ImportTaskMetaData)
                ((GsrsUnwrappedEntityModel) responseEntity.getBody()).getObj();
        Assertions.assertEquals(DummyImportAdapterFactory.ADAPTER_NAME, task.getAdapter());
    }
*/

    @Test
    void getImportWhenNotFound() throws IOException {
        //hack to force autowiring
        controller.getImportAdapterNames();
        String id = UUID.randomUUID().toString();
        Map<String, String> parms = new HashMap<>();
        ResponseEntity<Object> responseEntity = controller.getImport(id, parms);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }


    @Test
    void getImportWhenFound() throws IOException {
        //hack to force autowiring
        controller.getImportAdapterNames();
        String id = oneTaskId;
        Map<String, String> parms = new HashMap<>();
        ResponseEntity<Object> responseEntity = controller.getImport(id, parms);
        AbstractImportSupportingGsrsEntityController.ImportTaskMetaData data = (AbstractImportSupportingGsrsEntityController.ImportTaskMetaData) ((GsrsUnwrappedEntityModel) responseEntity.getBody()).getObj();
        Assertions.assertEquals(DummyImportAdapterFactory.ADAPTER_NAME, data.getAdapter());
    }

    @Test
    void getImportPredictTest() {
        //hack to force autowiring, etc.
        controller.getImportAdapterNames();
        //todo: fill in something or delete

    }

    @Test
    public void testremoveMetadataFromDomainObjectJson(){
        String jsonBefore = "{\n" +
                "\t\"deprecated\": false,\n" +
                "\t\"definitionType\": \"PRIMARY\",\n" +
                "\t\"definitionLevel\": \"COMPLETE\",\n" +
                "\t\"substanceClass\": \"chemical\",\n" +
                "\t\"status\": \"pending\",\n" +
                "\t\"version\": \"1\",\n" +
                "\t\"names\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"deprecated\": false,\n" +
                "\t\t\t\"name\": \"34-74-2\",\n" +
                "\t\t\t\"type\": \"cn\",\n" +
                "\t\t\t\"domains\": [],\n" +
                "\t\t\t\"languages\": [\n" +
                "\t\t\t\t\"en\"\n" +
                "\t\t\t],\n" +
                "\t\t\t\"nameJurisdiction\": [],\n" +
                "\t\t\t\"nameOrgs\": [],\n" +
                "\t\t\t\"preferred\": false,\n" +
                "\t\t\t\"displayName\": false,\n" +
                "\t\t\t\"references\": [\n" +
                "\t\t\t\t\"7207ef8e-e6b6-45af-8845-5a85ffabbc32\"\n" +
                "\t\t\t],\n" +
                "\t\t\t\"access\": []\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"deprecated\": false,\n" +
                "\t\t\t\"name\": \"Monobutyl phthalate\",\n" +
                "\t\t\t\"type\": \"cn\",\n" +
                "\t\t\t\"domains\": [],\n" +
                "\t\t\t\"languages\": [\n" +
                "\t\t\t\t\"en\"\n" +
                "\t\t\t],\n" +
                "\t\t\t\"nameJurisdiction\": [],\n" +
                "\t\t\t\"nameOrgs\": [],\n" +
                "\t\t\t\"preferred\": false,\n" +
                "\t\t\t\"displayName\": true,\n" +
                "\t\t\t\"references\": [\n" +
                "\t\t\t\t\"7207ef8e-e6b6-45af-8845-5a85ffabbc32\"\n" +
                "\t\t\t],\n" +
                "\t\t\t\"access\": []\n" +
                "\t\t},\n" +
                "\t\t{\n" +
                "\t\t\t\"deprecated\": false,\n" +
                "\t\t\t\"name\": \"Mono-n-butyl-phthalate\",\n" +
                "\t\t\t\"type\": \"cn\",\n" +
                "\t\t\t\"domains\": [],\n" +
                "\t\t\t\"languages\": [\n" +
                "\t\t\t\t\"en\"\n" +
                "\t\t\t],\n" +
                "\t\t\t\"nameJurisdiction\": [],\n" +
                "\t\t\t\"nameOrgs\": [],\n" +
                "\t\t\t\"preferred\": false,\n" +
                "\t\t\t\"displayName\": false,\n" +
                "\t\t\t\"references\": [\n" +
                "\t\t\t\t\"7207ef8e-e6b6-45af-8845-5a85ffabbc32\"\n" +
                "\t\t\t],\n" +
                "\t\t\t\"access\": []\n" +
                "\t\t}\n" +
                "\t],\n" +
                "\t\"codes\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"deprecated\": false,\n" +
                "\t\t\t\"codeSystem\": \"CAS\",\n" +
                "\t\t\t\"code\": \"34-74-2\",\n" +
                "\t\t\t\"type\": \"PRIMARY\",\n" +
                "\t\t\t\"_isClassification\": false,\n" +
                "\t\t\t\"references\": [],\n" +
                "\t\t\t\"access\": []\n" +
                "\t\t}\n" +
                "\t],\n" +
                "\t\"notes\": [],\n" +
                "\t\"properties\": [],\n" +
                "\t\"relationships\": [],\n" +
                "\t\"references\": [\n" +
                "\t\t{\n" +
                "\t\t\t\"deprecated\": false,\n" +
                "\t\t\t\"uuid\": \"7207ef8e-e6b6-45af-8845-5a85ffabbc32\",\n" +
                "\t\t\t\"citation\": \"File \\\"nlm_small_1.sdf\\\" imported on Thu Sep 08 21:29:09 EDT 2022\",\n" +
                "\t\t\t\"docType\": \"CATALOG\",\n" +
                "\t\t\t\"publicDomain\": false,\n" +
                "\t\t\t\"tags\": [],\n" +
                "\t\t\t\"access\": []\n" +
                "\t\t}\n" +
                "\t],\n" +
                "\t\"tags\": [],\n" +
                "\t\"structure\": {\n" +
                "\t\t\"deprecated\": false,\n" +
                "\t\t\"molfile\": \"\\n  CDK     09082221292D\\n\\n 16 16  0  0  0  0  0  0  0  0999 V2000\\n   -0.3750   -8.1495    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -1.6740   -8.8995    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -1.6740  -10.3995    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -0.3750  -11.1495    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -2.9731  -11.1495    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -4.2721  -10.3995    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -4.2721   -8.8995    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n   -2.9731   -8.1495    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.9240   -8.8995    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n   -0.3750   -6.6495    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n   -0.3750  -12.6495    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.9240  -10.3995    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.9240   -5.8995    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    0.9240   -4.3995    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.2231   -3.6495    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n    2.2231   -2.1495    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\\n  1  2  1  0  0  0  0\\n  2  3  1  0  0  0  0\\n  3  4  1  0  0  0  0\\n  3  5  2  0  0  0  0\\n  5  6  1  0  0  0  0\\n  6  7  2  0  0  0  0\\n  7  8  1  0  0  0  0\\n  2  8  2  0  0  0  0\\n  1  9  2  0  0  0  0\\n  1 10  1  0  0  0  0\\n  4 11  2  0  0  0  0\\n  4 12  1  0  0  0  0\\n 13 14  1  0  0  0  0\\n 14 15  1  0  0  0  0\\n 15 16  1  0  0  0  0\\n 10 13  1  0  0  0  0\\nM  END\",\n" +
                "\t\t\"atropisomerism\": \"No\",\n" +
                "\t\t\"mwt\": 0.0,\n" +
                "\t\t\"properties\": [],\n" +
                "\t\t\"links\": [],\n" +
                "\t\t\"count\": 1,\n" +
                "\t\t\"references\": [\n" +
                "\t\t\t\"7207ef8e-e6b6-45af-8845-5a85ffabbc32\"\n" +
                "\t\t],\n" +
                "\t\t\"access\": []\n" +
                "\t},\n" +
                "\t\"moieties\": [],\n" +
                "\t\"_name\": \"Monobutyl phthalate\",\n" +
                "\t\"_approvalIDDisplay\": \"pending record\",\n" +
                "\t\"access\": [],\n" +
                "\t\"_metadata\": {\n" +
                "\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\"recordId\": \"453d8ba3-5d25-4f14-b8d5-bd94d25fcffe\",\n" +
                "\t\t\"version\": 1,\n" +
                "\t\t\"sourceName\": \"nlm_small_1.sdf\",\n" +
                "\t\t\"versionCreationDate\": 1662686949748,\n" +
                "\t\t\"importStatus\": \"staged\",\n" +
                "\t\t\"importType\": \"create\",\n" +
                "\t\t\"versionStatus\": \"current\",\n" +
                "\t\t\"validationStatus\": \"error\",\n" +
                "\t\t\"processStatus\": \"loaded\",\n" +
                "\t\t\"entityClassName\": \"ix.ginas.models.v1.Substance\",\n" +
                "\t\t\"keyValueMappings\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"mappingId\": \"2cccb34e-e6ed-4776-a05d-d420b3c45040\",\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"recordId\": \"453d8ba3-5d25-4f14-b8d5-bd94d25fcffe\",\n" +
                "\t\t\t\t\"key\": \"CAS NUMBER\",\n" +
                "\t\t\t\t\"value\": \"34-74-2\",\n" +
                "\t\t\t\t\"dataLocation\": \"Staging Area\",\n" +
                "\t\t\t\t\"entityClass\": \"substances\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"mappingId\": \"b17fdc94-5eab-4627-b13a-b4b1ac30c909\",\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"recordId\": \"453d8ba3-5d25-4f14-b8d5-bd94d25fcffe\",\n" +
                "\t\t\t\t\"key\": \"Substance Name\",\n" +
                "\t\t\t\t\"value\": \"34-74-2\",\n" +
                "\t\t\t\t\"dataLocation\": \"Staging Area\",\n" +
                "\t\t\t\t\"entityClass\": \"substances\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"mappingId\": \"349daeb4-a849-47c8-b6f5-21798e182e2e\",\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"recordId\": \"453d8ba3-5d25-4f14-b8d5-bd94d25fcffe\",\n" +
                "\t\t\t\t\"key\": \"Substance Name\",\n" +
                "\t\t\t\t\"value\": \"Mono-n-butyl-phthalate\",\n" +
                "\t\t\t\t\"dataLocation\": \"Staging Area\",\n" +
                "\t\t\t\t\"entityClass\": \"substances\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"mappingId\": \"643d8112-f407-4e72-bb21-e076041fc4c1\",\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"recordId\": \"453d8ba3-5d25-4f14-b8d5-bd94d25fcffe\",\n" +
                "\t\t\t\t\"key\": \"Substance Name\",\n" +
                "\t\t\t\t\"value\": \"Monobutyl phthalate\",\n" +
                "\t\t\t\t\"dataLocation\": \"Staging Area\",\n" +
                "\t\t\t\t\"entityClass\": \"substances\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"mappingId\": \"f6d0db70-a1df-4e82-bf8a-6a67eebf2fa6\",\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"recordId\": \"453d8ba3-5d25-4f14-b8d5-bd94d25fcffe\",\n" +
                "\t\t\t\t\"key\": \"Primary Name\",\n" +
                "\t\t\t\t\"value\": \"34-74-2\",\n" +
                "\t\t\t\t\"dataLocation\": \"Staging Area\",\n" +
                "\t\t\t\t\"entityClass\": \"substances\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"mappingId\": \"29d1357f-1b54-4766-a8b6-03cc852af61c\",\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"recordId\": \"453d8ba3-5d25-4f14-b8d5-bd94d25fcffe\",\n" +
                "\t\t\t\t\"key\": \"Definitional Hash - Layer 1\",\n" +
                "\t\t\t\t\"value\": \"806d83c4b5da31e7aef450e58ee70c29d49a3869\",\n" +
                "\t\t\t\t\"dataLocation\": \"Staging Area\",\n" +
                "\t\t\t\t\"entityClass\": \"substances\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"mappingId\": \"279bca4f-f8ed-4cd0-b41e-076398d8fa75\",\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"recordId\": \"453d8ba3-5d25-4f14-b8d5-bd94d25fcffe\",\n" +
                "\t\t\t\t\"key\": \"Definitional Hash - Layer 2\",\n" +
                "\t\t\t\t\"value\": \"c93e97c8e6c800a4513abd517fbd139faea219e8\",\n" +
                "\t\t\t\t\"dataLocation\": \"Staging Area\",\n" +
                "\t\t\t\t\"entityClass\": \"substances\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"mappingId\": \"9a2f0ab5-f53c-4d6d-a160-bb0470ca9905\",\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"recordId\": \"453d8ba3-5d25-4f14-b8d5-bd94d25fcffe\",\n" +
                "\t\t\t\t\"key\": \"CODE\",\n" +
                "\t\t\t\t\"value\": \"34-74-2\",\n" +
                "\t\t\t\t\"dataLocation\": \"Staging Area\",\n" +
                "\t\t\t\t\"entityClass\": \"substances\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"mappingId\": \"1cc7ebfa-d5bb-4cf7-ac5c-ea794826e4a7\",\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"recordId\": \"453d8ba3-5d25-4f14-b8d5-bd94d25fcffe\",\n" +
                "\t\t\t\t\"key\": \"UUID\",\n" +
                "\t\t\t\t\"value\": \"dd0ef046-77fb-492b-9819-af0f9c81037f\",\n" +
                "\t\t\t\t\"dataLocation\": \"Staging Area\",\n" +
                "\t\t\t\t\"entityClass\": \"substances\"\n" +
                "\t\t\t}\n" +
                "\t\t],\n" +
                "\t\t\"validations\": [\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"version\": 1,\n" +
                "\t\t\t\t\"validationType\": \"error\",\n" +
                "\t\t\t\t\"validationDate\": 1662686950240,\n" +
                "\t\t\t\t\"validationMessage\": \"Public records must have a PUBLIC DOMAIN reference with a 'PUBLIC_DOMAIN_RELEASE' tag\",\n" +
                "\t\t\t\t\"validationJson\": \"ValidationResponse{validationMessages=[INFO: Substance has no UUID, will generated uuid:\\\"980402e4-4a0d-4cd6-ac1d-4692af12019d\\\", ERROR: The name :\\\"34-74-2\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Monobutyl phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Mono-n-butyl-phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., INFO: Substances should have exactly one (1) display name, Default to using:Monobutyl phthalate, INFO: No moieties found in submission. They will be generated automatically., ERROR: Public records must have a PUBLIC DOMAIN reference with a 'PUBLIC_DOMAIN_RELEASE' tag, ERROR: Public substance definitions require a public definitional reference.  Please add one.], valid=false, newObject=Substance{uuid=dd0ef046-77fb-492b-9819-af0f9c81037f, substanceClass=chemical, version='1'}}\",\n" +
                "\t\t\t\t\"validationId\": \"6f08543c-0f07-4a42-acf4-2f0f6d3e82bc\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"version\": 1,\n" +
                "\t\t\t\t\"validationType\": \"error\",\n" +
                "\t\t\t\t\"validationDate\": 1662686950227,\n" +
                "\t\t\t\t\"validationMessage\": \"The name :\\\"34-74-2\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public.\",\n" +
                "\t\t\t\t\"validationJson\": \"ValidationResponse{validationMessages=[INFO: Substance has no UUID, will generated uuid:\\\"980402e4-4a0d-4cd6-ac1d-4692af12019d\\\", ERROR: The name :\\\"34-74-2\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Monobutyl phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Mono-n-butyl-phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., INFO: Substances should have exactly one (1) display name, Default to using:Monobutyl phthalate, INFO: No moieties found in submission. They will be generated automatically., ERROR: Public records must have a PUBLIC DOMAIN reference with a 'PUBLIC_DOMAIN_RELEASE' tag, ERROR: Public substance definitions require a public definitional reference.  Please add one.], valid=false, newObject=Substance{uuid=dd0ef046-77fb-492b-9819-af0f9c81037f, substanceClass=chemical, version='1'}}\",\n" +
                "\t\t\t\t\"validationId\": \"1c5c4578-1dae-4eed-8956-503876dd497c\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"version\": 1,\n" +
                "\t\t\t\t\"validationType\": \"error\",\n" +
                "\t\t\t\t\"validationDate\": 1662686950234,\n" +
                "\t\t\t\t\"validationMessage\": \"The name :\\\"Monobutyl phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public.\",\n" +
                "\t\t\t\t\"validationJson\": \"ValidationResponse{validationMessages=[INFO: Substance has no UUID, will generated uuid:\\\"980402e4-4a0d-4cd6-ac1d-4692af12019d\\\", ERROR: The name :\\\"34-74-2\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Monobutyl phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Mono-n-butyl-phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., INFO: Substances should have exactly one (1) display name, Default to using:Monobutyl phthalate, INFO: No moieties found in submission. They will be generated automatically., ERROR: Public records must have a PUBLIC DOMAIN reference with a 'PUBLIC_DOMAIN_RELEASE' tag, ERROR: Public substance definitions require a public definitional reference.  Please add one.], valid=false, newObject=Substance{uuid=dd0ef046-77fb-492b-9819-af0f9c81037f, substanceClass=chemical, version='1'}}\",\n" +
                "\t\t\t\t\"validationId\": \"38903496-ea8a-4de9-b2b9-31e9a8c2b375\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"version\": 1,\n" +
                "\t\t\t\t\"validationType\": \"error\",\n" +
                "\t\t\t\t\"validationDate\": 1662686950237,\n" +
                "\t\t\t\t\"validationMessage\": \"The name :\\\"Mono-n-butyl-phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public.\",\n" +
                "\t\t\t\t\"validationJson\": \"ValidationResponse{validationMessages=[INFO: Substance has no UUID, will generated uuid:\\\"980402e4-4a0d-4cd6-ac1d-4692af12019d\\\", ERROR: The name :\\\"34-74-2\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Monobutyl phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Mono-n-butyl-phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., INFO: Substances should have exactly one (1) display name, Default to using:Monobutyl phthalate, INFO: No moieties found in submission. They will be generated automatically., ERROR: Public records must have a PUBLIC DOMAIN reference with a 'PUBLIC_DOMAIN_RELEASE' tag, ERROR: Public substance definitions require a public definitional reference.  Please add one.], valid=false, newObject=Substance{uuid=dd0ef046-77fb-492b-9819-af0f9c81037f, substanceClass=chemical, version='1'}}\",\n" +
                "\t\t\t\t\"validationId\": \"5a447953-3ac4-4dc5-8a3c-d9943156f0c9\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"version\": 1,\n" +
                "\t\t\t\t\"validationType\": \"error\",\n" +
                "\t\t\t\t\"validationDate\": 1662686950242,\n" +
                "\t\t\t\t\"validationMessage\": \"Public substance definitions require a public definitional reference.  Please add one.\",\n" +
                "\t\t\t\t\"validationJson\": \"ValidationResponse{validationMessages=[INFO: Substance has no UUID, will generated uuid:\\\"980402e4-4a0d-4cd6-ac1d-4692af12019d\\\", ERROR: The name :\\\"34-74-2\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Monobutyl phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Mono-n-butyl-phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., INFO: Substances should have exactly one (1) display name, Default to using:Monobutyl phthalate, INFO: No moieties found in submission. They will be generated automatically., ERROR: Public records must have a PUBLIC DOMAIN reference with a 'PUBLIC_DOMAIN_RELEASE' tag, ERROR: Public substance definitions require a public definitional reference.  Please add one.], valid=false, newObject=Substance{uuid=dd0ef046-77fb-492b-9819-af0f9c81037f, substanceClass=chemical, version='1'}}\",\n" +
                "\t\t\t\t\"validationId\": \"5e3650e4-c974-4a2f-b6f9-291d509a176e\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"version\": 1,\n" +
                "\t\t\t\t\"validationType\": \"info\",\n" +
                "\t\t\t\t\"validationDate\": 1662686950244,\n" +
                "\t\t\t\t\"validationMessage\": \"Substance has no UUID, will generated uuid:\\\"980402e4-4a0d-4cd6-ac1d-4692af12019d\\\"\",\n" +
                "\t\t\t\t\"validationJson\": \"ValidationResponse{validationMessages=[INFO: Substance has no UUID, will generated uuid:\\\"980402e4-4a0d-4cd6-ac1d-4692af12019d\\\", ERROR: The name :\\\"34-74-2\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Monobutyl phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Mono-n-butyl-phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., INFO: Substances should have exactly one (1) display name, Default to using:Monobutyl phthalate, INFO: No moieties found in submission. They will be generated automatically., ERROR: Public records must have a PUBLIC DOMAIN reference with a 'PUBLIC_DOMAIN_RELEASE' tag, ERROR: Public substance definitions require a public definitional reference.  Please add one.], valid=false, newObject=Substance{uuid=dd0ef046-77fb-492b-9819-af0f9c81037f, substanceClass=chemical, version='1'}}\",\n" +
                "\t\t\t\t\"validationId\": \"2254ae83-2d23-4d4c-aee1-3c04a790a039\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"version\": 1,\n" +
                "\t\t\t\t\"validationType\": \"info\",\n" +
                "\t\t\t\t\"validationDate\": 1662686950246,\n" +
                "\t\t\t\t\"validationMessage\": \"Substances should have exactly one (1) display name, Default to using:Monobutyl phthalate\",\n" +
                "\t\t\t\t\"validationJson\": \"ValidationResponse{validationMessages=[INFO: Substance has no UUID, will generated uuid:\\\"980402e4-4a0d-4cd6-ac1d-4692af12019d\\\", ERROR: The name :\\\"34-74-2\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Monobutyl phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Mono-n-butyl-phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., INFO: Substances should have exactly one (1) display name, Default to using:Monobutyl phthalate, INFO: No moieties found in submission. They will be generated automatically., ERROR: Public records must have a PUBLIC DOMAIN reference with a 'PUBLIC_DOMAIN_RELEASE' tag, ERROR: Public substance definitions require a public definitional reference.  Please add one.], valid=false, newObject=Substance{uuid=dd0ef046-77fb-492b-9819-af0f9c81037f, substanceClass=chemical, version='1'}}\",\n" +
                "\t\t\t\t\"validationId\": \"d63ef5e1-c63d-447b-97cf-4a40e8881bcd\"\n" +
                "\t\t\t},\n" +
                "\t\t\t{\n" +
                "\t\t\t\t\"instanceId\": \"dcdeb92e-f1b1-451f-a780-a76c8a50b0b8\",\n" +
                "\t\t\t\t\"version\": 1,\n" +
                "\t\t\t\t\"validationType\": \"info\",\n" +
                "\t\t\t\t\"validationDate\": 1662686950249,\n" +
                "\t\t\t\t\"validationMessage\": \"No moieties found in submission. They will be generated automatically.\",\n" +
                "\t\t\t\t\"validationJson\": \"ValidationResponse{validationMessages=[INFO: Substance has no UUID, will generated uuid:\\\"980402e4-4a0d-4cd6-ac1d-4692af12019d\\\", ERROR: The name :\\\"34-74-2\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Monobutyl phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., ERROR: The name :\\\"Mono-n-butyl-phthalate\\\" needs an unprotected reference marked \\\"Public Domain\\\" in order to be made public., INFO: Substances should have exactly one (1) display name, Default to using:Monobutyl phthalate, INFO: No moieties found in submission. They will be generated automatically., ERROR: Public records must have a PUBLIC DOMAIN reference with a 'PUBLIC_DOMAIN_RELEASE' tag, ERROR: Public substance definitions require a public definitional reference.  Please add one.], valid=false, newObject=Substance{uuid=dd0ef046-77fb-492b-9819-af0f9c81037f, substanceClass=chemical, version='1'}}\",\n" +
                "\t\t\t\t\"validationId\": \"bca76401-563d-4c25-b3cb-41611ced7de6\"\n" +
                "\t\t\t}\n" +
                "\t\t],\n" +
                "\t\t\"dataFormat\": \"application/octet-stream\",\n" +
                "\t\t\"importAdapter\": \"SDF Adapter\",\n" +
                "\t\t\"access\": []\n" +
                "\t}\n" +
                "}";

        String cleanJson = ImportUtilities.removeMetadataFromDomainObjectJson(jsonBefore);
        Assertions.assertFalse(cleanJson.contains("_metadata"));
        Assertions.assertTrue(cleanJson.length()<jsonBefore.length());
        Assertions.assertFalse(cleanJson.contains("_metadata"));
    }

    @Test
    public void testFromText() throws JsonProcessingException {
        Text text = new Text();
        text.setValue(createSimpleConfig());
        AbstractImportSupportingGsrsEntityController.ImportTaskMetaData recreatedMetadata = AbstractImportSupportingGsrsEntityController.ImportTaskMetaData.fromText(text);
        Assertions.assertNotNull(recreatedMetadata);
    }
    @Test
    void updateImport() {
    }

    @Test
    void executePreview() {
    }

    @Test
    void executeValidate() {
    }

    @Test
    void findMatches() {
    }

    @Test
    void deleteRecord() {
    }

    @Test
    void retrieveRecord() {
    }

    @Test
    void getInstances() {
    }

    @Test
    void retrieveInstanceData() {
    }

    @Test
    void updateImportData() {
    }

    @Test
    void executeImport() {
    }

    private String createSimpleConfig()  {
        AbstractImportSupportingGsrsEntityController.ImportTaskMetaData metaData =new AbstractImportSupportingGsrsEntityController.ImportTaskMetaData();
        metaData.setId(UUID.randomUUID().toString());
        metaData.setEntityType("Substance");
        metaData.setAdapter("SDF Importer");
        metaData.setMimeType("chemical/x-mdl-sdfile");
        ObjectNode adapterSettings = JsonNodeFactory.instance.objectNode();
        ArrayNode actions = JsonNodeFactory.instance.arrayNode();
        ObjectNode action1 = JsonNodeFactory.instance.objectNode();
        action1.put("actionName", "structure_and_moieties");
        ObjectNode parameterNode = JsonNodeFactory.instance.objectNode();
        parameterNode.put("molfile", "{{molfile}}");
        ArrayNode refArray = JsonNodeFactory.instance.arrayNode();
        refArray.add("[[UUID_1]]");
        parameterNode.set("referenceUUIDs", refArray);
        action1.set("actionParameters", parameterNode);
        action1.put("label", "Import Structure Action");
        actions.add(action1);
        adapterSettings.set("actions", actions);
        metaData.setAdapterSettings(adapterSettings);
        metaData.setAdapterSchema(JsonNodeFactory.instance.objectNode());
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(metaData);
        } catch (JsonProcessingException e) {
            log.error("Error serializing ImportTaskMetaData", e);
            throw new RuntimeException(e);
        }
    }
}