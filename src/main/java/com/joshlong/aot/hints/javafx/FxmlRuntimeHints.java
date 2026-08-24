package com.joshlong.aot.hints.javafx;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

class FxmlRuntimeHints implements RuntimeHintsRegistrar {

	private static final String DOCUMENTS = "fxml/**/*.fxml";

	/* `<?import javafx.scene.control.Button?>`, or `<?import javafx.scene.control.*?>` */
	private static final Pattern IMPORT = Pattern.compile("<\\?import\\s+([\\w.$*]+)\\s*\\?>");

	private static final String WILDCARD = ".*";

	private final List<String> loader = List.of("javafx.fxml");

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		var loaderToUse = classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader();
		Hints.classesInPackages(loaderToUse, this.loader)
			.forEach(type -> hints.reflection().registerType(type, Hints.EVERYTHING));
		hints.resources().registerPattern(DOCUMENTS);
		var resolver = new PathMatchingResourcePatternResolver(loaderToUse);
		var imported = imports(Hints.resources(resolver, ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + DOCUMENTS));
		imported.stream()
			.filter(name -> !name.endsWith(WILDCARD))
			.forEach(name -> hints.reflection().registerTypeIfPresent(loaderToUse, name, Hints.EVERYTHING));
		var packages = imported.stream()
			.filter(name -> name.endsWith(WILDCARD))
			.map(name -> name.substring(0, name.length() - WILDCARD.length()))
			.toList();
		Hints.classesInPackages(loaderToUse, packages)
			.forEach(type -> hints.reflection().registerType(type, Hints.EVERYTHING));
	}

	private static Collection<String> imports(Collection<Resource> documents) {
		var imports = new TreeSet<String>();
		for (var document : documents) {
			try (var in = document.getInputStream()) {
				var matcher = IMPORT.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
				while (matcher.find()) {
					imports.add(matcher.group(1));
				}
			} //
			catch (IOException ioException) {
				throw new UncheckedIOException("could not read [" + document + "]", ioException);
			}
		}
		return imports;
	}

}
