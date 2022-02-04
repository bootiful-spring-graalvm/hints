package io.cloudnativejava.templating;

import lombok.extern.slf4j.Slf4j;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.ResourceHint;
import org.springframework.nativex.type.NativeConfiguration;

@ResourceHint(patterns = { "org/commonmark/internal/util/entities.properties" })
@NativeHint(options = "-H:+AddAllCharsets")
@Slf4j
public class TemplateNativeConfiguration implements NativeConfiguration {

	TemplateNativeConfiguration() {
		log.info("contributing template native hints.");
	}

}