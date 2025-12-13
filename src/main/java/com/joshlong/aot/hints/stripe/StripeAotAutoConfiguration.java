package com.joshlong.aot.hints.stripe;

import com.google.gson.*;
import com.google.gson.annotations.*;
import com.stripe.Stripe;
import com.stripe.StripeContext;
import com.stripe.events.*;
import com.stripe.model.*;
import com.stripe.model.v2.EventNotificationClassLookup;
import com.stripe.net.ApiResource;
import com.stripe.net.HttpClient;
import com.stripe.net.Webhook;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.AnnotatedElement;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * registers hints for common types in the Stripe payment processor library.
 *
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass({ StripeContext.class, Gson.class })
@ImportRuntimeHints(StripeAotAutoConfiguration.StripeRuntimeHints.class)
class StripeAotAutoConfiguration {

	/**
	 * generic Stripe AOT hints.
	 */
	static class StripeRuntimeHints implements RuntimeHintsRegistrar {

		private final Logger log = LoggerFactory.getLogger(getClass());

		private final Collection<TypeReference> all;

		private final MemberCategory[] memberCategories = MemberCategory.values();

		private final ClassLoader classLoader = getClass().getClassLoader();

		StripeRuntimeHints() {
			this.all = this.find(StripeContext.class.getPackageName());
		}

		@Override
		public void registerHints(@NonNull RuntimeHints hints, ClassLoader classLoader) {
			this.registerStripeModelClasses(hints);
			this.registerGsonTypeAdapters(hints);
			this.registerUtilityClasses(hints);
			this.registerGsonSerializedTypes(hints);
		}

		private boolean isGsonAnnotationPresent(AnnotatedElement accessibleObject) {
			var gsonAnnotations = Set.of(SerializedName.class, JsonAdapter.class, Since.class, Until.class,
					Expose.class);
			var annotated = new AtomicBoolean(false);
			for (var a : gsonAnnotations)
				if (accessibleObject.isAnnotationPresent(a))
					annotated.set(true);
			return annotated.get();
		}

		private boolean isGsonAnnotated(Class<?> aClass) {

			var annotated = new AtomicBoolean(false);

			if (isGsonAnnotationPresent(aClass))
				annotated.set(true);

			ReflectionUtils.doWithFields(aClass, field -> {
				if (this.isGsonAnnotationPresent(field))
					annotated.set(true);
			});
			ReflectionUtils.doWithMethods(aClass, field -> {
				if (this.isGsonAnnotationPresent(field))
					annotated.set(true);
			});

			this.log.debug("registering {} for reflection", aClass.getName());
			return annotated.get();
		}

		private void registerGsonSerializedTypes(RuntimeHints hints) {
			this.registerConditionally(hints, this::isGsonAnnotated);
		}

		private void registerConditionally(RuntimeHints hints, Predicate<Class<?>> predicate) {
			var toRegister = new HashSet<TypeReference>();
			for (var tr : this.all) {
				try {
					var classLiteral = ClassUtils.forName(tr.getName(), this.classLoader);
					if (predicate.test(classLiteral)) {
						toRegister.add(tr);
					}
				} //
				catch (Throwable throwable) {
					// don't care
				}
			}

			for (var tr : toRegister) {
				hints.reflection().registerType(tr, this.memberCategories);
				this.log.debug("registered Stripe class: {}", tr.getName());
			}
		}

		private void registerStripeModelClasses(RuntimeHints hints) {
			var roots = Set.of(StripeObject.class, StripeActiveObject.class, StripeObjectInterface.class);
			this.registerConditionally(hints, aClass -> {
				// register the types
				if (roots.contains(aClass))
					return true;

				// or classes that extend those types
				for (var root : roots) {
					if (root.isAssignableFrom(aClass)) {
						return true;
					}
				}
				return false;
			});
		}

		private void registerGsonTypeAdapters(RuntimeHints hints) {
			this.registerAll(hints, Set.of(Gson.class, GsonBuilder.class, JsonObject.class, FieldNamingPolicy.class));
			this.registerConditionally(hints, aClass -> JsonSerializer.class.isAssignableFrom(aClass)
					|| JsonDeserializer.class.isAssignableFrom(aClass));
			this.registerAll(hints, Set.of(EventDataClassLookup.class, com.stripe.model.v2.EventDataClassLookup.class,
					EventNotificationClassLookup.class));
			this.registerConditionally(hints, TypeAdapterFactory.class::isAssignableFrom);
		}

		private void registerAll(RuntimeHints hints, Collection<?> types) {
			for (var type : types) {
				if (type instanceof TypeReference typeReference) {
					hints.reflection().registerType(typeReference, this.memberCategories);
				}
				else if (type instanceof Class<?> clazz) {
					hints.reflection().registerType(clazz, this.memberCategories);
				}
				else if (type instanceof String str) {
					hints.reflection().registerType(TypeReference.of(str), this.memberCategories);
				}
				else
					throw new IllegalArgumentException("Unknown type " + type);
			}
		}

		private void registerUtilityClasses(RuntimeHints hints) {

			// Core API and networking classes
			this.registerAll(hints, Set.of(ApiResource.class, HttpClient.class, Webhook.class, Stripe.class));

			// Collection and pagination classes
			this.registerAll(hints, Set.of(StripeCollection.class, StripeSearchResult.class, PagingIterator.class));

			// Amount and common value objects
			this.registerAll(hints, Set.of(com.stripe.v2.Amount.class));

			// Event notification classes
			this.registerAll(hints,
					Set.of(UnknownEventNotification.class, V1BillingMeterErrorReportTriggeredEvent.class,
							V1BillingMeterErrorReportTriggeredEventNotification.class,
							V1BillingMeterNoMeterFoundEvent.class, V2CoreEventDestinationPingEvent.class));

			// ThreadLocalRandom for HTTP client jitter calculations
			this.registerAll(hints, Set.of(ThreadLocalRandom.class));

		}

		protected List<TypeReference> find(String packageName) {
			var scanner = new ClassPathScanningCandidateComponentProvider(false) {
				@Override
				protected boolean isCandidateComponent(@NonNull MetadataReader metadataReader) {
					return true;
				}

				@Override
				protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
					return true;
				}
			};
			return scanner //
				.findCandidateComponents(packageName) //
				.stream()//
				.map(BeanDefinition::getBeanClassName) //
				.filter(Objects::nonNull) //
				.filter(x -> !x.contains("package-info"))
				.map(TypeReference::of) //
				.toList();
		}

	}

}
