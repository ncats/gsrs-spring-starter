package gsrs.scheduledTasks;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.persistence.Id;
import javax.persistence.Transient;

import gsrs.security.AdminService;
import gsrs.springUtils.StaticContextAccessor;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.common.util.TimeUtil;
import gov.nih.ncats.common.util.Unchecked;
import gsrs.model.GsrsApiAction;
import ix.core.EntityMapperOptions;
import ix.core.FieldResourceReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class SchedulerPlugin{

    
    //TODO: All of this is hacky to force missing parts of the scheduled tasks to work
    // but should be done as actual components.
    
    
    // #############################    
    // ###### START HACK ###########
    // #############################
    private static Scheduler scheduler;
    static {
        try {
            init();
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void init() throws SchedulerException {
        StdSchedulerFactory factory = new StdSchedulerFactory();
        scheduler = factory.getScheduler();
        scheduler.start();
    }
    
    public static void close() {
        if (scheduler != null) {
            try {
                scheduler.shutdown();
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    public static void submit (ScheduledTask task) {
        try {
            task.getJob().consume((j,t)->{
                scheduler.scheduleJob(j, t);
            });
        } catch (Exception e) {
            log.warn("Trouble scheduling Job", e);
        }
    }

    // #############################    
    // ###### END HACK ###########
    // #############################

    // This may not be needed anymore?
    private Map<String,ScheduledTask> tasks = new ConcurrentHashMap<>();



    
    public static class JobRunnable implements Job{
    	public JobRunnable(){}
		@Override
		public void execute(JobExecutionContext arg0) throws JobExecutionException {
			Runnable r = (Runnable)arg0.getJobDetail().getJobDataMap().get("run");
//			StaticContextAccessor.getBean(AdminService.class).runAsAdmin(()->r.run());
            r.run();
		}
    }
    
    
    private static AtomicLong idmaker = new AtomicLong(1l);
    private static Supplier<Long> idSupplier =()->{
        return idmaker.getAndIncrement();
    };
    
    public static class TaskListener{
        private Double p=null;
        private String msg = null;
        
        public Double getCompletePercentage(){
            return p;
        }
        public String getMessage(){
            return msg;
        }
        
        public TaskListener progress(double p){
            this.p=p;
            return this;
        }
        
        public TaskListener message(String msg){
            this.msg=msg;
            return this;
        }
        
        public TaskListener complete(){
            return progress(100);
        }
        
        public TaskListener start(){
            return progress(0);
        }
        
        
        
    }
    
//    @Entity
    @EntityMapperOptions(getSelfRel = "url")
    public static class ScheduledTask implements Toer<ScheduledTask>{
        @Transient
        private CachedSupplier<Authentication> admin = CachedSupplier.of(()-> StaticContextAccessor.getBean(AdminService.class).getAnyAdmin());
        @Transient
        @JsonUnwrapped
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, Object> additionalProperties = new HashMap<>();
        
    	private BiConsumer<JobStats, TaskListener> consumer=(s, l)->{};
    	
    	private Supplier<Boolean> check = ()->true;
    	
    	private CronScheduleBuilder sched=CronScheduleBuilder.dailyAtHourAndMinute(2, 1);
    	private String key= UUID.randomUUID().toString();
    	private String description = "Unnamed process";
    	
    	private Date lastStarted=null;
        private Date lastFinished=null;
        private Date lastSuccessfulStart, lastSuccesfulFinish;

        private int numberOfRuns=0;
        private boolean enabled=true;

        private FutureTask currentTask=null;
        
        private AtomicBoolean isRunning=new AtomicBoolean(false);

        private AtomicBoolean isLocked=new AtomicBoolean(false);
        
        private CronExpression cronExp= null;
        
        private TaskListener listener = new TaskListener();
        
        
        @JsonProperty("running")
        public boolean isRunning(){
            return isRunning.get();
        }
        
        
        public TaskListener getTaskDetails(){
            if(this.isRunning())
            return this.listener;
            return null;
        }
        
        @Id
        public Long id=idSupplier.get();
    	
    	
    	public ScheduledTask(BiConsumer<JobStats,TaskListener> consumer){

    	    this.consumer = consumer;
    	}
    	
    	public ScheduledTask(BiConsumer<JobStats, TaskListener> consumer, CronScheduleBuilder sched){
    		this.consumer=consumer;
    		this.sched=sched;
    	}
    	
    	public ScheduledTask runnable(Runnable r){
    		this.consumer=(s, l)->r.run();
    		return this;
    	}
    	
    	public ScheduledTask onlyIf(Supplier<Boolean> onlyIf){
            this.check=onlyIf;
            return this;
        }
    	
    	public ScheduledTask onlyIf(Predicate<ScheduledTask> onlyIf){
            this.check=()->onlyIf.test(this);
            return this;
        }
    	
    	private ScheduledTask schedule(CronScheduleBuilder s){
    	    this.cronExp=null;
    		this.sched=s;
    		return this;
    	}
    	
    	public ScheduledTask key(String key){
    		this.key=key;
    		return this;
    	}
    	
    	
    	@JsonIgnore
    	public CronScheduleBuilder getSchedule(){
    		return this.sched;
    	}
    	
    	//TODO katzelda April 2021 : move Date getNextRun() to controller as extra attribute
    	@JsonIgnore
    	public Trigger getSubmittedTrigger() throws SchedulerException{
    	    Trigger t=this.getJob().v();
    	    return scheduler.getTrigger(t.getKey());
    	}
//    	
    	
    	public Date getNextRun(){
    	    try {
                return getSubmittedTrigger().getNextFireTime();
            } catch (SchedulerException e) {
                e.printStackTrace();
                return null;
            }
    	}
    	
    	public String getCronSchedule(){
    	    if(cronExp==null)return null;
            return cronExp.getCronExpression();
        }
    	
    	
    	@JsonIgnore
    	public Runnable getRunnable() {
            return () -> {
                if (enabled) {
                    if (check.get()) {
                        StaticContextAccessor.getBean(AdminService.class).runAs(admin.get(), () -> runNow());

                    }
                }
            };
        }

    	public synchronized void runNow(){
    	    numberOfRuns++;
    	    //TODO figure out way to compute nextRun
    	    JobStats stats = new JobStats(numberOfRuns, lastStarted, lastFinished, getNextRun(),
    	                                lastSuccessfulStart, lastSuccesfulFinish);

    	    
//            JobStats stats = new JobStats(numberOfRuns, lastStarted, lastFinished, null,
//                    lastSuccessfulStart, lastSuccesfulFinish);
    	    isRunning.set(true);
            lastStarted= TimeUtil.getCurrentDate();
            this.listener.start();
            AtomicBoolean successful = new AtomicBoolean(false);
            try {
                Callable<Void> callable = () -> {
                    try {
                        consumer.accept(stats, this.listener);
                        return null;
                    }catch(Throwable t){
                        t.printStackTrace();
                        throw t;
                }
            };
                currentTask = new FutureTask<>(callable);
                currentTask.run();
                //if we get this far we didn't error out
                lastSuccessfulStart = lastStarted;
            }finally{
                lastFinished=TimeUtil.getCurrentDate();
                isRunning.set(false);
                this.listener.complete();
                if(lastSuccessfulStart == lastStarted){
                    lastSuccesfulFinish = lastFinished;
                }
            }
            isLocked.set(false);
    	}
    	
    	
    	public int getNumberOfRuns(){
    	    return this.numberOfRuns;
    	}
    	
    	public String getDescription(){
    	    return this.description;
    	}

    	public String getKey(){
    		return this.key;
    	}
    	
    	public Date getLastStarted(){
    	    return this.lastStarted;
    	}
    	
    	public Date getLastFinished(){
            return this.lastFinished;
        }
    	
    	@JsonProperty("enabled")
    	public boolean isEnabled(){
            return this.enabled;
        }
    	//TODO the self url is called url ? shouldn't it be self?
//    	@JsonProperty("url")
//        public String getSelfUrl () {
//            return Global.getNamespace()+"/scheduledjobs("+id+")";
//        }
//


        @JsonIgnore
        @GsrsApiAction(value ="@disable", serializeUrlOnly = true)
        public FieldResourceReference<ScheduledTask> getDisableAction () {
            if(!this.enabled){
                return null;
            }
            return FieldResourceReference.forField("@disable", this::disable);
        }

        @JsonIgnore
        @GsrsApiAction(value ="@cancel", serializeUrlOnly = true)
        public FieldResourceReference<ScheduledTask> getCancelAction () {
            if(!this.isRunning())return null;
            return FieldResourceReference.forField("@cancel", this::cancel);
        }
        @JsonIgnore
        @GsrsApiAction(value ="@enable", serializeUrlOnly = true)
        public FieldResourceReference<ScheduledTask> getEnableAction() {
    	    if(this.enabled){
    	        return null;
            }
            return FieldResourceReference.forField("@enable", this::enable);

        }
        @JsonIgnore
        @GsrsApiAction(value = "@execute", serializeUrlOnly = true)
        public FieldResourceReference<ScheduledTask> getExecuteAction () {
    	      if(this.isRunning())return null;
            return FieldResourceReference.forField("@execute", this::execute);

        }

        private ScheduledTask execute(){
            if(!isRunning() && !isLocked.get()){
                isLocked.set(true);
                //run as current user
                //when we submit the job to the forkJoin pool we probably
                //lose the current thread local authentication obj

                ForkJoinPool.commonPool().submit(()->StaticContextAccessor.getBean(AdminService.class).runAsAdmin(this::runNow));
            }
            return this;
        }
    	
    	public ScheduledTask dailyAtHourAndMinute(int hour, int minute){
    		return at(new CronExpressionBuilder()
    		               .everyDay()
    		               .atHourAndMinute(hour, minute));
    	}
    	
    	public ScheduledTask at(CronExpressionBuilder ceb){
            CronExpression cex= Unchecked.uncheck(()->ceb
                                       .buildExpression());
            return this.atCronTab(cex);
        }
    	
    	public ScheduledTask wrap(Consumer<Runnable> wrapper){
    	    BiConsumer<JobStats, TaskListener> c=consumer;
    	    consumer=(s,l)->{
    	        wrapper.accept(()->{
    	           c.accept(s, l);
    	        });
    	    };
            return this;
        }
    	
    	/**
    	 * See here for examples:
    	 * 
    	 * 
    	 * http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html
    	 * @param cron
    	 * @return
    	 */
    	public ScheduledTask atCronTab(String cron){
    		try {
                return atCronTab(new CronExpression(cron));
            } catch (ParseException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
    	}
    	
    	public ScheduledTask atCronTab(CronExpression cron){
    	    schedule(CronScheduleBuilder.cronSchedule(cron));
    	    cronExp=cron;
            return this;
        }
    	
    	public ScheduledTask atCronTab(CRON_EXAMPLE cron){
    		return atCronTab(cron.getString());
    	}
    	
    	private CachedSupplier<Tuple<JobDetail,Trigger>> submitted=CachedSupplier.of(()->{
    	    JobDataMap jdm = new JobDataMap();
            jdm.put("run", this.getRunnable());

            String key = this.getKey();
            JobDetail job = newJob(JobRunnable.class)
                                .setJobData(jdm)
                                .withIdentity(key)
                                .build();
            Trigger trigger = newTrigger().withIdentity(key)
                                .forJob(job)
                                .withSchedule(this.getSchedule())
                                .build();
            return Tuple.of(job,trigger);
    	});
    	
    	
    	@JsonIgnore
    	public Tuple<JobDetail,Trigger> getJob(){
    	    return submitted.get();
    	}
    	
    	public static ScheduledTask of(BiConsumer<JobStats, TaskListener> consumer){
    	    return new ScheduledTask(consumer);
        }
    	public static ScheduledTask of(Consumer<TaskListener> r){
            return new ScheduledTask((s,l) -> r.accept(l));
        }
    	
    	public static ScheduledTask of(Runnable r){
            return new ScheduledTask((s,l)->r.run());
        }

        public Map<String, Object> getAdditionalProperties() {
            return additionalProperties;
        }

        public void setAdditionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
        }

        public static enum CRON_EXAMPLE{
    		EVERY_SECOND         ("* * * * * ? *"),
    		EVERY_10_SECONDS     ("0/10 * * * * ? *"),
    		EVERY_MINUTE         ("0 * * * * ? *"),
    		EVERY_DAY_AT_2AM     ("0 0 2 * * ? *"),
    		EVERY_SATURDAY_AT_2AM("0 0 2 * * SAT *")
    		;
    		
    		private String c;
    		private CRON_EXAMPLE(String d){
    			c=d;
    		}
    		public String getString(){
    			return c;
    		}
    		
    		public CronScheduleBuilder getSchedule(){
    			return CronScheduleBuilder.cronSchedule(c);
    		}
    	}

        public ScheduledTask description(String description) {
            this.description=description;
            return this;
        }
        
        public ScheduledTask disable(){
            this.enabled=false;
            return this;
            
        }

        public ScheduledTask cancel(){
            System.out.println("in cancel method");
            if(currentTask !=null){
                System.out.println("calling cancel in currentTask");
                currentTask.cancel(true);
            }
            return this;

        }

        public ScheduledTask enable(){
            this.enabled=true;
            return this;
        }
        public ScheduledTask enable(boolean enabled){
            this.enabled=enabled;
            return this;
        }
//
//        public void submit(SchedulerPlugin plug){
//            plug.submit(this);
//        }
//
//        /**
//         * Submits the task to the default SchedulerPlugin
//         */
//        public void submit(){
//            this.submit(Play.application().plugin(SchedulerPlugin.class));
//        }
    }
    
//    public void submit (ScheduledTask task) {
//
//		try {
//		    tasks.put(task.getKey(), task);
//		    task.getJob().consume((j,t)->{
//		        scheduler.scheduleJob(j, t);
//		    });
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//    }
    
    public List<ScheduledTask> getTasks(){
        return this.tasks.values().stream().collect(Collectors.toList());
    }
    
    public ScheduledTask getTask(String key){
        return this.tasks.get(key);
    }
    
    
    public static class JobStats{
        private final int numberOfRuns;
        private final Date lastStarted, lastFinished, nextRun, lastSuccessfulStart, getLastSuccessfulFinish;

        public JobStats(int numberOfRuns, Date lastStarted, Date lastFinished, Date nextRun, Date lastSuccessfulStart, Date getLastSuccessfulFinish) {
            this.numberOfRuns = numberOfRuns;
            this.lastStarted = lastStarted;
            this.lastFinished = lastFinished;
            this.nextRun = nextRun;
            this.lastSuccessfulStart = lastSuccessfulStart;
            this.getLastSuccessfulFinish = getLastSuccessfulFinish;
        }

        public int getNumberOfRuns() {
            return numberOfRuns;
        }

        public Date getLastStarted() {
            return lastStarted;
        }

        public Date getLastFinished() {
            return lastFinished;
        }

        public Date getNextRun() {
            return nextRun;
        }

        public Date getLastSuccessfulStart() {
            return lastSuccessfulStart;
        }

        public Date getGetLastSuccessfulFinish() {
            return getLastSuccessfulFinish;
        }
    }

}
