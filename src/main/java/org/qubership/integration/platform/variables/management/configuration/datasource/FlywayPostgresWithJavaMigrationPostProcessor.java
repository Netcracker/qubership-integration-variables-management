package org.qubership.integration.platform.variables.management.configuration.datasource;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.qubership.cloud.dbaas.client.FlywayConfigurationProperties;
import org.qubership.cloud.dbaas.client.FlywayPostgresPostProcessor;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.integration.platform.variables.management.db.migration.postgresql.configs.ConfigsJavaMigration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import javax.sql.DataSource;

import static org.qubership.cloud.dbaas.client.DbaasConst.CUSTOM_KEYS;
import static org.qubership.cloud.dbaas.client.DbaasConst.LOGICAL_DB_NAME;

/**
 * Additionally, load java migrations
 */
@Slf4j
@Component("flywayPostgresPostProcessor")
public class FlywayPostgresWithJavaMigrationPostProcessor extends FlywayPostgresPostProcessor {

    private final FlywayConfigurationProperties properties;
    private final List<? extends JavaMigration> configsJavaMigrationList;

    @Autowired
    public FlywayPostgresWithJavaMigrationPostProcessor(FlywayConfigurationProperties properties,
                                                        List<ConfigsJavaMigration> configsJavaMigrationList) {
        super(properties);
        this.properties = properties;
        this.configsJavaMigrationList = configsJavaMigrationList;
    }

    @Override
    public void process(PostgresDatabase postgresDatabase) {
        String databaseName = postgresDatabase.getName();
        log.info("Starting Flyway migration for database: {}", databaseName);

        FluentConfiguration configure = null;
        if (postgresDatabase.isClassifierContainsLogicalDbName()) {
            Map<String, Object> customKeys = (Map<String, Object>) postgresDatabase.getClassifier().get(CUSTOM_KEYS);
            String logicalDbName = (String) customKeys.get(LOGICAL_DB_NAME);
            FlywayConfigurationProperties.Datasource flywayProperties = properties.getDatasources().get(logicalDbName);
            configure = configureFlyway(postgresDatabase.getConnectionProperties().getDataSource(), flywayProperties, logicalDbName, configsJavaMigrationList);
        } else {
            String sharedDbName = "static";
            FlywayConfigurationProperties.Datasource flywayProperties = properties.getDatasource();
            configure = configureFlyway(postgresDatabase.getConnectionProperties().getDataSource(), flywayProperties, sharedDbName, Collections.emptyList());
        }

        configure.load().migrate();
        log.info("Finished Flyway migration for database: {}", databaseName);
    }

    private FluentConfiguration configureFlyway(DataSource dataSource, FlywayConfigurationProperties.Datasource flywayProperties, String logicalDbName, List<? extends JavaMigration> javaMigrations) {
        Map<String, String> flywayPropertiesForDatasource = flywayProperties != null ? flywayProperties.getFlyway() : new HashMap<>();
        FluentConfiguration configure = bindInitialFlywayConfiguration(flywayPropertiesForDatasource, dataSource, javaMigrations);
        if (!flywayPropertiesForDatasource.containsKey(LOCATIONS_PROPERTY)) {
            configure = configure.locations(CLASSPATH_DB_MIGRATION_POSTGRES + "/" + logicalDbName);
        }
        return configure;
    }

    private FluentConfiguration bindInitialFlywayConfiguration(Map<String, String> properties, DataSource dataSource, List<? extends JavaMigration> javaMigrations) {
        Map<String, String> modifiedProperties = enrichPropertiesMapKey(properties);
        Properties propertiesWrapper = new Properties();
        propertiesWrapper.putAll(modifiedProperties);
        FluentConfiguration configure = Flyway.configure().configuration(propertiesWrapper);
        configure.dataSource(dataSource);
        if (!javaMigrations.isEmpty()) {
            configure.javaMigrations(javaMigrations.toArray(new JavaMigration[0]));
        }
        return configure;
    }

    private Map<String, String> enrichPropertiesMapKey(Map<String, String> properties) {
        Map<String, String> modifiedProperties = new HashMap<>();
        properties.forEach((key, value) -> {
            String updatedKey = FLYWAY_PREFIX + key;
            modifiedProperties.put(updatedKey, value);
        });
        return modifiedProperties;
    }

}
