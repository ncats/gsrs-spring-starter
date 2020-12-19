package gsrs.controller.hateoas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.hateoas.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
@Data
@AllArgsConstructor
public class CollectionLink extends Link {
    private int count;
    @JsonUnwrapped
    private Link link;



}
