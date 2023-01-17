package com.joshlong.mqtt;

import com.joshlong.HintsUtils;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

import java.util.stream.Stream;

/**
 *
 * Support for MQTT via <a href="https://www.eclipse.org/paho/">the Eclipse Paho
 * project</a>
 *
 * @author Josh Long
 */
public class EclipsePahoRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		if (!(HintsUtils.isClassPresent("org.eclipse.paho.client.mqttv3.logging.JSR47Logger")
				|| HintsUtils.isClassPresent("org.eclipse.paho.mqttv5.client.logging.JSR47Logger")))
			return;

		var values = MemberCategory.values();
		Stream.of("org.eclipse.paho.client.mqttv3.logging.JSR47Logger",
				"org.eclipse.paho.mqttv5.client.logging.JSR47Logger").map(TypeReference::of)
				.forEach(tr -> hints.reflection().registerType(tr, values));
		Stream.of("org/eclipse/paho/client/mqttv3/internal/nls/messages",
				"org/eclipse/paho/client/mqttv3/internal/nls/logcat", "org/eclipse/paho/mqttv5/common/nls/messages",
				"org/eclipse/paho/mqttv5/client/internal/nls/logcat")
				.forEach(bundle -> hints.resources().registerResourceBundle(bundle));
	}

}
