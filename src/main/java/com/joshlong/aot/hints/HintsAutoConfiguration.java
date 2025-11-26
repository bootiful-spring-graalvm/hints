package com.joshlong.aot.hints;

import com.joshlong.aot.hints.kubernetes.fabric8.Fabric8BeanFactoryInitializationAotProcessor;
import com.joshlong.aot.hints.kubernetes.fabric8.Fabric8RuntimeHintsRegistrar;
import com.joshlong.aot.hints.liquibase.LiquibaseRuntimeHintsRegistrar;
import com.joshlong.aot.hints.mqtt.EclipsePahoRuntimeHintsRegistrar;
import com.joshlong.aot.hints.rome.RomeRuntimeHintsRegistrar;
import com.joshlong.aot.hints.twitter4j.Twitter4jRuntimeHintsRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 *
 * most of the hints stem from this autoconfiguration class, thought there are some others.
 *
 * @author Josh Long
 */
@Configuration
@ImportRuntimeHints({ EclipsePahoRuntimeHintsRegistrar.class, RomeRuntimeHintsRegistrar.class,
		Fabric8RuntimeHintsRegistrar.class, LiquibaseRuntimeHintsRegistrar.class,
		Twitter4jRuntimeHintsRegistrar.class })
class HintsAutoConfiguration {

	@Bean
	static Fabric8BeanFactoryInitializationAotProcessor fabric8BeanFactoryInitializationAotProcessor() {
		return new Fabric8BeanFactoryInitializationAotProcessor();
	}

}
