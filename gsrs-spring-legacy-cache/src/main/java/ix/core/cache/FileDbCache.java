package ix.core.cache;

import com.sleepycat.je.*;
import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.model.AbstractGsrsEntity;
import ix.core.search.SearchResultContext;
import ix.utils.Util;
import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;

import java.io.*;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by katzelda on 7/7/16.
 */
@Slf4j
public class FileDbCache implements GinasFileBasedCacheAdapter {

    private Database db;

    private final File dir;
    private final String cacheName;
    private final boolean clearDb;
    private int serializableCount=0, notSerializableCount=0;
    
    public FileDbCache(File dir, String cacheName, boolean clearDb){
        Objects.requireNonNull(dir);
        Objects.requireNonNull(cacheName);
        this.clearDb= clearDb;
        this.cacheName = cacheName;
        this.dir = dir;
    }

    private volatile boolean init=false;

    @Override
    public Object createEntry(Object key) throws Exception {

        //System.out.println("Finding key:" + key);
        if (!(key instanceof Serializable)) {
            throw new IllegalArgumentException
                    ("Cache key "+key+" is not serliazable!");
        }
        
        Element elm = null;
        try {
            DatabaseEntry dkey = getKeyEntry (key);
            DatabaseEntry data = new DatabaseEntry ();
            OperationStatus status = db.get(null, dkey, data, null);
            if (status == OperationStatus.SUCCESS) {
                try(ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data.getData(), data.getOffset(), data.getSize()))) {
                    elm = new Element(key, ois.readObject());
                }
            }
            else if (status == OperationStatus.NOTFOUND) {
        
            }
            else {
                log.warn("Unknown status for key "+key+": "+status);
            }
        }
        catch (Exception ex) {
            log.error("Can't recreate entry for "+key, ex);
        }
        return elm;
    }
    
    public void init(){
    	init(clearDb);
    }

    public void init(boolean cleardb) {
    	if(init){
            return;
        }
    	//use this instead of dir.mkdirs()
        //because it will throw IOException with reason why dir couldn't be created
        //mkdirs just returns boolean
        try{
            Files.createDirectories(dir.toPath());
        }catch(IOException e){
            throw new RuntimeException("error creating dir", e);
        }

        EnvironmentConfig envconf = new EnvironmentConfig ();
        envconf.setAllowCreate(true);
        Environment env = new Environment (dir, envconf);
        if(cleardb){
	        try{
	        	env.removeDatabase(null, cacheName);
	        }catch(Exception e){
	        	log.warn("No persist cache to delete", e);
	        }
        }
        DatabaseConfig dbconf = new DatabaseConfig ();
        dbconf.setAllowCreate(true);
        db = env.openDatabase(null, cacheName, dbconf);
        init=true;
    }

    @Override
    public void dispose() throws CacheException {
        if (db != null) {
            try {
                log.debug("#### closing cache writer "+cacheName
                        +"; "+db.count()+" entries #####");
                db.close();
                db =null;
            }
            catch (Exception ex) {
                log.error("Can't close lucene index!", ex);
            }
        }
    }

    
    static DatabaseEntry getKeyEntry (Object value) {
        return new DatabaseEntry (value.toString().getBytes());
    }
    
    public static Optional<Tuple<Serializable,Object>> getSerializableObect(Element elm){
    	
    	Object value =elm.getObjectValue();
    	Object key = elm.getObjectKey();
    	if(!(key instanceof Serializable)){
    		return Optional.empty();
    	}
    	
    	if(value instanceof CachedSupplier){
    		//Warning: forces a real, synchronized call
    		@SuppressWarnings("unchecked")
			Object realValue=((CachedSupplier<Object>)value).getSync();
    		
    		return getKeyValueSerialized((Serializable) key,realValue);
    	}else{
    		return getKeyValueSerialized((Serializable) key,value);
    	}
    	
    }
    
    private static Optional<Tuple<Serializable,Object>> 
    					getKeyValueSerialized(Serializable key, Object realValue){
    	if(realValue instanceof SearchResultContext){
    		
    		SearchResultContext.SerailizedSearchResultContext 
    			serializedContext=
    			((SearchResultContext)realValue).getSerializedForm();
    		
    		return Optional.of(Tuple.of(serializedContext.getSerializedKey(),
    									serializedContext
    									));
    	}
    	
    	if(!(realValue instanceof Serializable)){
    		return Optional.empty();
		}else if(realValue instanceof AbstractGsrsEntity){
			return Optional.empty();
		}
    	return Optional.of(Tuple.of(key, realValue));
    	
    }
    
    @Override
    public void write(Element elm) throws CacheException {
    	Optional<Tuple<Serializable,Object>> seralizable=getSerializableObect(elm);
    	
        if(!seralizable.isPresent()){
            notSerializableCount++;
            return;
        }
        //System.out.println("Writing key:" + elm.getObjectKey());
        serializableCount++;
        //TODO is this a safe cast?


        Serializable key = seralizable.get().k();
        Object value = seralizable.get().v();
        
        if (key != null) {
            //log.debug("Persisting cache key="+key+" value="+elm.getObjectValue());
            try {
                DatabaseEntry dkey = getKeyEntry (key);
                DatabaseEntry data = new DatabaseEntry
                        (Util.serialize(value));
                OperationStatus status = db.put(null, dkey, data);
                if (status != OperationStatus.SUCCESS) {
                    log.warn
                            ("** PUT for key " + key + " returns status " + status);
                }
            }catch (Exception ex) {
                ex.printStackTrace();
                log.error("Can't write cache element: key="
                        +key+" value="+elm.getObjectValue(), ex);
            }
        }else {
            log.warn("Key "+elm.getObjectKey()+" isn't serializable!");
        }
    }

    @Override
    public void writeAll(Collection<Element> elements) throws CacheException {
        //TODO is there a better way?
        for(Element e : elements){
            write(e);
        }
    }

    @Override
    public void delete(CacheEntry entry) throws CacheException {
            Object key = entry.getKey();
            if (!(key instanceof Serializable))
                return;

            try {
                DatabaseEntry dkey = getKeyEntry (key);
                OperationStatus status = db.delete(null, dkey);
                if (status != OperationStatus.SUCCESS)
                    log.warn("Delete cache key '"
                            +key+"' returns status "+status);
            }
            catch (Exception ex) {
                log.error("Deleting cache "+key+" from persistence!", ex);
            }
    }

    @Override
    public void deleteAll(Collection<CacheEntry> entries) throws CacheException {
        for(CacheEntry e : entries){
            delete(e);
        }
    }

    @Override
    public void throwAway(Element elm, SingleOperationType operationType, RuntimeException ex) {
        log.error("Throwing away cache element "+elm.getObjectKey(), ex);
    }
}
