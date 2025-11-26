package com.joshlong.aot.hints.kubernetes.fabric8;

import com.joshlong.aot.hints.HintsUtils;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class Fabric8BeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	@SuppressWarnings("deprecation")
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
				registerMe.addAll(reflections.getSubTypesOf(DefaultKubernetesResourceList.class));
				customResources.forEach(cr -> GenericTypeResolver.getTypeVariableMap(cr).forEach((tv, clazz) -> {
					try {
						var type = Class.forName(clazz.getTypeName());
						if (this.log.isDebugEnabled())
							this.log.debug("the type variable is {} and the class is {}", type.getName(),
									clazz.getTypeName());
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