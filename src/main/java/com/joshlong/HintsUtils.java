package com.joshlong;

import org.springframework.util.ClassUtils;

/**
 *
 * Utilities for working with classes in a consistent way
 *
 * @author Josh Long
 */
public abstract class HintsUtils {

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
