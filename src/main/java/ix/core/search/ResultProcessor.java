package ix.core.search;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.common.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.stream.Stream;

//katzelda in GSRS 3 we made this an abstract class instead of an interface so we could DI the Akka System.

public abstract class ResultProcessor<T, R> implements ResultMapper<T,R> {

	@Autowired
	private ActorSystem system;

	public boolean isDone(){
		return getContext().isDetermined(); //kinda weird way to do this
	}
	
	/**
	 * Should create a context if none is present,
	 * or return the one already created
	 * @return
	 */
	public abstract SearchResultContext getContext();
	

	public int process() throws Exception{
	        return process (0);
	}
	
	
	public void setResults(Enumeration<T> results){
		setResults(StreamUtil.forEnumeration(results));
	}
	public void setResults(Stream<T> results){
		setUnadaptedResults(results.iterator());
	}
	public abstract void setUnadaptedResults(Iterator<T> results);
	
	public abstract Iterator<T> getUnadaptedResults();
	
	public int process(int max) throws Exception {
        while (getUnadaptedResults().hasNext()
               && !isDone ()
               && (max <= 0 || getContext().getCount() < max)) {
            T r = getUnadaptedResults().next();
            // This will simulate a slow structure processing (e.g. slow database fetch)
            // This should be used in conjunction with another debugSpin in TextIndexer
            // to simulate both slow fetches and slow lucene processing
            //System.out.println("Processing:" + r);

//            Util.debugSpin(ConfigHelper.getLong("ix.settings.debug.processordelay", 0));

            try {
                this.map(r).forEach(obj->{
                	getContext().add(obj);
                });
            }catch (Exception ex) {
                ex.printStackTrace();
//                log.error("Can't process structure search result", ex);
            }
        }
        return getContext().getCount();
    }

	public void run(int rows) throws Exception{
	    SearchResultContext context=getContext();
	    if(context.getStart()==null){
	        context.setStart(TimeUtil.getCurrentTimeMillis());
	    }
        // the idea is to generate enough results for 1 page, and 1 extra record
        // (enough to show pagination) and return immediately. as the user pages,
        // the background job will fill in the rest of the results.
        int count = process (rows+1);

        // while we continue to fetch the rest of the results in the
        // background
        ActorRef handler = system.actorOf(Props.create(SearchResultHandler.class));
        handler.tell(this, ActorRef.noSender());
//        log.debug("## search results submitted: "+handler);
	}


	/*
	 *
	 * Composition wise, there are a few tasks:
	 *
	 * 1. mapper from given result to new result (should be 1 to 1? -- no!)
	 * 2. consumer, which takes the result set to be passed to the mapper
	 * 3. accumulator, which takes the results from each mapped thing,
	 *    and puts it somewhere
	 * 5. supplier, which gives back the accumulated form
	 * 4. processor, which runs each piece accepted through the
	 *    mapper, and accumulates the results
	 *
	 */


	public void setResults(int rows, Enumeration<T> results) throws Exception {
		setResults(results);
		run(rows);
    }
	
	public Future<Void> getCompletedFuture(){
	    return this.getContext().getDeterminedFuture();
	}
}