package gsrs.config;

import gov.nih.ncats.common.util.CachedSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

@Component
@ConfigurationProperties("gsrs.serviceinfo.api.endpoints")
@Slf4j
public class GsrsServiceInfoEndpointPathConfiguration {

    private List<String> entities;

    @Value("${spring.application.name}")
    private String service;

    public String getService() {
        return this.service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public List<String> getEntities () {
        return this.entities;
    }
    public void setEntities(List<String> entities) {
        this.entities = entities;
    }

    public List<ServiceInfoEndpointPathConfig> getEndpointsList(){
        return new ArrayList<>(endpoints.get());
    }

    private Map<String, ServiceInfoEndpointPathConfig> list = new HashMap<String, ServiceInfoEndpointPathConfig>();

    private static final Pattern regex1 = Pattern.compile("\\{serviceContext\\}");
    private static final Pattern regex2 = Pattern.compile("\\{entityContext\\}");

    public Map<String, ServiceInfoEndpointPathConfig> getList() {
        return list;
    }

    public void setList(Map<String, ServiceInfoEndpointPathConfig> list) {
        this.list = list;
    }

    private CachedSupplier<List<ServiceInfoEndpointPathConfig>> endpoints = CachedSupplier.of(()->{
        List<ServiceInfoEndpointPathConfig> expandedConfigs = new ArrayList<>();
        String reportTag = "ServiceInfoEndpointPathConfig";
        if (list == null) {
            return Collections.emptyList();
        }
        Map<String, ServiceInfoEndpointPathConfig> map = getList();
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        for (String k: map.keySet()) {
            map.get(k).setParentKey(k);
        }
        List<ServiceInfoEndpointPathConfig> configs = map.values().stream().collect(Collectors.toList());
        System.out.println(reportTag + " found before filtering: " + configs.size());
        configs = configs.stream().filter(c->!c.isDisabled()).sorted(Comparator.comparing(c->c.getOrder(),nullsFirst(naturalOrder()))).collect(Collectors.toList());
        System.out.println(reportTag + " active after filtering: " + configs.size());
        System.out.printf("%s|%s|%s|%s|%s\n", reportTag, "name", "parentKey", "order", "isDisabled");
        String service = getService();
        if (service==null)  {
            log.warn("Service is null, please make sure \"spring.application.name\" is set.");
        }

        for (ServiceInfoEndpointPathConfig config : configs) {
            List<String> myEntities = getEntities();
            if (myEntities == null) { myEntities = new ArrayList<>(); }
            // Should we allow override at by config object?
            // if (config.getEntities() != null) { myEntities = config.getEntities(); }
            String path1 = config.getPath();
            Matcher regexMatcher1 = regex1.matcher(path1);
            path1 = regexMatcher1.replaceAll(service);
            config.setPath(path1);
            String path2 = new String(path1);
            Matcher regexMatcher2 = regex2.matcher(path2);
            if(regexMatcher2.find()) {
                for(String entity: myEntities) {
                    String pathWithEntity = regexMatcher2.replaceAll(entity);
                    ServiceInfoEndpointPathConfig newConfig = null;
                    try {
                        newConfig = (ServiceInfoEndpointPathConfig) config.clone();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                    newConfig.setPath(pathWithEntity);
                    expandedConfigs.add(newConfig);
                }
            } else {
                expandedConfigs.add(config);
            }
        }
        System.out.println(reportTag + " active after expansion: " + expandedConfigs.size());
        for (ServiceInfoEndpointPathConfig config : expandedConfigs) {
            System.out.printf("%s|%s|%s|%s|%s\n", reportTag, config.getName(), config.getParentKey(), config.getOrder(), config.isDisabled());
        }
        return expandedConfigs;
    });

}