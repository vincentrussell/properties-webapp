package com.github.vincentrusell.web.conditional;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(SystemPropertyCondition.class)
public @interface ConditionalOnSystemProperty {

	String value() default "";

	String name() default "";

	String[] absentProperties() default {};

}
