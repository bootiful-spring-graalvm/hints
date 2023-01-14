package com.joshlong.twitter4j;

import com.joshlong.HintsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.reflections.Reflections;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import twitter4j.v1.TweetEntity;

import java.io.Serializable;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Makes a <em>trivial</em> Twitter4j client work correctly
 *
 * @author Josh Long
 */
public class Twitter4jRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (!HintsUtils.isClassPresent("twitter4j.UserJSONImpl"))
			return;
		var memberCategories = MemberCategory.values();
		var reflections = new Reflections("twitter4j");
		List.of(ServiceLoader.class, DefaultFlowMessageFactory.class, ParameterizedMessageFactory.class,
				LogManager.class).forEach(o -> hints.reflection().registerType(o, memberCategories));
		List.of("twitter4j.UserJSONImpl", "twitter4j.StatusJSONImpl", "twitter4j.TwitterImpl")
				.forEach(s -> hints.reflection().registerType(TypeReference.of(s), memberCategories));
		List.of(java.lang.String[].class, long[].class, java.util.Date.class)
				.forEach(t -> hints.reflection().registerType(t, memberCategories));
		reflections.getSubTypesOf(Serializable.class)
				.forEach(t -> hints.reflection().registerType(t, memberCategories));
		reflections.getSubTypesOf(TweetEntity.class).forEach(t -> hints.reflection().registerType(t, memberCategories));
	}

}
