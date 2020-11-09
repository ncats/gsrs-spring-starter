package ix.core.search;


import akka.actor.AbstractActor;
import akka.actor.UntypedActor;
import gov.nih.ncats.common.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class SearchResultHandler extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ResultProcessor.class, this::handleResultProcessor )
                .build();
    }

    private void handleResultProcessor(ResultProcessor processor) {
        SearchResultContext ctx = processor.getContext();
        try {
            ctx.setStatus(SearchResultContext.Status.Running);
            ctx.setStart(TimeUtil.getCurrentTimeMillis());

            int count = processor.process();
            if(count==0){
                ctx.setStatus(SearchResultContext.Status.Done);
            }else{
                ctx.setStatus(SearchResultContext.Status.Determined);
            }

            ctx.setStop(TimeUtil.getCurrentTimeMillis());
//                log.debug("Actor "+self()+" finished; "+count
//                             +" search result(s) instrumented!");

        }catch (Exception ex) {
            ctx.setStatus(SearchResultContext.Status.Failed);
            ctx.setMessage(ex.getMessage());
            ex.printStackTrace();
            log.error("Unable to process search results", ex);
        }
    }
}