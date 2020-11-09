package ix.core.util.pojopointer;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.RegisteredFunctionProperties;
import gsrs.springUtils.AutowireHelper;

import ix.core.util.pojopointer.extensions.RegisteredFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.*;
import java.util.function.Function;

@Component
public class LambdaParseRegistry implements ApplicationListener<ContextRefreshedEvent> {

	private CachedSupplier<Map<String, Function<String,? extends PojoPointer>>> subURIparsers;
	@Autowired
	private RegisteredFunctionProperties registeredFunctionProperties;

	private List<RegisteredFunction> registeredFunctions = new ArrayList<>();
	private static LambdaParseRegistry instance;


	public static LambdaParseRegistry getInstance(){
		return instance;
	}

	public List<RegisteredFunction> getRegisteredFunctions() {
		return registeredFunctions;
	}

	public static void setInstance(LambdaParseRegistry registry){
		instance = registry;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {

		subURIparsers = CachedSupplier.of(() -> {
			final Map<String, Function<String, ? extends PojoPointer>> map = new HashMap<>();


			//Needs an argument, definitely
			map.put("map", FieldBasedLambdaArgumentParser.of("map", (p) -> new MapPath(p)));

			//Can use an argument, definitely
			map.put("sort", FieldBasedLambdaArgumentParser.of("sort", (p) -> new SortPath(p, false)));
			map.put("revsort", FieldBasedLambdaArgumentParser.of("revsort", (p) -> new SortPath(p, true)));
			map.put("flatmap", FieldBasedLambdaArgumentParser.of("flatmap", (p) -> new FlatMapPath(p)));


			map.put("distinct", FieldBasedLambdaArgumentParser.of("distinct", (p) -> new DistinctPath(p)));

			//Probably doesn't need an argument
			map.put("count", FieldBasedLambdaArgumentParser.of("count", (p) -> new CountPath(p)));


			//Not for collections
			map.put("$fields", FieldBasedLambdaArgumentParser.of("$fields", (p) -> new FieldPath(p)));


			map.put("group", FieldBasedLambdaArgumentParser.of("group", (p) -> new GroupPath(p)));

			map.put("limit", LongBasedLambdaArgumentParser.of("limit", (p) -> new LimitPath(p)));
			map.put("skip", LongBasedLambdaArgumentParser.of("skip", (p) -> new SkipPath(p)));

			for(Map<String, Object> m : registeredFunctionProperties.getRegisteredfunctions()){
				try{
					String className = (String) m.get("class");
					Class<?> c = ClassUtils.forName(className, null);
					RegisteredFunction rf = (RegisteredFunction) c.getDeclaredConstructor().newInstance();
					AutowireHelper.getInstance().autowire(rf);

					LambdaArgumentParser p = rf.getFunctionURIParser();
					System.out.println("Found special Function:" + p.getKey());
					map.put(p.getKey(), p);
					registeredFunctions.add(rf);
				}catch(Exception e){
					e.printStackTrace();
				}
			}


//			try {
//				functionFactory
//						.getRegisteredFunctions()
//						.stream().forEach(rf -> {
//					System.out.println("Found special Function:" + rf.getFunctionURIParser().getKey());
//					LambdaArgumentParser<?> lap = rf.getFunctionURIParser();
//					map.put(lap.getKey(), lap);
//				});
//			} catch (Exception e) {
//				//there's no started application
//			}

			return map;
		});

		instance = this;
	}

	public Optional<Function<String,? extends PojoPointer>> getPojoPointerParser(final String key) throws NoSuchElementException {
		Function<String,? extends PojoPointer> parser= subURIparsers.get().get(key);

			return Optional.ofNullable(parser);

	}


}