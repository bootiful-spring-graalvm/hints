package com.joshlong.aot.hints.springframework.ws;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.util.Assert;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * registers AOT hints for Spring WS, a project that is nearly 20 years old but that,
 * through the magic of Java virtual threads and GraalVM and Spring's amazing backwards
 * could run as efficiently as possible in modern day code.
 *
 * @author Josh Long
 */
@AutoConfiguration
@ConditionalOnClass({ Endpoint.class, RequestPayload.class })
@ImportRuntimeHints({ SpringWsHints.class })
class SpringWsConfiguration {

	SpringWsConfiguration() {
		LoggerFactory.getLogger(SpringWsConfiguration.class).debug("initializing AOT support for Spring WS");
	}

	@Configuration
	@ConditionalOnClass(WSSConfig.class)
	@ImportRuntimeHints(Wss4jHints.class)
	static class Wss4jConfiguration {

		Wss4jConfiguration() {
			LoggerFactory.getLogger(Wss4jConfiguration.class).debug("initializing AOT support for Wss4j");
		}

	}

	@Bean
	static JaxbBeanFactoryInitializationAotProcessor jaxbBeanFactoryInitializationAotProcessor() {
		return new JaxbBeanFactoryInitializationAotProcessor();
	}

	@Bean
	static EndpointBeanFactoryInitializationAotProcessor endpointBeanFactoryInitializationAotProcessor() {
		return new EndpointBeanFactoryInitializationAotProcessor();
	}

	@Bean
	static WsdlLocationBeanFactoryInitializationAotProcessor wsdlLocationBeanFactoryInitializationAotProcessor() {
		return new WsdlLocationBeanFactoryInitializationAotProcessor();
	}

	static class JaxbBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

		@Override
		public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(
				@NonNull ConfigurableListableBeanFactory beanFactory) {
			var classes = new HashSet<Class<?>>();
			for (var pkg : AutoConfigurationPackages.get(beanFactory)) {
				for (var c : AotUtils.findAllClasses(pkg)) {
					if (this.isJaxbClass(c))
						classes.add(c);
				}
			}
			return (generationContext, code) -> {
				var values = MemberCategory.values();
				var reflection = generationContext.getRuntimeHints().reflection();
				for (var clazz : classes) {
					reflection.registerType(clazz, values);
				}
			};
		}

		private boolean isJaxbClass(Class<?> clzz) {
			var annotations = Set.of(XmlRootElement.class, XmlType.class, XmlAccessorType.class, XmlAccessorOrder.class,
					XmlSeeAlso.class, XmlRegistry.class, XmlEnum.class, XmlTransient.class, XmlJavaTypeAdapter.class);
			for (var jaxbRootAnnotation : annotations) {
				if (clzz.getAnnotation(jaxbRootAnnotation) != null) {
					return true;
				}
			}
			return false;
		}

	}

	static class WsdlLocationBeanFactoryInitializationAotProcessor
			implements ApplicationContextAware, BeanFactoryInitializationAotProcessor {

		private ApplicationContext applicationContext;

		@Override
		public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(
				@NonNull ConfigurableListableBeanFactory beanFactory) {
			Assert.notNull(this.applicationContext, "ApplicationContext must not be null");
			Assert.notNull(this.applicationContext.getEnvironment(), "the environment must not be null");
			var binder = Binder.get(this.applicationContext.getEnvironment());
			var wsdlLocations = binder.bind("spring.webservices.wsdl-locations", Bindable.listOf(String.class))
				.orElse(Collections.emptyList());
			return (generationContext, code) -> {
				var resources = generationContext.getRuntimeHints().resources();
				for (var wsdlLocation : wsdlLocations) {
					resources.registerPattern(wsdlLocation);
				}
			};
		}

		@Override
		public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

	}

	static class EndpointBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

		@Override
		public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(
				ConfigurableListableBeanFactory beanFactory) {

			var endpoints = new HashSet<TypeReference>();
			var beanNamesForAnnotation = beanFactory.getBeanNamesForAnnotation(Endpoint.class);
			for (var beanName : beanNamesForAnnotation) {
				var type = beanFactory.getType(beanName);
				Assert.notNull(type, "the type for beanName " + beanName + " not found");
				endpoints.add(TypeReference.of(type));
			}
			return (generationContext, code) -> {
				var runtimeHints = generationContext.getRuntimeHints().reflection();
				for (var tr : endpoints) {
					runtimeHints.registerType(tr, MemberCategory.values());
				}
			};
		}

	}

}
