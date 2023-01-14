package com.joshlong.kubernetes.fabric8;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.joshlong.HintsUtils;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.ExtensionAdapter;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring Boot 3 AOT support for the <a href="https://fabric8.io/">Fabric8 Kubernetes
 * client</a>.
 *
 * Alternatively, if you want to use <a href="https://github.com/kubernetes-client/java">
 * the official Kubernetes Java client</a>, there's support in that project directly.
 *
 * @author Josh Long
 */
@Slf4j
public class Fabric8RuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	private final Reflections reflections = new Reflections("io.fabric8");

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		if (!HintsUtils.isClassPresent("io.fabric8.kubernetes.api.model.KubernetesResource"))
			return;

		if (log.isDebugEnabled())
			log.debug("running " + Fabric8RuntimeHintsRegistrar.class.getName());

		var impls = reflections//
				.getAllTypes()//
				.stream()//
				.filter(cname -> cname.endsWith("Impl")) //
				.map((Function<String, Class<?>>) this::forName)//
				.collect(Collectors.toSet());
		var subtypesOfKubernetesResource = reflections.getSubTypesOf(KubernetesResourceUtil.class);
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

	@SneakyThrows
	private Class<?> forName(String name) {
		return Class.forName(name);
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
				catch (ClassNotFoundException e) {
					ReflectionUtils.rethrowRuntimeException(e);
				}

			});
		}
		return classes;
	}

	@SneakyThrows
	private <R extends Annotation> Set<Class<?>> resolveSerializationClasses(Class<R> annotationClazz) {
		log.info("trying to resolve types annotated with " + annotationClazz.getName());
		var method = annotationClazz.getMethod("using");
		var classes = this.reflections.getTypesAnnotatedWith(annotationClazz);
		return classes.stream().map(clazzWithAnnotation -> {
			if (log.isInfoEnabled()) {
				log.info("found " + clazzWithAnnotation.getName() + " : " + annotationClazz.getName());
			}
			var annotation = clazzWithAnnotation.getAnnotation(annotationClazz);
			try {
				if (annotation != null) {
					return (Class<?>) method.invoke(annotation);
				}
			}
			catch (Exception e) {
				ReflectionUtils.rethrowRuntimeException(e);
			}
			return null;
		}).collect(Collectors.toSet());
	}

}
