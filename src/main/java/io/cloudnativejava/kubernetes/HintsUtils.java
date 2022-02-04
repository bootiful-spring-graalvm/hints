package io.cloudnativejava.kubernetes;

import org.springframework.util.ClassUtils;

public abstract class HintsUtils {

	public static boolean isClassPresent(String className) {
		return ClassUtils.isPresent(className, HintsUtils.class.getClassLoader());
	}

}
