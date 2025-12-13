package com.joshlong.aot.hints.kubernetes.fabric8;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Registers support for the Fabric8 Kubernetes client.
 */
@Configuration
@ConditionalOnClass(io.fabric8.kubernetes.client.CustomResource.class)
@ImportRuntimeHints(Fabric8RuntimeHintsRegistrar.class)
class Fabric8AotAutoConfiguration {

	@Bean
	static Fabric8BeanFactoryInitializationAotProcessor fabric8BeanFactoryInitializationAotProcessor() {
		return new Fabric8BeanFactoryInitializationAotProcessor();
	}

}
