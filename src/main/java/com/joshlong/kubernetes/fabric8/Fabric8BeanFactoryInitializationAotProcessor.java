package com.joshlong.kubernetes.fabric8;

import com.joshlong.HintsUtils;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.ReflectionUtils;

import java.util.HashSet;

/**
 * Registers Fabric8 types in Spring Boot userspace packages, as well.
 *
 * @author Josh Long
 */
@Slf4j
public class Fabric8BeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {

		if (!HintsUtils.isClassPresent("io.fabric8.kubernetes.client.CustomResource"))
			return null;

		return (generationContext, beanFactoryInitializationCode) -> {
			var hints = generationContext.getRuntimeHints();
			var registerMe = new HashSet<Class<?>>();
			var strings = AutoConfigurationPackages.get(beanFactory);
			for (var pkg : strings) {
				var reflections = new Reflections(pkg);
				var customResources = reflections.getSubTypesOf(CustomResource.class);
				registerMe.addAll(customResources);
				registerMe.addAll(reflections.getSubTypesOf(CustomResourceList.class));
				customResources.forEach(cr -> GenericTypeResolver.getTypeVariableMap(cr).forEach((tv, clazz) -> {
					try {
						var type = Class.forName(clazz.getTypeName());
						if (log.isDebugEnabled())
							log.debug("the type variable is " + type.getName() + " and the class is "
									+ clazz.getTypeName());
						registerMe.add(type);
					} //
					catch (ClassNotFoundException e) {
						ReflectionUtils.rethrowRuntimeException(e);
					}
				}));
			}
			registerMe.forEach(c -> hints.reflection().registerType(c, MemberCategory.values()));

		};
	}

}