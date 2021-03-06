package com.joshlong;

import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.type.NativeConfiguration;

/**
 * Honestly, I don't know why this isn't already setup for <em>all</em> Spring Native
 * applications, out of the box. I know, in theory, that we could decide not to support
 * HTTP and HTTPs, but who would? Why would you? If nothing else, it'd be cool if you
 * could opt-out of it? So it's there by default, but you can opt-out? And if we agree
 * HTTP should be there, then surely we want (and indeed prefer) HTTPS? So, anyway, this
 * handles that.
 *
 * @author Josh Long
 */
@NativeHint(options = { "-H:+AddAllCharsets", "--enable-https", "--enable-http", })
public class CommonNativeConfiguration implements NativeConfiguration {

}
