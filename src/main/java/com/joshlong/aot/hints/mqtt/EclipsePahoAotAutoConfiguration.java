package com.joshlong.aot.hints.mqtt;

import com.joshlong.aot.hints.HintsUtils;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.stream.Stream;

@Configuration
@ConditionalOnClass(name = "org.eclipse.paho.client.mqttv3.logging.JSR47Logger")
@ImportRuntimeHints(EclipsePahoAotAutoConfiguration.EclipsePahoRuntimeHintsRegistrar.class)
class EclipsePahoAotAutoConfiguration {

	static class EclipsePahoRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

			if (!(HintsUtils.isClassPresent("org.eclipse.paho.client.mqttv3.logging.JSR47Logger")
					|| HintsUtils.isClassPresent("org.eclipse.paho.mqttv5.client.logging.JSR47Logger")))
				return;

			var values = MemberCategory.values();
			Stream
				.of("org.eclipse.paho.client.mqttv3.logging.JSR47Logger",
						"org.eclipse.paho.mqttv5.client.logging.JSR47Logger")
				.map(TypeReference::of)
				.forEach(tr -> hints.reflection().registerType(tr, values));
			Stream.of("org/eclipse/paho/client/mqttv3/internal/nls/messages",
					"org/eclipse/paho/client/mqttv3/internal/nls/logcat", "org/eclipse/paho/mqttv5/common/nls/messages",
					"org/eclipse/paho/mqttv5/client/internal/nls/logcat")
				.forEach(bundle -> hints.resources().registerResourceBundle(bundle));
		}

	}

}
