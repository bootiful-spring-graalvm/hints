package com.joshlong.aot.hints.javafx;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class Hints {

	static final MemberCategory[] EVERYTHING = Stream.of(MemberCategory.values()) //
		.filter(category -> {//
			try {
				return !MemberCategory.class.getField(category.name()) //
					.isAnnotationPresent(Deprecated.class);
			} //
			catch (NoSuchFieldException noSuchField) {
				throw new IllegalStateException(noSuchField);
			}
		}) //
		.toArray(MemberCategory[]::new);

	static List<String> classNames(Class<?>... classes) {
		return Stream.of(classes).map(Class::getName).toList();
	}

	@SafeVarargs
	static List<String> flatten(Collection<String>... groups) {
		return Stream.of(groups).flatMap(Collection::stream).toList();
	}

	/**
	 * Enumerate the types in each package by reading the class files off the classpath -
	 * no class loading, no initialization, just the names in the constant pool.
	 */
	static Collection<TypeReference> classesInPackages(ClassLoader classLoader, Collection<String> packageNames) {
		var resolver = new PathMatchingResourcePatternResolver(classLoader);
		var metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
		var classNames = new TreeSet<String>();
		for (var packageName : packageNames) {
			var pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
					+ ClassUtils.convertClassNameToResourcePath(packageName) + "/*.class";
			for (var resource : resources(resolver, pattern)) {
				if (isSynthetic(resource.getFilename())) {
					continue;
				}
				try {
					classNames.add(metadataReaderFactory.getMetadataReader(resource).getClassMetadata().getClassName());
				} //
				catch (IOException ioException) {
					throw new UncheckedIOException("could not read [" + resource + "]", ioException);
				}
			}
		}
		return classNames//
			.stream() //
			.map(TypeReference::of) //
			.collect(Collectors.toUnmodifiableSet());
	}

	static Collection<Resource> resources(ResourcePatternResolver resolver, String pattern) {
		try {
			return Stream.of(resolver.getResources(pattern)).filter(Resource::isReadable).toList();
		} //
		catch (IOException ioException) {
			throw new UncheckedIOException("could not resolve [" + pattern + "]", ioException);
		}
	}

	private static boolean isSynthetic(String filename) {
		return filename == null || filename.startsWith("package-info") || filename.startsWith("module-info");
	}

}
