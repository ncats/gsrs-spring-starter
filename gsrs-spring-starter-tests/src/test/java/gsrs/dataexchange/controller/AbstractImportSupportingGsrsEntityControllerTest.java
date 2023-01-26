package gsrs.dataexchange.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.imports.ClientFriendlyImportAdapterConfig;
import gsrs.imports.ImportAdapterFactory;
import gsrs.legacy.LegacyGsrsSearchService;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.controller.MyEntityService;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.Payload;
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

    private String originalFileName= "basicvalues.txt";
    @InjectMocks
    AbstractImportSupportingGsrsEntityController controller = new AbstractImportSupportingGsrsEntityController() {
        @Mock(name = "payloadService")
        public PayloadService payloadService = mock(PayloadService.class);

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
            when( controller.payloadService.createPayload( originalFileName, "text/plain", "chemical CCCCCC".getBytes(Charset.defaultCharset()), PayloadService.PayloadPersistType.TEMP))
                    .thenReturn( p);

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
}