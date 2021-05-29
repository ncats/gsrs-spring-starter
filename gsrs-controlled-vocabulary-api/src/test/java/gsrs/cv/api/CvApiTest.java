package gsrs.cv.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import static org.junit.jupiter.api.Assertions.*;
//@SpringBootTest
@RestClientTest(ControlledVocabularyApi.class)
public class CvApiTest {

    @Autowired
    private MockRestServiceServer mockRestServiceServer;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    RestTemplateBuilder restTemplateBuilder;
    @Autowired
    private ControlledVocabularyApi api;

    @TestConfiguration
    static class Testconfig{
        @Bean
        public ControlledVocabularyApi controlledVocabularyApi(RestTemplateBuilder restTemplateBuilder){

            return new ControlledVocabularyApi(restTemplateBuilder, "http://example.com", new ObjectMapper());
        }
    }
    @BeforeEach
    public void setup(){
        this.mockRestServiceServer.reset();
    }

    @AfterEach
    public void verify(){
        this.mockRestServiceServer.verify();
    }

    @Test
    public void count() throws IOException {
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/vocabularies/@count"))
                .andRespond(withSuccess("5", MediaType.APPLICATION_JSON));

        assertEquals(5L, api.count());
    }
    @Test
    public void count0() throws IOException {
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/vocabularies/@count"))
                .andRespond(withSuccess("0", MediaType.APPLICATION_JSON));

        assertEquals(0L, api.count());
    }
    @Test

    public void countError() throws IOException {
        this.mockRestServiceServer
                .expect(requestTo("/api/v1/vocabularies/@count"))
                .andRespond(withServerError());

        assertThrows(IOException.class,()-> api.count());
    }
}
