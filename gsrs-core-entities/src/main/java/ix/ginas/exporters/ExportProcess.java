package ix.ginas.exporters;


import gov.nih.ncats.common.io.IOUtil;
import gov.nih.ncats.common.util.TimeUtil;
import gov.nih.ncats.common.util.Unchecked;
import ix.utils.Util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 *
 * @param <T> the type being exported.
 *
 * Created by katzelda on 4/18/17.
 */
public class ExportProcess<T> {

    private ExportDir.ExportFile<ExportMetaData> exportFile;



    private State currentState = State.INITIALIZED;
    
    
    
    private Exporter<T> exporter;
    private final Supplier<Stream<T>> entitySupplier;

    public ExportMetaData getMetaData() {
        try {
            return exportFile.getMetaData().get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    /**
     * Create a new ExportProcess.
     * @param exportFile the ExportFile to update.
     *
     * @param entitySupplier a {@link Supplier} for the {@link Stream} of objects of type T to process for export.
     *
     * @throws NullPointerException if either parameter is null.
     */
    public  ExportProcess(ExportDir.ExportFile<ExportMetaData> exportFile, Supplier<Stream<T>> entitySupplier){
        this.exportFile= Objects.requireNonNull(exportFile);

        this.entitySupplier = Objects.requireNonNull(entitySupplier);
    }


    public synchronized void run(Executor executor, Function<OutputStream, Exporter<T>> exporterFunction) throws IOException{

        if(currentState != State.INITIALIZED){
            return;
        }
        if(exporterFunction ==null){
            throw new NullPointerException("exporter function can not be null");
        }
        
        long writeOutEvery = 10;
        long writeOutEveryTime = 5_000;
        
        
        SimpleTimer timer = new SimpleTimer().setDuration(writeOutEveryTime).mark();        
        
        
        
        OutputStream out=null;
        try{
            out = createOutputFileStream(); // throws IOException
            exporter = exporterFunction.apply(out);
            ExportMetaData metaData = exportFile.getMetaData().orElse(new ExportMetaData());
            currentState = State.PREPARING;
            metaData.started = TimeUtil.getCurrentTimeMillis();

            IOUtil.closeQuietly(() ->  exportFile.saveMetaData(metaData));
            //make another final reference to outputstream
            //so we can reference it in the lambda for submit
            //final OutputStream fout = out;
            
            executor.execute( ()->{
                try(Stream<T> sstream = entitySupplier.get()){
                    currentState = State.RUNNING;
//                    System.out.println("Starting export");
                    sstream.peek(s -> {
                        Unchecked.uncheck( () ->{
                            try {
                            exporter.export(s);
                            
                        }catch(Throwable t) {
                            t.printStackTrace();
                            throw t;
                        }});
                        metaData.addRecord();
                        if(timer.isReady() && (metaData.getNumRecords() % writeOutEvery ==0)) {
                            timer.mark();
                            IOUtil.closeQuietly(() ->  exportFile.saveMetaData(metaData));
                        }
//                        metaData.
                        
                    })
                    .anyMatch(m->{
                        //katzelda March 2021: if we cancel the executor it should
                        //set the interrupt flag so check that too #cancelled field only set from API call
                        if(Thread.currentThread().isInterrupted()){
                            metaData.cancel();
                        }
                     return metaData.cancelled;
                    });
                    
                    currentState = State.DONE;
                }catch(Throwable t){
                    t.printStackTrace();
                    currentState = State.ERRORED_OUT;
                    throw t;
                }finally{
                    //close the exporter first this should flush out all data to the file
                    IOUtil.closeQuietly(exporter);
                    try {
                    	File f=exportFile.getFile();
                    	
                        metaData.sha1= Util.sha1(f);
                        metaData.size=f.length();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    metaData.finished=TimeUtil.getCurrentTimeMillis();
                    
                    IOUtil.closeQuietly(() ->  exportFile.saveMetaData(metaData));

                    
                    
                    //IxCache.remove(metaData.getKey());
                }
            });
            

        }catch(Throwable t){
            IOUtil.closeQuietly(out);
            currentState = State.ERRORED_OUT;

            throw t;
        }
    }

    private void writeMetaDataFile() throws IOException{
//        try(BufferedWriter writer = new BufferedWriter(new FileWriter(metaDataFile))){
//            EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer().writeValue(writer, metaData);
//        }
//        exportFile.saveMetaData(metaData);
    }
    private OutputStream createOutputFileStream() throws IOException{
        return IOUtil.newBufferedOutputStream(exportFile.getFile());
    }




    public enum State{
        INITIALIZED,
        RUNNING,
        DONE,
        ERRORED_OUT,
        /**
         * Gathering data required to begin export process.
         */
        PREPARING;
    }
    

    public static class SimpleTimer{
        private long lastTime;
        private long duration;
        public SimpleTimer setDuration(long delta) {
            this.duration=delta;
            return this;
        }
        public SimpleTimer mark() {
            return this.mark(TimeUtil.getCurrentTimeMillis());
        }
        public SimpleTimer mark(long stamp) {
            this.lastTime=stamp;
            return this;
        }
        
        public boolean isReady(long ts) {
            if(ts-this.lastTime>this.duration) {
                return true;
            }
            return false;
        }
        
        public boolean isReady() {
            return isReady(TimeUtil.getCurrentTimeMillis());
        }
        
    }
}
