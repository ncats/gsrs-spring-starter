package gsrs;

import pl.joegreen.lambdaFromString.*;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class GsrsUtils {

    private GsrsUtils(){
        //can not instantiate
    }
    /**
     * Create a BiFunction from a lambda String.  This method will compile the lambda String using the given type information
     * and any additional imports that are required for the lambda to compile aside from the default java imports.
     * <br/>
     *
     * Examples:
     * <br/>
     * With just basic arithmetic:
     * <pre>
     * {@code BiFunction<Integer, Integer, Integer> lambda= GsrsUtils.toLambdaBiFunction(getClass().getClassLoader(),
     *                                                       "(x, y) -> x*y +1",
     *                                                       new GsrsUtils.LambdaTypeReference<BiFunction<Integer, Integer, Integer>>(){});
     *
     *         assertEquals(2, lambda.apply(1,1));
     *         assertEquals(3, lambda.apply(1,2));
     *         assertEquals(5, lambda.apply(2,2));
     *         assertEquals(51, lambda.apply(5,10));}
     * </pre>
     * <br/>
     *
     * With an import. Assume we have a class like this:
     * <pre>
     * {@code public class MyComputer{
     *
     *         public static int add(int x, int y){
     *             return x+y;
     *         }
     *     }}
     * </pre>
     * If we use this in our lambda expression we need to add it to our import list like this:
     * <pre>
     * {@code BiFunction<Integer, Integer, Integer> lambda= GsrsUtils.toLambdaBiFunction(getClass().getClassLoader(),
     *                                                                 "(x, y) -> MyComputer.add(x,y)",
     *                                                                 new GsrsUtils.LambdaTypeReference<BiFunction<Integer, Integer, Integer>>(){},
     *                                                                 MyComputer.class);
     *
     *         assertEquals(3, lambda.apply(1, 2));
     *         assertEquals(15, lambda.apply(5, 10));}
     * </pre>
     * @param classLoader the classloader to use that "knows" about all the classes to import.
     * @param lambdaString the actual lambda to compile as a String.
     * @param typeReference a new {@link LambdaTypeReference} with the same generic as the returned
     *                      Function should have.  This new instance is used to figure out the type of the lambda so it has to match.
     *                      Note the empty braces in the examples given.
     * @param imports a list of classes that need to be additionally imported see examples.
     * @param <T> The input parameter type of the lambda.
     * @param <R> the return type of the lambda.
     * @return a new Function object.
     * @throws IOException if there was a problem compiling the lambda string.
     *
     */
    public static <T, U, R> BiFunction<T,U, R> toLambdaBiFunction(ClassLoader classLoader, String lambdaString,
                                                                  LambdaTypeReference<BiFunction<T,U,R>> typeReference,Class<?>... imports) throws IOException {
        LambdaFactoryConfiguration config = LambdaFactoryConfiguration.get()
                .withParentClassLoader(classLoader)
                .withImports(imports)
                ;

        try {
            return LambdaFactory.get(config)
                    .createLambda(lambdaString, typeReference);
        } catch (LambdaCreationException e) {
            throw new IOException("error creating lambda", e);
        }
    }

    /**
     * Create a Function from a lambda String.  This method will compile the lambda String using the given type information
     * and any additional imports that are required for the lambda to compile aside from the default java imports.
     * <br/>
     *
     * Examples:
     * <br/>
     * With just basic arithmetic:
     * <pre>
     * {@code Function<Integer, Integer> lambda= GsrsUtils.toLambdaFunction(getClass().getClassLoader(),
     *                                                         "x -> x+1",
     *                                                         new GsrsUtils.LambdaTypeReference<Function<Integer, Integer>>(){});
     *
     *         assertEquals(2, lambda.apply(1));
     *         assertEquals(3, lambda.apply(2));}
     * </pre>
     * <br/>
     * With Math library (don't have to import)
     * <pre>
     * {@code Function<Integer, Integer> lambda= GsrsUtils.toLambdaFunction(getClass().getClassLoader(),
     *                                                       "x -> Math.abs(x)",
     *                                                       new GsrsUtils.LambdaTypeReference<Function<Integer, Integer>>(){});
     *
     *         assertEquals(1, lambda.apply(1));
     *         assertEquals(2, lambda.apply(-2));}
     * </pre>
     * <br/>
     * With an import. Assume we have a class like this:
     * <pre>
     * {@code public class MyComputer{
     *
     *         public static int increment(int x){
     *             return x+1;
     *         }
     *     }}
     * </pre>
     * If we use this in our lambda expression we need to add it to our import list like this:
     * <pre>
     * {@code Function<Integer, Integer> lambda= GsrsUtils.toLambdaFunction(getClass().getClassLoader(),
     *                                           "x -> MyComputer.increment(x)",
     *                                           new GsrsUtils.LambdaTypeReference<Function<Integer, Integer>>(){},
     *                                           MyComputer.class);
     *
     *         assertEquals(2, lambda.apply(1));
     *         assertEquals(3, lambda.apply(2));}
     * </pre>
     * @param classLoader the classloader to use that "knows" about all the classes to import.
     * @param lambdaString the actual lambda to compile as a String.
     * @param typeReference a new {@link LambdaTypeReference} with the same generic as the returned
     *                      Function should have.  This new instance is used to figure out the type of the lambda so it has to match.
     *                      Note the empty braces in the examples given.
     * @param imports a list of classes that need to be additionally imported see examples.
     * @param <T> The input parameter type of the lambda.
     * @param <R> the return type of the lambda.
     * @return a new Function object.
     * @throws IOException if there was a problem compiling the lambda string.
     *
     */
    public static <T, R> Function<T,R> toLambdaFunction(ClassLoader classLoader, String lambdaString, LambdaTypeReference<Function<T,R>> typeReference,  Class<?>... imports) throws IOException {
        LambdaFactoryConfiguration config = LambdaFactoryConfiguration.get()
                .withParentClassLoader(classLoader)
                .withImports(imports)
                ;

        try {
            return LambdaFactory.get(config)
                    .createLambda(lambdaString, typeReference);
        } catch (LambdaCreationException e) {
            throw new IOException("error creating lambda", e);
        }
    }

    /**
     * Abstract class needed to tell lambda compiler about the generic types we have.
     * @param <T>
     */
    public static abstract class LambdaTypeReference<T> extends TypeReference<T>{


    }
}
