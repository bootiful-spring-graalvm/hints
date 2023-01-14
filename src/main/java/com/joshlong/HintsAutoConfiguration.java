package com.joshlong;

import com.joshlong.kubernetes.fabric8.Fabric8BeanFactoryInitializationAotProcessor;
import com.joshlong.kubernetes.fabric8.Fabric8RuntimeHintsRegistrar;
import com.joshlong.liquibase.LiquibaseRuntimeHintsRegistrar;
import com.joshlong.rome.RomeRuntimeHintsRegistrar;
import com.joshlong.twitter4j.Twitter4jRuntimeHintsRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints({ RomeRuntimeHintsRegistrar.class, Fabric8RuntimeHintsRegistrar.class,
		LiquibaseRuntimeHintsRegistrar.class, Twitter4jRuntimeHintsRegistrar.class })
class HintsAutoConfiguration {

	@Bean
	static Fabric8BeanFactoryInitializationAotProcessor fabric8BeanFactoryInitializationAotProcessor() {
		return new Fabric8BeanFactoryInitializationAotProcessor();
	}

}
