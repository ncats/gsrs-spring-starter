package gsrs.dataexchange.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Data
public class ProcessingActionConfig {

    private Class ProcessingActionClass;
    Map<String, Object> parameters;
}
