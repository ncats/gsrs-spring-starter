package gsrs.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ServiceInfoEndpointPathConfig implements EndpointPathConfig, Cloneable {
    private String name;
    private String parentKey;
    private String path;
    private Double order;
    private boolean disabled = false;
    private List<String> entities = new ArrayList<>();

    private Map<String, Object> parameters;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
