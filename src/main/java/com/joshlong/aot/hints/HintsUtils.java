package com.joshlong.aot.hints;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 *
 * Utilities for working with classes in a consistent way
 *
 * @author Josh Long
 */
public abstract class HintsUtils {

	public static List<? extends Class<?>> findAllClasses(String basePackage) {
		var scanner = new ClassPathScanningCandidateComponentProvider(false) {
			@Override
			protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
				return true;
			}
		};
		scanner.addIncludeFilter((y, x) -> true);
		return scanner //
			.findCandidateComponents(basePackage) //
			.stream() //
			.map(bd -> {
				try {
					return Class.forName(bd.getBeanClassName());
				} //
				catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			})
			.toList();

	}

	public static boolean isClassPresent(String className) {
		return ClassUtils.isPresent(className, HintsUtils.class.getClassLoader());
	}

	public static Class<?> classForName(String clazzName) {
		try {
			return Class.forName(clazzName);
		} //
		catch (Exception e) {
			return null;
		}

	}

}
