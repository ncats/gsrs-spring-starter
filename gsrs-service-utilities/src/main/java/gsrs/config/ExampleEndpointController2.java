package gsrs.config;



import lombok.Data;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RestControllerEndpoint(id = "stuff")
public class ExampleEndpointController2 {

    @Data
    class Information {
        String one = "One";
        String two = "Two";
    }

    @GetMapping("/xinfo")
    public Information getHello() {
        return new Information();
    }
}
