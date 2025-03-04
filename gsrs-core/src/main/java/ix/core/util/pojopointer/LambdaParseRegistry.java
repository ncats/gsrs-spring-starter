package ix.core.util.pojopointer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.RegisteredFunctionProperties;
import gsrs.springUtils.AutowireHelper;

import gsrs.util.RegisteredFunctionConfig;
import ix.core.util.pojopointer.extensions.RegisteredFunction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

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

	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	// This should only be populated if the CachedSupplier lambda is executed.
	public List<? extends RegisteredFunctionConfig> _configs = null;

	public List<? extends RegisteredFunctionConfig> reportConfigs() {
		return _configs;
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

			if(registeredFunctionProperties != null && registeredFunctionProperties.getRegisteredFunctions().getList() != null) {

				List<? extends RegisteredFunctionConfig> configs = loadRegisteredFunctionsFromConfiguration();
				// Point to new value of configs for reporting
                _configs = configs;

				for (RegisteredFunctionConfig config : configs) {
					try {
						RegisteredFunction rf = AutowireHelper.getInstance().autowireAndProxy(
							(RegisteredFunction) config.getRegisteredFunctionClass().getDeclaredConstructor().newInstance()
						);
						LambdaArgumentParser p = rf.getFunctionURIParser();
						System.out.println("Found special Function:" + p.getKey());
						map.put(p.getKey(), p);
						registeredFunctions.add(rf);
					} catch (Exception e) {
						e.printStackTrace();
					}
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

	private List<? extends RegisteredFunctionConfig>  loadRegisteredFunctionsFromConfiguration() {
		String reportTag = "RegisteredFunctionConfig";
		ObjectMapper mapper = new ObjectMapper();
		try {
			Map<String, Map<String, Object>> map = registeredFunctionProperties.getRegisteredFunctions().getList();
			if (map == null || map.isEmpty()) {
				return Collections.emptyList();
			}
			for (String k: map.keySet()) {
				map.get(k).put("parentKey", k);
			}
			List<Object> list = map.values().stream().collect(Collectors.toList());
			List<? extends RegisteredFunctionConfig> configs = mapper.convertValue(list, new TypeReference<List<? extends RegisteredFunctionConfig>>() { });
			System.out.println( reportTag + "found before filtering: " + configs.size());
			configs = configs.stream().filter(c->!c.isDisabled()).sorted(Comparator.comparing(c->c.getOrder(),nullsFirst(naturalOrder()))).collect(Collectors.toList());
			System.out.println(reportTag + " active after filtering: " + configs.size());
			System.out.printf("%s|%s|%s|%s|%s\n", "reportTag", "class", "parentKey", "order", "isDisabled");
			for (RegisteredFunctionConfig config : configs) {
				System.out.printf("%s|%s|%s|%s|%s\n", reportTag, config.getRegisteredFunctionClass(), config.getParentKey(), config.getOrder(), config.isDisabled());
			}
			return configs;
		} catch (Throwable t) {
			throw t;
		}
	}

	public Optional<Function<String,? extends PojoPointer>> getPojoPointerParser(final String key) throws NoSuchElementException {
		Function<String,? extends PojoPointer> parser= subURIparsers.get().get(key);

			return Optional.ofNullable(parser);

	}


}
