package gsrs.autoconfigure;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class RoleConfiguration {
    private String name;
    private List<String> privileges = Collections.emptyList();
    private List<String> include= Collections.emptyList();
}
