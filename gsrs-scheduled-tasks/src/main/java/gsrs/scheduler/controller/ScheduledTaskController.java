package gsrs.scheduler.controller;

import gsrs.controller.*;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.scheduler.GsrsSchedulerTaskPropertiesConfiguration;
import gsrs.security.hasAdminRole;
import ix.core.ResourceMethodReference;
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

@hasAdminRole
@ExposesResourceFor(SchedulerPlugin.ScheduledTask.class)
@GsrsRestApiController(context = "scheduledjobs")
public class ScheduledTaskController {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private GsrsSchedulerTaskPropertiesConfiguration gsrsSchedulerConfiguration;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;


    @GetGsrsRestApiMapping("/@count")
    public int count(){
       return gsrsSchedulerConfiguration.getTasks().size();
    }

    @GetGsrsRestApiMapping({"/",""})
    public ResponseEntity<Object> getTaskByOrdinal(@RequestParam(value ="top", defaultValue = "10") long top,
                                                   @RequestParam(value ="skip", defaultValue = "0") long skip,
                                                   @RequestParam Map<String,String> queryParameters){
        List<SchedulerPlugin.ScheduledTask> list = gsrsSchedulerConfiguration.getTasks();


        return new ResponseEntity(new PagedResult(list, top, skip, queryParameters), HttpStatus.OK);
    }

    @GetGsrsRestApiMapping({"({ID})","/{ID}"})
    public ResponseEntity<Object> getTaskByOrdinal(@PathVariable("ID") int index, @RequestParam Map<String,String> queryParameters){
        List<SchedulerPlugin.ScheduledTask> list = gsrsSchedulerConfiguration.getTasks();
        if(index > list.size()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        Long indexAsLong = Long.valueOf(index);
        Optional<SchedulerPlugin.ScheduledTask> task= list.stream().filter(s -> indexAsLong.equals(s.id)).findAny();
        if(!task.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        return new ResponseEntity(GsrsControllerUtil.enhanceWithView(task.get(), queryParameters), HttpStatus.OK);
    }
    @GetGsrsRestApiMapping({"({ID})/@execute","/{ID}/@execute"})
    public ResponseEntity<Object> executeTask(@PathVariable("ID") int index, @RequestParam Map<String,String> queryParameters){
        List<SchedulerPlugin.ScheduledTask> list = gsrsSchedulerConfiguration.getTasks();
        if(index > list.size()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        Long indexAsLong = Long.valueOf(index);
        Optional<SchedulerPlugin.ScheduledTask> task= list.stream().filter(s -> indexAsLong.equals(s.id)).findAny();
        if(!task.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        return new ResponseEntity(GsrsControllerUtil.enhanceWithView(task.get().getExecuteAction().invoke(), queryParameters), HttpStatus.OK);

    }

    @GetGsrsRestApiMapping({"({ID})/@cancel","/{ID}/@cancel"})
    public ResponseEntity<Object> cancelTask(@PathVariable("ID") int index, @RequestParam Map<String,String> queryParameters){
        List<SchedulerPlugin.ScheduledTask> list = gsrsSchedulerConfiguration.getTasks();
        if(index > list.size()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        Long indexAsLong = Long.valueOf(index);
        Optional<SchedulerPlugin.ScheduledTask> task= list.stream().filter(s -> indexAsLong.equals(s.id)).findAny();
        if(!task.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        return new ResponseEntity(GsrsUnwrappedEntityModel.of(task.get().getCancelAction().invoke()), HttpStatus.OK);

    }

    @GetGsrsRestApiMapping({"({ID})/@enable","/{ID}/@enable"})
    public ResponseEntity<Object> enableTask(@PathVariable("ID") int index, @RequestParam Map<String,String> queryParameters){
        List<SchedulerPlugin.ScheduledTask> list = gsrsSchedulerConfiguration.getTasks();
        if(index > list.size()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        Long indexAsLong = Long.valueOf(index);
        Optional<SchedulerPlugin.ScheduledTask> task= list.stream().filter(s -> indexAsLong.equals(s.id)).findAny();
        if(!task.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        return new ResponseEntity(GsrsUnwrappedEntityModel.of(task.get().getEnableAction().invoke()), HttpStatus.OK);

    }

    @GetGsrsRestApiMapping({"({ID})/@disable","/{ID}/@disable"})
    public ResponseEntity<Object> disableTask(@PathVariable("ID") int index, @RequestParam Map<String,String> queryParameters){
        List<SchedulerPlugin.ScheduledTask> list = gsrsSchedulerConfiguration.getTasks();
        if(index > list.size()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        Long indexAsLong = Long.valueOf(index);
        Optional<SchedulerPlugin.ScheduledTask> task= list.stream().filter(s -> indexAsLong.equals(s.id)).findAny();
        if(!task.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        return new ResponseEntity(GsrsUnwrappedEntityModel.of(task.get().getDisableAction().invoke()), HttpStatus.OK);

    }

    @GetGsrsRestApiMapping(value={"({id})/**", "/{id}/**" })
    public ResponseEntity<Object> getFieldById(@PathVariable("id") int index, @RequestParam Map<String, String> queryParameters, HttpServletRequest request){

        List<SchedulerPlugin.ScheduledTask> list = gsrsSchedulerConfiguration.getTasks();
        if(index > list.size()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }

        Long indexAsLong = Long.valueOf(index);
        Optional<SchedulerPlugin.ScheduledTask> task= list.stream().filter(s -> indexAsLong.equals(s.id)).findAny();
        if(!task.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }

        String field = GsrsControllerUtil.getEndWildCardMatchingPartOfUrl(request);

        EntityUtils.EntityWrapper<SchedulerPlugin.ScheduledTask> ew = EntityUtils.EntityWrapper.of(task.get());

        PojoPointer pojoPointer = PojoPointer.fromURIPath(field);

        Optional<EntityUtils.EntityWrapper<?>> at = ew.at(pojoPointer);
        if(!at.isPresent()){
            return gsrsControllerConfiguration.handleNotFound(queryParameters);
        }
        //match old Play version of GSRS which either return JSON for an object or raw string?
        EntityUtils.EntityWrapper<?> pojoEw= at.get();
        if(pojoPointer.isLeafRaw()){
            return new ResponseEntity<>(pojoEw.getRawValue(), HttpStatus.OK);
        }else{
            if(pojoEw.getValue() instanceof ResourceMethodReference){
                return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(((ResourceMethodReference)pojoEw.getValue()).invoke(), queryParameters), HttpStatus.OK);

            }
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(pojoEw.getValue(), queryParameters), HttpStatus.OK);

        }
    }
}

