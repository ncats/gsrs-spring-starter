package ix.core.cache;

import ix.utils.CallableUtil.TypedCallable;
import ix.utils.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(Parameterized.class)
public class TwoCacheGateKeeperTest /*extends AbstractGinasTest*/ {
	@Rule
	public TemporaryFolder tmpDir = new TemporaryFolder();



	@Parameterized.Parameters(name = "{0}")
	public static List<Object[]> params(){

		List<Object[]> list = new ArrayList<>();

		
		for(TypedCallableMaker type: TypedCallableMaker.values()){
			list.add(new Object[]{type + " raw",type, true});
			list.add(new Object[]{type + " adapted",type, false});
		}
		
		return list;
	}
	
	public static enum TypedCallableMaker{
		DIRECT_OBJECT,
		STATIC_WRAPPED,
		SIMPLE_LAMBDA,
		EXPLICIT_TYPED;
		public <T> TypedCallable<T> makeTypedCallable(T t){
			switch(this){
				case EXPLICIT_TYPED:
					return TypedCallable.of(()->t, (Class<T>)t.getClass());
				case SIMPLE_LAMBDA:
					return ()->t;
				case STATIC_WRAPPED:
					return TypedCallable.of(t);
				default:
					throw new IllegalArgumentException("Unsupported Type:" + this);
			}
		}
		
	}
	
	private TypedCallableMaker typedCallableMaker;

	private boolean useRaw;


	public TwoCacheGateKeeperTest(String justTheName, TypedCallableMaker typedCallableMaker, boolean useRaw){
		this.typedCallableMaker=typedCallableMaker;
		this.useRaw=useRaw;
	}


	int debugLevel = 0;
	int maxElements = 1;
	int notEvictableMaxElements = 1;
	int timeToLive = 1000 * 60 * 60;
	int timeToIdle = 1000 * 60 * 60;
	GateKeeper gateKeeper;

	@Before
	public void setup() throws IOException {
		gateKeeper = new GateKeeperFactory.Builder(maxElements, timeToLive, timeToIdle)
				.debugLevel(debugLevel)
				.useNonEvictableCache(notEvictableMaxElements, timeToLive, timeToIdle)
				.keyMaster(new KeyMaster() {
					private ConcurrentHashMap<String,Set<String>> thekeys= new ConcurrentHashMap<String,Set<String>>();
			        private int size=0;
			        public Set<String> getAllAdaptedKeys(String baseKey){
			            return thekeys.get(baseKey);
			        }

			        @Override
			        public void addKey(String baseKey, String adaptKey) {
			            if(thekeys.computeIfAbsent(baseKey, k-> new HashSet<>()).add(adaptKey)){
			                size++;
			            }
			        }

			        @Override
			        public void removeKey(String baseKey, String adaptKey) {
			            Set<String> keylist=thekeys.get(baseKey);
			            if(keylist!=null){
			                if(keylist.remove(adaptKey)){
			                    size--;
			                }
			            }
			        }

			        @Override
			        public void removeAll() {
			            thekeys.clear();
			        }
					public String adaptKey(String baseKey) {
						final String user = "Rando";
						return "!" + baseKey + "#" + Util.sha1(user);
					}

					public String unAdaptKey(String adaptedKey) {
						if (!adaptedKey.startsWith("!")) {
							return adaptedKey;
						}
						return adaptedKey.substring(1, adaptedKey.lastIndexOf('#'));
					}

				})
				.build()
				.create();
	}

	@After
	public void shutDown(){
		gateKeeper.close();
	}
	@Test
	public void addingNonEvictableThingsWillEventuallyEvictNonEvictableThings() throws Exception {
		NonEvictable ne = new NonEvictable();
		putIntoCache("TEST",ne);
		for (int n = 0; n < notEvictableMaxElements*10; n++) {
			putIntoCache("TEST" + n,new NonEvictable());
		}
		assertNull(getFromCache("TEST"));
	}

	@Test
	public void addingEvictableThingsWillNotEventuallyEvictNonEvictableThings() throws Exception {
		NonEvictable ne = new NonEvictable();
		putIntoCache("TEST",ne);
		for (int n = 0; n < maxElements*10; n++) {
			putIntoCache("TEST" + n, new Evictable());
		}
		assertEquals(ne,getFromCache("TEST"));
	}

	@Test
	public void addingEvictableThingsAndImmediatelyFetchingShouldReturnEvictableThing() throws Exception {
		Evictable e = new Evictable();
		putIntoCache("TEST",e);
		assertEquals(e,getFromCache("TEST"));
	}

	@Test
	public void addingNonEvictableThingsAndImmediatelyFetchingShouldReturnNonEvictableThing() throws Exception {
		NonEvictable ne = new NonEvictable();
		putIntoCache("TEST",ne);
		assertEquals(ne,getFromCache("TEST"));
	}

	@Test
	public void addingNonEvictableThingsWillNotEventuallyEvictEvictableThings() throws Exception {
		Evictable e = new Evictable();
		putIntoCache("TEST",e);
		for (int n = 0; n < notEvictableMaxElements*10; n++) {
			putIntoCache("TEST" + n, new NonEvictable());
		}
		assertEquals(e,getFromCache("TEST"));
	}

	@Test
	public void addingEvictableThingsWillEventuallyEvictEvictableThings() throws Exception {
		Evictable e = new Evictable();
		putIntoCache("TEST",e);
		for (int n = 0; n < maxElements*10; n++) {
			putIntoCache("TEST" + n, new Evictable());
		}
		assertNull(getFromCache("TEST"));
	}
	@Test
	public void addingPureObjectWillEventuallyEvictEvictableThings() throws Exception {
		Evictable e = new Evictable();
		putIntoCache("TEST",e);
		for (int n = 0; n < maxElements*10; n++) {
			putIntoCache("TEST" + n, new Object());
		}
		assertNull(getFromCache("TEST"));
	}
	

	public <T> void putIntoCache(String key, T t) throws Exception{
		if(typedCallableMaker== TypedCallableMaker.DIRECT_OBJECT){
			if(useRaw){
				gateKeeper.putRaw(key, t);
			}else{
				gateKeeper.put(key,t);
			}
		}else{
			if(useRaw){
				gateKeeper.getOrElseRaw(key, typedCallableMaker.makeTypedCallable(t));
			}else{
				gateKeeper.getOrElse(key, typedCallableMaker.makeTypedCallable(t));
			}
		}
	}

	public Object getFromCache(String key) throws Exception{
		if(typedCallableMaker!= TypedCallableMaker.DIRECT_OBJECT){
			if(useRaw){
				return gateKeeper.getOrElseRaw(key, ()->null);
			}else{
				return gateKeeper.getOrElse(key, ()->null);
			}
		}else{
			if(useRaw){
				return gateKeeper.getRaw(key);
			}else{
				return gateKeeper.get(key);
			}

		}
	}

	@CacheStrategy(evictable = false)
	public static class NonEvictable {}
	@CacheStrategy(evictable = true)
	public static class Evictable {}
}
