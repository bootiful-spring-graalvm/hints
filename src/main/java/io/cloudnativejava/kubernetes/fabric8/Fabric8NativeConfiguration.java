package io.cloudnativejava.kubernetes.fabric8;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.ExtensionAdapter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.core.GenericTypeResolver;
import org.springframework.nativex.AotOptions;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.nativex.type.NativeConfiguration;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Spring Native support for excellent <a href="https://fabric8.io/">Fabric8 Kubernetes
 * client</a>.
 *
 * @author Josh Long
 */
@Slf4j
@NativeHint(options = { "-H:+AddAllCharsets", "--enable-https", "--enable-url-protocols=https" })
public class Fabric8NativeConfiguration implements NativeConfiguration {

	private final Reflections reflections = new Reflections("io.fabric8", new TypeAnnotationsScanner(),
			new SubTypesScanner(false));

	@Override
	public void computeHints(NativeConfigurationRegistry registry, AotOptions aotOptions) {

		if (!ClassUtils.isPresent("io.fabric8.kubernetes.api.model.KubernetesResource", getClass().getClassLoader()))
			return;

		log.info("running " + Fabric8NativeConfiguration.class.getName());
		var impls = reflections.getAllTypes().stream().filter(cname -> cname.endsWith("Impl")) //
				.map((Function<String, Class<?>>) this::forName).collect(Collectors.toSet());
		var subtypesOfKubernetesResource = reflections.getSubTypesOf(KubernetesResource.class);
		var othersToAddForReflection = List.of(io.fabric8.kubernetes.internal.KubernetesDeserializer.class);
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
			if (log.isInfoEnabled()) {
				log.info("trying to register " + c.getName() + " for reflection");
			}
			registry.reflection().forType(c).withAccess(TypeAccess.values()).build();
		});
	}

	@SneakyThrows
	private Class<?> forName(String name) {
		return Class.forName(name);
	}

	private Set<Class<?>> registerExtensionAdapters() {
		Set<Class<? extends ExtensionAdapter>> subTypesOf = this.reflections.getSubTypesOf(ExtensionAdapter.class);

		Set<Class<?>> classes = new HashSet<>();

		for (var c : subTypesOf) {
			classes.add(c);
			Map<TypeVariable, Type> typeVariableMap = GenericTypeResolver.getTypeVariableMap(c);
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
