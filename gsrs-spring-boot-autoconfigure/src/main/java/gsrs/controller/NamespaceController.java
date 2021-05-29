package gsrs.controller;

import gsrs.service.GsrsEntityService;
import ix.core.models.Namespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.ExposesResourceFor;

@ExposesResourceFor(Namespace.class)
@GsrsRestApiController(context = NamespaceEntityService.CONTEXT, idHelper = IdHelpers.NUMBER)
public class NamespaceController extends AbstractGsrsEntityController<NamespaceController, Namespace, Long>{

    @Autowired
    private NamespaceEntityService namespaceEntityService;

    @Override
    protected GsrsEntityService<Namespace, Long> getEntityService() {
        return namespaceEntityService;
    }
}
