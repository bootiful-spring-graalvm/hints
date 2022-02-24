package com.joshlong;

import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.type.NativeConfiguration;

@NativeHint(options = { "-H:+AddAllCharsets", "--enable-https", "--enable-http", })
public class CommonNativeConfiguration implements NativeConfiguration {

}
