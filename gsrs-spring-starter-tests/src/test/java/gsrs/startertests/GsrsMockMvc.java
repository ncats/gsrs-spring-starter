//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.boot.test.autoconfigure.web.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;
import org.springframework.boot.test.autoconfigure.properties.SkipPropertyMapping;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@PropertyMapping("spring.test.mockmvc")
public @interface GsrsMockMvc {
    boolean addFilters() default true;

    @PropertyMapping(
            skip = SkipPropertyMapping.ON_DEFAULT_VALUE
    )
    MockMvcPrint print() default MockMvcPrint.DEFAULT;

    boolean printOnlyOnFailure() default true;

    @PropertyMapping("webclient.enabled")
    boolean webClientEnabled() default true;

    @PropertyMapping("webdriver.enabled")
    boolean webDriverEnabled() default true;
}
