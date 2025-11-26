package com.joshlong.aot.hints.springframework.ws;

import com.joshlong.aot.hints.HintsUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.server.EndpointAdapter;
import org.springframework.ws.server.EndpointExceptionResolver;
import org.springframework.ws.server.EndpointMapping;
import org.springframework.ws.server.endpoint.MethodEndpoint;
import org.springframework.ws.server.endpoint.PayloadEndpoint;
import org.springframework.ws.server.endpoint.adapter.AbstractMethodEndpointAdapter;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.soap.server.endpoint.adapter.method.SoapHeaderElementMethodArgumentResolver;
import org.springframework.ws.soap.server.endpoint.adapter.method.SoapMethodArgumentResolver;
import org.springframework.ws.transport.WebServiceMessageReceiver;
import org.springframework.ws.transport.http.WebServiceMessageReceiverHandlerAdapter;

import java.util.Arrays;
import java.util.Properties;

/**
 * Registers types related to the core Spring WS machinery in
 * spring-boot-starter-web-services
 *
 * @author Josh Long
 */
class SpringWsHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
		var values = MemberCategory.values();
		this.registerXmlRelatedHints(hints, values);
		this.registerMiscSpringWsTypes(hints, values);
		this.registerEndpoints(hints, values);
		this.registerResources(hints);
	}

	private void registerResources(RuntimeHints hints) {
		for (var config : new String[] { "org/springframework/ws/server/MessageDispatcher.properties",
				"org/springframework/ws/client/core/WebServiceTemplate.properties",
				"org/springframework/ws/soap/server/SoapMessageDispatcher.properties",
				"org/springframework/ws/transport/http/MessageDispatcherServlet.properties" }) {
			try {
				this.registerWsServiceLoaderProperties(config, hints);
			} //
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		for (var r : new String[] { "org/apache/xml/security/resource/xmlsecurity*.properties",
				"com/sun/org/apache/xml/internal/security/resource/xmlsecurity_en.properties" })
			hints.resources().registerPattern(r);

		for (var p : new String[] { "messages/wss4j_errors", "com.sun.xml.messaging.saaj.util.LocalStrings" })
			hints.resources().registerResourceBundle(p);
	}

	private void registerEndpoints(RuntimeHints hints, MemberCategory[] values) {
		for (var a : HintsUtils.findAllClasses(Endpoint.class.getPackageName()))
			hints.reflection().registerType(TypeReference.of(a), values);
	}

	private void registerMiscSpringWsTypes(RuntimeHints hints, MemberCategory[] values) {
		for (var c : new Class<?>[] { AbstractMethodEndpointAdapter.class, EndpointAdapter.class,
				EndpointExceptionResolver.class, EndpointMapping.class, MethodEndpoint.class, PayloadEndpoint.class,
				SoapHeaderElementMethodArgumentResolver.class, SoapMethodArgumentResolver.class,
				WebServiceMessageFactory.class, WebServiceMessageReceiver.class,
				WebServiceMessageReceiverHandlerAdapter.class, })
			hints.reflection().registerType(c, values);
	}

	private void registerXmlRelatedHints(RuntimeHints hints, MemberCategory[] values) {
		for (var c : new String[] { "nu.xom.Element", "org.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl",
				"org.glassfish.jaxb.runtime.v2.runtime.property.SingleElementNodeProperty", "org.dom4j.Element",
				"com.sun.org.apache.xpath.internal.functions.FuncNormalizeSpace",
				"com.sun.xml.internal.messaging.saaj.soap.SOAPDocumentImpl",
				"com.sun.xml.messaging.saaj.soap.SOAPDocumentImpl",
				"org.glassfish.jaxb.runtime.v2.model.runtime.RuntimeElementPropertyInfo", "org.jdom2.Element" })
			hints.reflection().registerType(TypeReference.of(c), values);
	}

	private void registerWsServiceLoaderProperties(String url, RuntimeHints hints) throws Exception {
		var classPathResource = new ClassPathResource(url);
		var properties = new Properties();
		var commaDelimiter = ",";
		try (var inputStream = classPathResource.getInputStream();) {
			properties.load(inputStream);
			properties.propertyNames().asIterator().forEachRemaining(key -> {
				var classes = properties.getProperty((String) key);
				var splitClasses = Arrays
					.stream(classes.contains(commaDelimiter) ? classes.split(commaDelimiter) : new String[] { classes })
					.map(String::strip)
					.toList();
				hints.resources().registerPattern(url);
				for (var clazz : splitClasses) {
					hints.reflection().registerType(TypeReference.of(clazz), MemberCategory.values());
				}
			});

		}
	}

}
