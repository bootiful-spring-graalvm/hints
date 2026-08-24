package com.joshlong.aot.hints.javafx;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JavaFxRuntimeHints implements RuntimeHintsRegistrar {

	private final List<String> nativeCallbacks = List.of("com.sun.glass.events", "com.sun.glass.ui",
			"com.sun.glass.ui.delegate", "com.sun.glass.ui.headless", "com.sun.glass.ui.mac", "com.sun.glass.utils",
			"com.sun.javafx.font.coretext");

	private final List<String> prismShaders = List.of("com.sun.prism.shader");

	private final List<String> effectPeers = List.of("com.sun.scenario.effect.impl.es2",
			"com.sun.scenario.effect.impl.hw.mtl", "com.sun.scenario.effect.impl.prism",
			"com.sun.scenario.effect.impl.prism.ps", "com.sun.scenario.effect.impl.prism.sw",
			"com.sun.scenario.effect.impl.sw.java", "com.sun.scenario.effect.impl.sw.sse");

	private final List<String> publicApi = List.of("javafx.animation", "javafx.application", "javafx.collections",
			"javafx.css", "javafx.event", "javafx.geometry", "javafx.scene", "javafx.scene.control",
			"javafx.scene.effect", "javafx.scene.image", "javafx.scene.layout", "javafx.scene.paint",
			"javafx.scene.shape", "javafx.scene.text", "javafx.scene.transform", "javafx.stage");

	private final List<String> toolkit = List.of("com.sun.javafx", "com.sun.javafx.logging",
			"com.sun.javafx.logging.jfr", "com.sun.javafx.scene.control.skin", "com.sun.javafx.tk.quantum",
			"com.sun.prism", "com.sun.prism.es2");

	/*
	 * these are types used by JNI. Some of them are the same as in the reflection hints.
	 */
	private final List<String> nativeCallbackTypes = Hints.flatten(
			Hints.classNames(Runnable.class, Boolean.class, Class.class, Integer.class, Double.class, Float.class,
					Byte.class, Character.class, Long.class, Object.class, String.class),
			Hints.classNames(Collections.class, HashMap.class, List.class, Map.class),
			Hints.classNames(javafx.scene.paint.Color.class),
			Hints.classNames(javafx.scene.shape.LineTo.class, javafx.scene.shape.MoveTo.class),
			List.of("sun.management.VMManagementImpl"));

	/* `getCanonicalName`, not `getName`: for an array */
	private final List<String> arrays = List.of(com.sun.glass.ui.Screen[].class.getCanonicalName(),
			javafx.scene.paint.Color[].class.getCanonicalName());

	private final List<String> appResources = List.of("styles.css", "templates/*");

	private final List<String> javafxResources = List.of("*.dylib", "com/sun/glass/utils/NativeLibLoader.class",
			"com/sun/javafx/scene/control/skin/modena/**", "com/sun/javafx/scene/control/skin/caspian/**",
			"com/sun/javafx/scene/control/skin/resources/*.properties", "com/sun/javafx/tk/quantum/*.properties",
			"com/sun/prism/es2/glsl/**", "com/sun/prism/mtl/msl/**", "com/sun/scenario/effect/impl/es2/glsl/**");

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		var loader = classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader();
		var reflective = Hints.flatten(this.nativeCallbacks, this.prismShaders, this.effectPeers, this.publicApi,
				this.toolkit);
		Hints.classesInPackages(loader, reflective)
			.forEach(type -> hints.reflection().registerType(type, Hints.EVERYTHING));
		Hints.classesInPackages(loader, this.nativeCallbacks)
			.forEach(type -> hints.jni().registerType(type, Hints.EVERYTHING));
		this.nativeCallbackTypes.forEach(type -> {
			hints.reflection().registerTypeIfPresent(loader, type, Hints.EVERYTHING);
			hints.jni().registerTypeIfPresent(loader, type, Hints.EVERYTHING);
		});
		this.arrays.forEach(type -> hints.reflection().registerTypeIfPresent(loader, type, Hints.EVERYTHING));
		for (var listOfResources : List.of(this.javafxResources, this.appResources))
			listOfResources.forEach(hints.resources()::registerPattern);
	}

}
