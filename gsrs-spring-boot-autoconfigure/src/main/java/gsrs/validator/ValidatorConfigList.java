package gsrs.validator;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class ValidatorConfigList {
    @JsonUnwrapped
    private List<ValidatorConfig> configList;
}
