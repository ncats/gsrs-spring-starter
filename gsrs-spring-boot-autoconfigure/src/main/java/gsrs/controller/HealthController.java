package gsrs.controller;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.context.event.EventListener;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.cache.GsrsCache;
import gsrs.controller.hateoas.GsrsControllerInfo;
import gsrs.controller.hateoas.GsrsEntityToControllerMapper;
import lombok.Data;

@RestController
@ExposesResourceFor(HealthController.UpStatus.class)
public class HealthController {

    @Autowired
    private GsrsEntityToControllerMapper controllerMapper;

    @Autowired
    private List<DataSource> dataSourcesRaw;

    @Autowired(required = false)
    private GsrsCache gsrsCache;

    private long startTime =System.currentTimeMillis();

    @EventListener(ApplicationReadyEvent.class)
    public void startTime(ApplicationReadyEvent event){
        startTime = TimeUtil.getCurrentTimeMillis();
    }
    @Data
    public static class UpStatus{
        public static UpStatus INSTANCE = new UpStatus();

        public String status = "UP";



    }
    @GetMapping("api")
    public ResponseEntity landingPage(){
	//String script = "var _w=window;_w.$||(_w.$=function(e){var n={off:function(){},remove:function(){for(var n=document.getElementsByTagName(e),o=0;o<n.length;o++)n[o].parentNode.removeChild(n[o])}};return n});";
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
                .body("<html><head><title>GSRS landing page</title><head><script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js\"></script></head><body><h1>welcome to GSRS</h1</body></html>");
    }

    @GetMapping("api/v1")
    public List<GsrsControllerInfo> getControllerInfo(){
        return controllerMapper.getControllerInfos().collect(Collectors.toList());
    }

    @GetMapping("api/v1/health")
    public UpStatus isUp(){
        return UpStatus.INSTANCE;
    }

    @GetMapping("api/v1/health/info")
//    @hasAdminRole
    public HealthController.Application info() throws Exception{
        int[] uptime = uptime(startTime);
        return Application.createFromCurrentRuntime(uptime, startTime, gsrsCache, dataSourcesRaw);
    }

    public int[] uptime (long startTime) {
        int[] ups = null;
        if (startTime>0) {
            ups = new int[3];
            // epoch in seconds
            long u = (TimeUtil.getCurrentTimeMillis()- startTime)/1000;
            ups[0] = (int)(u/3600); // hour
            ups[1] = (int)((u/60) % 60); // min
            ups[2] = (int)((u%60)); // sec
        }
        return ups;
    }

    public static class RuntimeInfo{
        public long availableProcessors;
        public long freeMemory;
        public long totalMemory;
        public long maxMemory;

        public RuntimeInfo(){}
        public RuntimeInfo(Runtime rt){
            this.availableProcessors = rt.availableProcessors();
            this.freeMemory = rt.freeMemory();
            this.maxMemory = rt.maxMemory();
            this.totalMemory = rt.totalMemory();
        }
    }
    public static class Application{

        public Date epoch;
        public UptimeInfo uptime;
        public long threads;
        public long runningThreads;
        public String javaVersion;

        public String hostname;
        public RuntimeInfo runtime;

        public List<DataBaseInfo> databaseInformation;

        public Object cacheInfo;

        public static Application createFromCurrentRuntime(int[] uptime, long startTime, GsrsCache gsrsCache, List<DataSource> datasources) throws Exception {
            return createFrom(Runtime.getRuntime(), uptime, startTime, gsrsCache, datasources);
        }
        public static Application createFrom(Runtime rt, int[] uptime, long startTime, GsrsCache gsrsCache, List<DataSource> datasources) throws Exception {
            Application app = new Application();
            app.uptime = new UptimeInfo();

            app.uptime.hours = uptime[0];
            app.uptime.minutes = uptime[1];
            app.uptime.seconds = uptime[2];
            app.epoch = new Date(startTime);

            Set<Thread> threads = Thread.getAllStackTraces().keySet();
            app.threads = threads.size();
            app.runningThreads = threads.stream()
                                        .filter(t-> (t.getState()==Thread.State.RUNNABLE))
                                        .count();
            app.javaVersion = System.getProperty("java.version");

            app.hostname = java.net.InetAddress.getLocalHost().getCanonicalHostName();

            app.runtime = new RuntimeInfo(rt);

            app.databaseInformation = datasources
                                                .stream()
                                                .map(DataBaseInfo::create)
                                                .collect(Collectors.toList());

            app.cacheInfo = gsrsCache ==null? null: gsrsCache.getConfiguration();
            return app;
        }
    }

    /*
    application: {
    epoch: number,
    uptime: Array[number] //some backend processing done in App.java,
could be done frontend by getting current time
    threads: number,
    runningThreads: number,
    javaVersion: string,
    hostName: string,
    runtime: {
        availableProcessors: number,
        freeMemory:number,
        totalMemory:number,
        maxMemory:number,
    },
databaseInformation: Array [ {
    database: string;
    driver: string;
    product: string;
    connected: boolean;
    latency: number;
}]

     */

    public static class CacheInfo{
        public long maxCacheElements;
        public long maxNotEvictableCacheElements;
        public long timeToLive;
        public long timeToIdle;
        /*
         <div class="panel-heading">
       		<h3 class="panel-title">Cache Configuration</h3>
    	</div>
	    <div class="panel-body">
	       <table class="table table-striped">
	          <tr>
	            <td>Max Cache Elements</td>
	        	<td>@Play.application().configuration().getString(IxCache.CACHE_MAX_ELEMENTS)</td>
	          </tr>
	          <tr>
	        <td>Time to Live (seconds)</td>
	        <td>@Play.application().configuration().getInt(IxCache.CACHE_TIME_TO_LIVE)</td>
	          </tr>
	          <tr>
	        <td>Time to Idle (seconds)</td>
	        <td>@Play.application().configuration().getInt(IxCache.CACHE_TIME_TO_IDLE)</td>
	          </tr>
	       </table>
         */
    }
    public static class UptimeInfo{
        public long hours;
        public int minutes;
        public int seconds;



    }

	public static class DataBaseInfo {
		public String database = "Unnamed data source";
		public String driver;
		public String product;
		public Long latency = (long) -1;
		public boolean connected = false;
		public int maxConnectionPool;
		public int activeConnection;

		public DataBaseInfo() {
		}

		public static DataBaseInfo create(DataSource dataSource) {
			DataBaseInfo dbInfo = new DataBaseInfo();
			DatabaseMetaData metadata;

			long start = System.currentTimeMillis();
			
			try(Connection c = dataSource.getConnection()){				
				metadata = c.getMetaData();
				dbInfo.driver = metadata.getDriverName();
				dbInfo.product = metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion();
				long end = System.currentTimeMillis();
				dbInfo.connected = true;
				dbInfo.latency = end - start;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			HikariDataSourcePoolMetadata hikariMetadata = new HikariDataSourcePoolMetadata(
					(HikariDataSource) dataSource);
			dbInfo.maxConnectionPool = hikariMetadata.getMax();
			dbInfo.activeConnection = hikariMetadata.getActive();
			return dbInfo;
		}
//        public static  DataBaseInfo create(DBConfigInfo info){
//            DataBaseInfo dbInfo = new DataBaseInfo();
//            dbInfo.database = info.getName();
//            dbInfo.driver = info.getDriver();
//            dbInfo.product = info.getProduct();
//            dbInfo.latency = info.getLatency();
//            dbInfo.connected = info.getConnected();
//            return dbInfo;
//
//        }
    }
}
