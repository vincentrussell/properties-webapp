package com.github.vincentrusell.web.conditional;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Order(Ordered.LOWEST_PRECEDENCE)
class SystemPropertyCondition implements Condition {


	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String name = (String) metadata
				.getAnnotationAttributes(ConditionalOnSystemProperty.class.getName())
				.get("name");

		String value = (String) metadata
				.getAnnotationAttributes(ConditionalOnSystemProperty.class.getName())
				.get("value");


		if (value !=null && name != null && value.equals(System.getProperty(name))) {
			return true;
		} else if (name != null && System.getProperty(name) != null) {
			return true;
		}
		return false;
	}
}
