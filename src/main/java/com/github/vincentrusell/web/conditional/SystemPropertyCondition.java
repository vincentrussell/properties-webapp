package com.github.vincentrusell.web.conditional;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;

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

		List<String> absentProperties = Lists.newArrayList(firstNonNull((String[]) metadata
				.getAnnotationAttributes(ConditionalOnSystemProperty.class.getName())
				.get("absentProperties"), new String[0]));

		List<String> propertyKeyMatches = Lists.newArrayList(Iterables.transform(
				Iterables.filter(
						System.getProperties().keySet(), o -> absentProperties.contains(o)), o -> o.toString()));

		if (absentProperties.size() > 0 && propertyKeyMatches.size() == 0) {
			return true;
		} else if (value !=null && name != null && value.equals(System.getProperty(name))) {
			return true;
		} else if (name != null && System.getProperty(name) != null) {
			return true;
		}
		return false;
	}
}
