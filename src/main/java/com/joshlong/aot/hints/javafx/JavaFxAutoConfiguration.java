package com.joshlong.aot.hints.javafx;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ImportRuntimeHints;

@AutoConfiguration
@ImportRuntimeHints({ JavaFxRuntimeHints.class, FxmlRuntimeHints.class })
class JavaFxAutoConfiguration {

}
