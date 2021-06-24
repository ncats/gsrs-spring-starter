package gsrs.coretests;

import gsrs.GsrsUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

public class ToLambdaTest {

    @Test
    public void SimpleLambdaFunctionNoImports() throws IOException {
        Function<Integer, Integer> lambda= GsrsUtils.toLambdaFunction(getClass().getClassLoader(), "x -> x+1",
                new GsrsUtils.LambdaTypeReference<Function<Integer, Integer>>(){});

        assertEquals(2, lambda.apply(1));
        assertEquals(3, lambda.apply(2));
    }

    @Test
    public void SimpleLambdaBiFunctionNoImports() throws IOException {
        BiFunction<Integer, Integer, Integer> lambda= GsrsUtils.toLambdaBiFunction(getClass().getClassLoader(), "(x, y) -> x*y +1",
                new GsrsUtils.LambdaTypeReference<BiFunction<Integer, Integer, Integer>>(){});

        assertEquals(2, lambda.apply(1,1));
        assertEquals(3, lambda.apply(1,2));
        assertEquals(5, lambda.apply(2,2));
        assertEquals(51, lambda.apply(5,10));
    }

    @Test
    public void SimpleLambdaFunctionMathImport() throws IOException {
        Function<Integer, Integer> lambda= GsrsUtils.toLambdaFunction(getClass().getClassLoader(), "x -> Math.abs(x)",
                new GsrsUtils.LambdaTypeReference<Function<Integer, Integer>>(){});

        assertEquals(1, lambda.apply(1));
        assertEquals(2, lambda.apply(-2));
    }

    @Test
    public void LambdaFunctionWithImport() throws IOException {
        Function<Integer, Integer> lambda= GsrsUtils.toLambdaFunction(getClass().getClassLoader(), "x -> MyComputer.increment(x)",
                new GsrsUtils.LambdaTypeReference<Function<Integer, Integer>>(){},
                MyComputer.class);

        assertEquals(2, lambda.apply(1));
        assertEquals(3, lambda.apply(2));
    }

    @Test
    public void LambdaBiFunctionWithImport() throws IOException {
        BiFunction<Integer, Integer, Integer> lambda= GsrsUtils.toLambdaBiFunction(getClass().getClassLoader(), "(x, y) -> MyComputer.add(x,y)",
                new GsrsUtils.LambdaTypeReference<BiFunction<Integer, Integer, Integer>>(){},
                MyComputer.class);

        assertEquals(3, lambda.apply(1, 2));
        assertEquals(15, lambda.apply(5, 10));
    }

    public static class MyComputer{

        public static int increment(int x){
            return x+1;
        }
        public static int add(int x, int y){
            return x+y;
        }
    }
}
