package com.joshlong;

import com.joshlong.liquibase.LiquibaseRuntimeHintsRegistrar;
import com.joshlong.twitter4j.Twitter4jRuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints({ CommonNativeConfiguration.class, LiquibaseRuntimeHintsRegistrar.class,
		Twitter4jRuntimeHintsRegistrar.class })
class HintsAutoConfiguration {

}
