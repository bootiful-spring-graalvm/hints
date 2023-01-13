package com.joshlong.twitter4j;

import com.joshlong.HintsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

import java.util.List;

/**
 * Makes a <em>trivial</em> Twitter4j client work correctly
 *
 * @author Josh Long
 */
@Slf4j
public class Twitter4jRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		if (!HintsUtils.isClassPresent("twitter4j.UserJSONImpl"))
			return;

		var memberCategories = MemberCategory.values();

		for (var s : List.of("twitter4j.UserJSONImpl", "twitter4j.StatusJSONImpl", "twitter4j.TwitterImpl"))
			hints.reflection().registerType(TypeReference.of(s), memberCategories);

		for (var t : List.of(java.lang.String[].class, long[].class, java.util.Date.class,
				twitter4j.v1.HashtagEntity[].class, twitter4j.v1.URLEntity[].class, twitter4j.v1.MediaEntity[].class,
				twitter4j.v1.SymbolEntity[].class, twitter4j.v1.UserMentionEntity[].class))
			hints.reflection().registerType(t, memberCategories);

	}

}
