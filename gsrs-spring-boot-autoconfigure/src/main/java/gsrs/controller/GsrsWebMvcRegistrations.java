package gsrs.controller;

import gsrs.controller.hateoas.DefaultGsrsEntityToControllerMapper;
import gsrs.springUtils.AutowireHelper;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.*;

class GsrsWebMvcRegistrations implements WebMvcRegistrations {

    public GsrsWebMvcRegistrations(DefaultGsrsEntityToControllerMapper entityToControllerMapper) {
        this.entityToControllerMapper = entityToControllerMapper;
    }

    private final DefaultGsrsEntityToControllerMapper entityToControllerMapper;


    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {

        return new RequestMappingHandlerMapping() {
            private final static String API_BASE_PATH = "api/v";


            @Override
            protected boolean isHandler(Class<?> beanType) {

                return AnnotatedElementUtils.hasAnnotation(beanType, GsrsRestApiController.class) || super.isHandler(beanType);

            }

            @Override
            protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
                //the handler is often a String for the package name... convert to real class
                Class<?> handlerType = (handler instanceof String ?
                        obtainApplicationContext().getType((String) handler) : handler.getClass());

                if (handlerType == null) {
                    return;
                }
                Class<?> beanType = ClassUtils.getUserClass(handlerType);
                GsrsRestApiController gsrsRestApiAnnotation = AnnotationUtils.findAnnotation(beanType, GsrsRestApiController.class);

                //we need to use the findMergedAnnotation to combine all the aliased fields
                GsrsRestApiRequestMapping gsrsMapping = AnnotatedElementUtils.findMergedAnnotation(method, GsrsRestApiRequestMapping.class);
                ExposesResourceFor annotation = (ExposesResourceFor)AnnotationUtils.findAnnotation(beanType, ExposesResourceFor.class);
                if (annotation != null) {
                    entityToControllerMapper.addController(annotation.value(), beanType);
                }
                int[] versions = new int[]{1};
                if (gsrsMapping != null) {
                    versions = gsrsMapping.apiVersions();
                }

                //we want to do 2 things
                //1. add the api base to everything
                //2. if there's an ID use the regex for that entity type
                Set<String> apiBasePatterns = new HashSet<>();
                List<Set<String>> apiBasesByVersions = new ArrayList<>();
                for (int i = 0; i < versions.length; i++) {
                    Set<String> patterns = new PatternsRequestCondition(API_BASE_PATH + versions[i])
                            .combine(mapping.getPatternsCondition()).getPatterns();
                    apiBasePatterns.addAll(patterns);
                    apiBasesByVersions.add(patterns);
                }

                //this will be overridden if we have get or post mappings
                PatternsRequestCondition apiPattern = new PatternsRequestCondition(apiBasePatterns.toArray(new String[apiBasePatterns.size()]));
                if (gsrsMapping != null) {
                    if (gsrsRestApiAnnotation != null) {


                        IdHelpers commonIdHelper = gsrsRestApiAnnotation.idHelper();
                        IdHelper idHelper;
                        if (commonIdHelper == IdHelpers.CUSTOM) {
                            String className = gsrsRestApiAnnotation.customIdHelperClassName();
                            try {
                                if (className == null || className.isEmpty()) {

                                    idHelper = gsrsRestApiAnnotation.customIdHelperClass().newInstance();


                                } else {
                                    idHelper = (IdHelper) ClassUtils.forName(className, getClass().getClassLoader()).newInstance();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw new IllegalStateException("error instantiating idHelper class", e);
                            }
                            //inject anything if needed
                            idHelper = AutowireHelper.getInstance().autowireAndProxy(idHelper);
                        } else {
                            idHelper = commonIdHelper;
                        }

                        String idPlaceHolder = gsrsMapping.idPlaceholder();
                        String notIdPlaceHolder = gsrsMapping.notIdPlaceholder();

                        Set<String> updatedPatterns = new LinkedHashSet<>();
                        //Spring adds leading / to the paths if you forgot to put it
                        //but GSRS doesn't want to have it sometimes
                        // for example api/v1/context( id)  not api/v1/context/( id)
                        //so check to see if we put a leading slash and if not get rid
                        // of the leading slash which is now a middle slash

                        //now we do this for each version too!
                        Set<String> adjustedPatterns = new HashSet<>();
                        for (Set<String> basePatternPerVersion : apiBasesByVersions) {

                            String[] paths = gsrsMapping.value();

                            if (paths.length == 0) {
                                //no path it's probably a post to the root
                                adjustedPatterns.addAll(basePatternPerVersion);
                            } else {
                                Iterator<String> patternIter = basePatternPerVersion.iterator();

                                Iterator<String> definedInAnnotation = Arrays.asList(paths).iterator();

                                while (patternIter.hasNext()) {
                                    String pattern = patternIter.next();
                                    String defined = definedInAnnotation.next();

                                    if (!defined.isEmpty() && defined.charAt(0) != '/') {
                                        int offset = pattern.lastIndexOf(defined);
                                        if (offset > -1 && pattern.charAt(offset - 1) == '/') {

                                            String before = pattern.substring(0, offset - 1);
                                            adjustedPatterns.add(before + defined);
                                        }
                                    } else {
                                        adjustedPatterns.add(pattern);
                                    }
                                }
                            }
                        }
                        for (String route : adjustedPatterns) {
                            String updatedRoute = idHelper.replaceId(route, idPlaceHolder);
                            updatedRoute = idHelper.replaceInverseId(updatedRoute, notIdPlaceHolder);

                            updatedPatterns.add(updatedRoute);

                        }
                        apiPattern = new PatternsRequestCondition(updatedPatterns.toArray(new String[updatedPatterns.size()]));

                    }

                    mapping = new RequestMappingInfo(mapping.getName(), apiPattern,
                            mapping.getMethodsCondition(), mapping.getParamsCondition(),
                            mapping.getHeadersCondition(), mapping.getConsumesCondition(),
                            mapping.getProducesCondition(), mapping.getCustomCondition());

                }


                super.registerHandlerMethod(handler, method, mapping);

            }

            ;
        };
    }
}
