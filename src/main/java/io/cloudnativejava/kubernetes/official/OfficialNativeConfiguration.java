package io.cloudnativejava.kubernetes.official;

import com.google.gson.annotations.JsonAdapter;
import io.cloudnativejava.HintsUtils;
import io.swagger.annotations.ApiModel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.nativex.AotOptions;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.nativex.type.NativeConfiguration;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.nativex.hint.TypeAccess.*;

/**
 * These hints are inspired by <a href="https://github.com/scratches/spring-controller">
 * Dr. Dave Syer's sample Kubernetes controller</a> and the configuration therein.
 * <p>
 * These types work <a href="https://github.com/kubernetes-client/java">in conjunction
 * with the autoconfiguration provided by the official Kubernetes Java client</a>, most of
 * which is code-generated from Swagger. This support automatically registers any
 * code-generated types that have {@link io.swagger.annotations.ApiModel} on it, limiting
 * the registration to the code-generated types in the {@link io.kubernetes} package.
 * <p>
 * This hints class also registers options required to use this with a HTTPS API endpoints
 * with custom character sets.
 *
 * @author Josh Long
 * @author Dave Syer
 * @author Christian Tzolov
 */
@NativeHint(//
		options = { "-H:+AddAllCharsets", "--enable-all-security-services", "--enable-https", "--enable-http" },
		types = { //
				@TypeHint( //
						access = { DECLARED_CLASSES, DECLARED_CONSTRUCTORS, DECLARED_FIELDS, DECLARED_METHODS }, //
						typeNames = { //
								"io.kubernetes.client.informer.cache.ProcessorListener",
								"io.kubernetes.client.extended.controller.Controller",
								"io.kubernetes.client.util.generic.GenericKubernetesApi$StatusPatch",
								"io.kubernetes.client.util.Watch$Response", }) //
		}//
)
@Slf4j
public class OfficialNativeConfiguration implements NativeConfiguration {

	@Override
	public void computeHints(NativeConfigurationRegistry registry, AotOptions aotOptions) {

		if (!HintsUtils.isClassPresent("io.kubernetes.client.extended.controller.Controller"))
			return;

		var reflections = new Reflections("io.kubernetes");
		var apiModels = reflections.getTypesAnnotatedWith(ApiModel.class);
		var jsonAdapters = findJsonAdapters(reflections);

		var all = new HashSet<Class<?>>();
		all.addAll(jsonAdapters);
		all.addAll(apiModels);
		all.forEach(clzz -> registry.reflection().forType(clzz).withAccess(values()).build());
	}

	@SneakyThrows
	private <R extends Annotation> Set<Class<?>> findJsonAdapters(Reflections reflections) {
		var jsonAdapterClass = JsonAdapter.class;
		return reflections.getTypesAnnotatedWith(jsonAdapterClass).stream().flatMap(clazz -> {
			var list = new HashSet<Class<?>>();
			var annotation = clazz.getAnnotation(jsonAdapterClass);
			if (null != annotation) {
				list.add(annotation.value());
			}
			list.add(clazz);
			if (log.isDebugEnabled()) {
				list.forEach(c -> log.debug("found @JsonAdapter type: " + c.getName()));
			}
			return list.stream();
		}).collect(Collectors.toSet());
	}

}
