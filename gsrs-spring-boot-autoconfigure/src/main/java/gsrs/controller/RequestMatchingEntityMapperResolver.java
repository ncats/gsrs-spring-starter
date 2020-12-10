package gsrs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ix.core.controllers.EntityFactory;
import ix.core.models.BeanViews;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
public class RequestMatchingEntityMapperResolver implements ObjectMapperResolver {




    @Override
    public ObjectMapper getObjectMapper() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = sra.getRequest();
        return getMapperForView(request.getParameterValues("view"));

    }

    private EntityFactory.EntityMapper getMapperForView(String... view){
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
        }else {
            views.add(BeanViews.Compact.class);
        }

        return new EntityFactory.EntityMapper(views.toArray(new Class[0]));
    }

}