package com.joshlong.rome;

import com.joshlong.HintsUtils;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class RomeRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		if (!HintsUtils.isClassPresent("com.rometools.rome.feed.WireFeed"))
			return;

		var mcs = MemberCategory.values();

		// rome
		for (var c : new Class<?>[] { com.rometools.rome.feed.module.DCModuleImpl.class })
			hints.reflection().registerType(c, mcs);

		var resource = new ClassPathResource("/com/rometools/rome/rome.properties");
		hints.resources().registerResource(resource);
		try (var in = resource.getInputStream()) {
			var props = new Properties();
			props.load(in);
			props.propertyNames().asIterator().forEachRemaining(pn -> {
				var classes = loadClasses((String) pn, props.getProperty((String) pn));
				classes.forEach(cn -> hints.reflection().registerType(TypeReference.of(cn), mcs));
			});
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static List<String> loadClasses(String propertyName, String propertyValue) {
		Assert.hasText(propertyName, "the propertyName must not be null");
		Assert.hasText(propertyValue, "the propertyValue must not be null");
		return Arrays //
				.stream((propertyValue.contains(" ")) ? propertyValue.split(" ") : new String[] { propertyValue }) //
				.map(String::trim).filter(xValue -> !xValue.strip().equals("")).toList();
	}

}
