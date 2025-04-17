package org.qubership.integration.platform.variables.management.configuration.datasource;

import jakarta.persistence.SharedCacheMode;
import lombok.Getter;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.qubership.cloud.dbaas.client.config.EnableDbaasDefault;
import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import org.qubership.cloud.dbaas.client.entity.settings.PostgresSettings;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.DbaasPostgresProxyDataSource;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import org.qubership.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;
import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.integration.platform.variables.management.configuration.TenantConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Collections;
import java.util.Properties;
import javax.sql.DataSource;

import static org.qubership.cloud.dbaas.client.DbaasConst.LOGICAL_DB_NAME;

@Getter
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableJpaAuditing
@EnableDbaasDefault
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "org.qubership.integration.platform.variables.management.persistence.configs.repository",
        transactionManagerRef = "configsTransactionManager"
)
@EnableConfigurationProperties(JpaProperties.class)
public class PersistenceConfigsConfiguration {
    private static final String JPA_ENTITIES_PACKAGE_SCAN =
            "org.qubership.integration.platform.variables.management.persistence.configs.entity";
    private final JpaProperties jpaProperties;
    private final TenantConfiguration tenantConfiguration;

    @Autowired
    public PersistenceConfigsConfiguration(JpaProperties jpaProperties, TenantConfiguration tenantConfiguration) {
        this.jpaProperties = jpaProperties;
        this.tenantConfiguration = tenantConfiguration;
    }

    @Primary
    @Bean("configsDataSource")
    @ConditionalOnProperty(value = "qip.datasource.configuration.enabled", havingValue = "true", matchIfMissing = true)
    DataSource dataSource(DatabasePool dbaasConnectionPool,
                          DbaasClassifierFactory classifierFactory,
                          @Autowired(required = false) @Qualifier("dbaasApiProperties") DbaasApiProperties dbaasApiProperties
    ) {
        PostgresSettings databaseSettings =
                new PostgresSettings(dbaasApiProperties == null
                        ? Collections.emptyMap()
                        : dbaasApiProperties.getDatabaseSettings(DbaasApiProperties.DbScope.TENANT));

        DatabaseConfig.Builder builder = DatabaseConfig.builder()
                .databaseSettings(databaseSettings);

        if (dbaasApiProperties != null) {
            builder
                    .userRole(dbaasApiProperties.getRuntimeUserRole())
                    .dbNamePrefix(dbaasApiProperties.getDbPrefix());
        }

        return new DbaasPostgresProxyDataSource(
                dbaasConnectionPool,
                new DefaultTenantDbaaSClassifierBuilder(
                        tenantConfiguration.getDefaultTenant(),
                        classifierFactory
                                .newTenantClassifierBuilder()
                                .withCustomKey(LOGICAL_DB_NAME, "configs")
                ),
                builder.build());
    }

    @Bean
    JdbcTemplate configsJdbcTemplate(@Qualifier("configsDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    NamedParameterJdbcTemplate configsNamedParameterJdbcTemplate(@Qualifier("configsDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Primary
    @Bean("entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean configsEntityManagerFactory(
            @Qualifier("configsDataSource") DataSource dataSource
    ) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();

        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabase(jpaProperties.getDatabase());
        jpaVendorAdapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        jpaVendorAdapter.setShowSql(jpaProperties.isShowSql());

        em.setDataSource(dataSource);
        em.setJpaVendorAdapter(jpaVendorAdapter);
        em.setPackagesToScan(JPA_ENTITIES_PACKAGE_SCAN);
        em.setPersistenceProvider(new HibernatePersistenceProvider());
        em.setJpaProperties(additionalProperties());
        em.setSharedCacheMode(SharedCacheMode.ENABLE_SELECTIVE);
        return em;
    }

    @Primary
    @Bean("configsTransactionManager")
    @ConditionalOnProperty(value = "qip.datasource.configuration.enabled", havingValue = "true", matchIfMissing = true)
    public PlatformTransactionManager configsTransactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory
    ) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

    private Properties additionalProperties() {
        Properties properties = new Properties();
        if (jpaProperties != null) {
            properties.putAll(jpaProperties.getProperties());
        }
        return properties;
    }

    static class DefaultTenantDbaaSClassifierBuilder extends DbaaSChainClassifierBuilder {

        private final String tenant;
        private final DbaaSChainClassifierBuilder dbaaSClassifierBuilder;

        public DefaultTenantDbaaSClassifierBuilder(String tenant, DbaaSChainClassifierBuilder dbaaSClassifierBuilder) {
            super(null);
            this.tenant = tenant;
            this.dbaaSClassifierBuilder = dbaaSClassifierBuilder;
        }

        @Override
        public DbaasDbClassifier build() {
            TenantContext.set(tenant);
            return dbaaSClassifierBuilder.build();
        }
    }
}
