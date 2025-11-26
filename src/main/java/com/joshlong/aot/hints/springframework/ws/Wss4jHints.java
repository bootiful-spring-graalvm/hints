package com.joshlong.aot.hints.springframework.ws;

import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.transform.AttachmentCiphertextTransform;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ReflectionUtils;

import java.util.Map;

/**
 * Supports GraalVM native image hints for WSS4J security integration with Spring WS.
 *
 * @author Josh Long
 */
class Wss4jHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
		LoggerFactory.getLogger(Wss4jHints.class).info("Registering WSS4J Hint for ");
		this.registerWssConfigClasses(hints);
	}

	private void registerWssConfigClasses(RuntimeHints hints) {
		var instance = WSSConfig.getNewInstance();
		hints.reflection().registerType(AttachmentCiphertextTransform.class, MemberCategory.values());
		for (var fieldName : new String[] { "actionMap", "processorMap", "validatorMap" })
			this.registerWssConfigType(instance, fieldName, hints);
	}

	private void registerWssConfigType(WSSConfig instance, String fieldName, RuntimeHints hints) {
		try {
			// yuck. O_o
			var dp = WSSConfig.class.getDeclaredField(fieldName);
			ReflectionUtils.makeAccessible(dp);
			var map = (Map<?, ?>) dp.get(instance);
			for (var clazz : map.values()) {
				if (clazz instanceof Class<?> v) {
					hints.reflection().registerType(v, MemberCategory.values());
				}
			}
		} //
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
