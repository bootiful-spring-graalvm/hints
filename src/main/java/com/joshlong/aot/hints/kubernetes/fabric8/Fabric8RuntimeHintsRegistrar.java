package com.joshlong.aot.hints.kubernetes.fabric8;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.joshlong.aot.hints.HintsUtils;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.extension.ExtensionAdapter;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring Boot 3 AOT support for the <a href="https://fabric8.io/">Fabric8 Kubernetes
 * client</a>.
 * <p>
 * Alternatively, if you want to use <a href="https://github.com/kubernetes-client/java">
 * the official Kubernetes Java client</a>, there's support in that project directly.
 *
 * @author Josh Long
 */
public class Fabric8RuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Reflections reflections = new Reflections("io.fabric8");

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		if (!HintsUtils.isClassPresent("io.fabric8.kubernetes.api.model.KubernetesResource"))
			return;

		if (log.isDebugEnabled())
			log.debug("running {}", Fabric8RuntimeHintsRegistrar.class.getName());

		var impls = reflections//
			.getAll(Scanners.SubTypes)//
			.stream()//
			.filter(cname -> cname.endsWith("Impl")) //
			.map((Function<String, Class<?>>) this::forName)//
			.collect(Collectors.toSet());
		var subtypesOfKubernetesResource = reflections.getSubTypesOf(KubernetesResource.class);
		var othersToAddForReflection = List.of(KubernetesDeserializer.class);
		var clients = this.reflections.getSubTypesOf(Client.class);
		var combined = new HashSet<Class<?>>();
		combined.addAll(subtypesOfKubernetesResource);
		combined.addAll(othersToAddForReflection);
		combined.addAll(registerExtensionAdapters());
		combined.addAll(impls);
		combined.addAll(clients);
		combined.addAll(this.resolveSerializationClasses(JsonSerialize.class));
		combined.addAll(this.resolveSerializationClasses(JsonDeserialize.class));
		combined.stream().filter(Objects::nonNull).forEach(c -> {
			if (log.isDebugEnabled()) {
				log.debug("trying to register " + c.getName() + " for reflection");
			}
			hints.reflection().registerType(c, MemberCategory.values());
		});
	}

	private Class<?> forName(String name) {
		try {
			return Class.forName(name);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private Set<Class<?>> registerExtensionAdapters() {
		var subTypesOf = this.reflections.getSubTypesOf(ExtensionAdapter.class);
		var classes = new HashSet<Class<?>>();
		for (var c : subTypesOf) {
			classes.add(c);
			var typeVariableMap = GenericTypeResolver.getTypeVariableMap(c);
			typeVariableMap.forEach((tv, clazz) -> {
				log.info("trying to register " + clazz.getTypeName() + '.');
				try {
					classes.add(Class.forName(clazz.getTypeName()));
				}
				catch (UndeclaredThrowableException | ClassNotFoundException e) {
					log.error("couldn't register ", e);
					// ReflectionUtils.rethrowRuntimeException(e);
				}

			});
		}
		return classes;
	}

	private <R extends Annotation> Set<Class<?>> resolveSerializationClasses(Class<R> annotationClazz) {
		try {
			this.log.info("trying to resolve types annotated with {}", annotationClazz.getName());
			var method = annotationClazz.getMethod("using");
			var classes = this.reflections.getTypesAnnotatedWith(annotationClazz);
			var result = new HashSet<Class<?>>();

			classes.forEach(clazzWithAnnotation -> {
				if (this.log.isInfoEnabled()) {
					this.log.info("found {} : {}", clazzWithAnnotation.getName(), annotationClazz.getName());
				}
				result.add(clazzWithAnnotation);
				var annotation = clazzWithAnnotation.getAnnotation(annotationClazz);
				try {
					if (annotation != null) {
						// Add the serializer/deserializer class from the annotation
						var serializerClass = (Class<?>) method.invoke(annotation);
						if (serializerClass != null) {
							result.add(serializerClass);
						}
					}
				}
				catch (Exception e) {
					ReflectionUtils.rethrowRuntimeException(e);
				}

				// Add all child classes (subclasses) of the annotated class
				var subClasses = this.reflections.getSubTypesOf(clazzWithAnnotation);
				subClasses.forEach(subClass -> {
					if (this.log.isInfoEnabled()) {
						this.log.info("found {} : {}", subClass.getName(), annotationClazz.getName());
					}
					result.add(subClass);
				});
			});

			return result;
		} //
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

}
