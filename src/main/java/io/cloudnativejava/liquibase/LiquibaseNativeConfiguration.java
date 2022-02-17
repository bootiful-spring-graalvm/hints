package io.cloudnativejava.liquibase;

import io.cloudnativejava.HintsUtils;
import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.datatype.LiquibaseDataType;
import liquibase.serializer.LiquibaseSerializable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeResourcesEntry;
import org.springframework.nativex.AotOptions;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.nativex.type.NativeConfiguration;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Supports Liquibase database migrations library
 *
 * @author Josh Long
 */
@Slf4j
public class LiquibaseNativeConfiguration implements NativeConfiguration {

	private final String[] typeNames = { "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
			"com.sun.xml.internal.stream.events.XMLEventFactoryImpl",
			"com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl",
			"com.sun.org.apache.xerces.internal.impl.dv.xs.ExtendedSchemaDVFactoryImpl", };

	private final String[] resources = { "liquibase.build.properties",
			"www.liquibase.org/xml/ns/dbchangelog/dbchangelog-.*\\.xsd",
			"www.liquibase.org/xml/ns/pro/liquibase-.*\\.xsd", };

	private final String[] bundles = { "liquibase/i18n/liquibase-core", };

	@Override
	@SneakyThrows
	public void computeHints(NativeConfigurationRegistry registry, AotOptions aotOptions) {

		log.info("evaluating " + getClass().getName());
		if (!ClassUtils.isPresent("liquibase.plugin.AbstractPlugin", getClass().getClassLoader()))
			return;

		var types = new Class<?>[] { liquibase.lockservice.StandardLockService.class,
				liquibase.plugin.AbstractPlugin.class, liquibase.sql.visitor.AppendSqlVisitor.class,
				liquibase.parser.ChangeLogParserCofiguration.class,
				liquibase.serializer.AbstractLiquibaseSerializable.class,
				liquibase.sql.visitor.RegExpReplaceSqlVisitor.class, liquibase.change.ConstraintsConfig.class,
				liquibase.sql.visitor.PrependSqlVisitor.class, liquibase.license.LicenseServiceFactory.class,
				liquibase.configuration.GlobalConfiguration.class, liquibase.changelog.RanChangeSet.class,
				liquibase.hub.HubServiceFactory.class, liquibase.AbstractExtensibleObject.class,
				liquibase.configuration.LiquibaseConfiguration.class, liquibase.change.ChangeFactory.class,
				liquibase.changelog.StandardChangeLogHistoryService.class, liquibase.database.jvm.JdbcConnection.class,
				liquibase.sql.visitor.ReplaceSqlVisitor.class, liquibase.change.ColumnConfig.class,
				liquibase.executor.ExecutorService.class, liquibase.logging.core.LogServiceFactory.class,
				liquibase.executor.jvm.JdbcExecutor.class, };

		var reflections = new Reflections("liquibase");
		var changes = reflections.getSubTypesOf(Change.class);
		var liquibaseTypes = reflections.getSubTypesOf(LiquibaseDataType.class);
		var liquibaseSerializables = reflections.getSubTypesOf(LiquibaseSerializable.class);
		var databases = reflections.getSubTypesOf(Database.class);

		var compositeTypes = new HashSet<Class<?>>();
		compositeTypes.addAll(changes);
		compositeTypes.addAll(liquibaseTypes);
		compositeTypes.addAll(liquibaseSerializables);
		compositeTypes.addAll(databases);
		compositeTypes.addAll(Arrays.asList(types));
		compositeTypes.addAll(Arrays.stream(this.typeNames).map(HintsUtils::classForName).collect(Collectors.toSet()));

		for (var b : this.bundles)
			registry.resources().add(NativeResourcesEntry.ofBundle(b));

		for (var r : this.resources)
			registry.resources().add(NativeResourcesEntry.of(r));

		var values = TypeAccess.values();
		for (var c : compositeTypes)
			registry.reflection().forType(c).withAccess(values).build();

		log.info("registered " + compositeTypes.size() + " types for reflection for Liquibase");

	}

}
