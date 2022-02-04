package io.cloudnativejava.lucene;

import io.cloudnativejava.HintsUtils;

import java.util.function.BooleanSupplier;

class LuceneBooleanSupplier implements BooleanSupplier {

	@Override
	public boolean getAsBoolean() {
		return HintsUtils.isClassPresent("org.apache.lucene.util.AttributeFactory");
	}

}
