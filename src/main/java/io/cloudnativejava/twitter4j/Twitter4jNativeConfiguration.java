package io.cloudnativejava.twitter4j;

import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.nativex.type.NativeConfiguration;

/**
 * Makes a <em>trivial</em> Twitter4j client work correctly
 *
 * @author Josh Long
 */
@NativeHint(options = "--enable-url-protocols=http,https",
		types = @TypeHint(
				access = { TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS, TypeAccess.DECLARED_FIELDS,
						TypeAccess.DECLARED_METHODS, },
				types = { java.lang.String[].class, long[].class, java.util.Date.class, twitter4j.HashtagEntity[].class,
						twitter4j.URLEntity[].class, twitter4j.MediaEntity[].class, twitter4j.SymbolEntity[].class,
						twitter4j.UserMentionEntity[].class,

				}, typeNames = { //
						"twitter4j.UserJSONImpl", "twitter4j.StatusJSONImpl", "twitter4j.StdOutLoggerFactory",
						"twitter4j.AlternativeHttpClientImpl", "twitter4j.conf.PropertyConfigurationFactory",
						"twitter4j.TwitterImpl", "twitter4j.conf.ConfigurationBase", "twitter4j.HttpClientImpl" }) //
)
public class Twitter4jNativeConfiguration implements NativeConfiguration {

}
