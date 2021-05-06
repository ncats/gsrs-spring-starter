package gsrs.controller;

import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduler.GsrsSchedulerTaskPropertiesConfiguration;
import ix.core.models.Edit;
import ix.core.util.EntityUtils;
import ix.core.util.pojopointer.PojoPointer;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExposesResourceFor(Edit.class)
@GsrsRestApiController(context = "scheduledJobs")
public class ScheduledTaskController {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private GsrsSchedulerTaskPropertiesConfiguration gsrsSchedulerConfiguration;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private EntityLinks entityLinks;

    @GetGsrsRestApiMapping("/@count")
    public int count(){
       return gsrsSchedulerConfiguration.getTasks().size();
    }

    @GetGsrsRestApiMapping({"({ID})","/{ID}"})
    public ResponseEntity<Object> getTaskByOrdinal(@PathVariable("ID") int index, @RequestParam Map<String,String> queryParameters){
        List<ScheduledTaskInitializer> list = gsrsSchedulerConfiguration.getTasks();
        if(index > list.size()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        Long indexAsLong = Long.valueOf(index);
        Optional<ScheduledTaskInitializer> task= list.stream().filter(s -> indexAsLong.equals(index)).findAny();
        if(!task.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        return new ResponseEntity(GsrsUnwrappedEntityModel.of(task.get()), HttpStatus.OK);
    }

    @GetGsrsRestApiMapping(value={"({id})/**", "/{id}/**" })
    public ResponseEntity<Object> getFieldById(@PathVariable("id") int index, @RequestParam Map<String, String> queryParameters, HttpServletRequest request){

        List<ScheduledTaskInitializer> list = gsrsSchedulerConfiguration.getTasks();
        if(index > list.size()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        Long indexAsLong = Long.valueOf(index);
        Optional<ScheduledTaskInitializer> task= list.stream().filter(s -> indexAsLong.equals(index)).findAny();
        if(!task.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        String field =GsrsControllerUtil.getEndWildCardMatchingPartOfUrl(request);

        EntityUtils.EntityWrapper<ScheduledTaskInitializer> ew = EntityUtils.EntityWrapper.of(task.get());

        PojoPointer pojoPointer = PojoPointer.fromURIPath(field);

        Optional<EntityUtils.EntityWrapper<?>> at = ew.at(pojoPointer);
        if(!at.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        //match old Play version of GSRS which either return JSON for an object or raw string?

        if(pojoPointer.isLeafRaw()){
            return new ResponseEntity<>(at.get().getRawValue(), HttpStatus.OK);
        }else{
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(at.get().getValue(), queryParameters), HttpStatus.OK);

        }
    }
}

