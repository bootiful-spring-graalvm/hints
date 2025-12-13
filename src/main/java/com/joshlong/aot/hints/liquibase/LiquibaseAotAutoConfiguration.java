package com.joshlong.aot.hints.liquibase;

import com.joshlong.aot.hints.HintsUtils;
import liquibase.change.Change;
import liquibase.database.Database;
import liquibase.datatype.LiquibaseDataType;
import liquibase.serializer.LiquibaseSerializable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * registers hints for the Liquibase database migration library.
 */
@Configuration
@ImportRuntimeHints(LiquibaseAotAutoConfiguration.LiquibaseRuntimeHintsRegistrar.class)
@ConditionalOnClass(name = "liquibase.plugin.AbstractPlugin")
class LiquibaseAotAutoConfiguration {

	/**
	 * Provides support for Liquibase database migrations. This draws and expands on some
	 * amazing work that <a href="https://twitter.com/ilopmar">Iván López</a> and
	 * <a href="https://twitter.com/mRaible">Matt Raible</a> did.
	 *
	 * @author Josh Long
	 */
	@Deprecated
	static class LiquibaseRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

		private final String[] typeNames = { "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
				"com.sun.xml.internal.stream.events.XMLEventFactoryImpl",
				"com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl",
				"com.sun.org.apache.xerces.internal.impl.dv.xs.ExtendedSchemaDVFactoryImpl", };

		private final String[] resources = { "liquibase.build.properties",
				"www.liquibase.org/xml/ns/dbchangelog/dbchangelog-.*\\.xsd",
				"www.liquibase.org/xml/ns/pro/liquibase-.*\\.xsd", };

		private final String[] bundles = { "liquibase/i18n/liquibase-core", };

		private final Logger log = LoggerFactory.getLogger(getClass());

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

			if (!HintsUtils.isClassPresent("liquibase.plugin.AbstractPlugin"))
				return;

			var types = new Class<?>[] { liquibase.lockservice.StandardLockService.class,
					liquibase.plugin.AbstractPlugin.class, liquibase.sql.visitor.AppendSqlVisitor.class,
					liquibase.parser.ChangeLogParserCofiguration.class,
					liquibase.serializer.AbstractLiquibaseSerializable.class,
					liquibase.sql.visitor.RegExpReplaceSqlVisitor.class, liquibase.change.ConstraintsConfig.class,
					liquibase.sql.visitor.PrependSqlVisitor.class, liquibase.license.LicenseServiceFactory.class,
					liquibase.configuration.GlobalConfiguration.class, liquibase.changelog.RanChangeSet.class,
					liquibase.AbstractExtensibleObject.class, liquibase.configuration.LiquibaseConfiguration.class,
					liquibase.change.ChangeFactory.class, liquibase.changelog.StandardChangeLogHistoryService.class,
					liquibase.database.jvm.JdbcConnection.class, liquibase.sql.visitor.ReplaceSqlVisitor.class,
					liquibase.change.ColumnConfig.class, liquibase.executor.ExecutorService.class,
					liquibase.logging.core.LogServiceFactory.class, liquibase.executor.jvm.JdbcExecutor.class, };

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
			compositeTypes
				.addAll(Arrays.stream(this.typeNames).map(HintsUtils::classForName).collect(Collectors.toSet()));

			for (var b : this.bundles)
				hints.resources().registerResourceBundle(b);

			for (var r : Arrays.stream(this.resources)//
				.map(ClassPathResource::new)//
				.filter(ClassPathResource::exists)//
				.toList())
				hints.resources().registerResource(r);

			var values = MemberCategory.values();
			for (var c : compositeTypes)
				hints.reflection().registerType(c, values);

			this.log.info("registered {} types for reflection for Liquibase", compositeTypes.size());

		}

	}

}
