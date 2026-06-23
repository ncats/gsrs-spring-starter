package gsrs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import ix.core.controllers.EntityFactory;
import ix.core.models.BeanViews;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Slf4j
public class RequestMatchingEntityMapperResolver implements ObjectMapperResolver {




    private final CachedSupplier<ObjectMapper> defaultMapper;

    public RequestMatchingEntityMapperResolver(){
        this.defaultMapper = CachedSupplier.runOnce(()-> EntityFactory.EntityMapper.COMPACT_ENTITY_MAPPER());
    }
    public RequestMatchingEntityMapperResolver(ObjectMapper defaultMapper) {
        Objects.requireNonNull(defaultMapper);
        this.defaultMapper = CachedSupplier.ofConstant(defaultMapper);
    }

    @Override
    public ObjectMapper getObjectMapper() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(sra ==null){
            return defaultMapper.get();
        }
        HttpServletRequest request = sra.getRequest();
        return getMapperForView(request.getParameterValues("view"));

    }

    private ObjectMapper getMapperForView(String... view){
        List<Class> views= new ArrayList<>();
        if (view != null) {

            Class<?>[] classes = BeanViews.class.getClasses();
            for (String a : view) {
                int matches = 0;
                for (Class<?> c : classes) {
                    if (a.equalsIgnoreCase(c.getSimpleName())) {
                        views.add(c);
                        ++matches;
                    }
                }

                if (matches == 0)
                    log.warn("Unsupported view: "+a);
            }
        }
        if(views.isEmpty()){
            return defaultMapper.get();
        }

        return new EntityFactory.EntityMapper(views.toArray(new Class[views.size()]));
    }

}