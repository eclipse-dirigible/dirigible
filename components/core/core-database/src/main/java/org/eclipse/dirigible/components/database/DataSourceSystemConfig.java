/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database;

import java.util.Properties;
import javax.sql.DataSource;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;

/**
 * The Class DataSourceSystemConfig.
 */
@org.springframework.context.annotation.Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager",
        basePackages = {"org.eclipse.dirigible.components"})
public class DataSourceSystemConfig {

    /** The dirigible scan packages. */
    @Value("${dirigible.scan.packages:org.eclipse.dirigible.components}")
    private String dirigibleScanPackages;

    /**
     * Gets the data source.
     *
     * @return the data source
     */
    @Primary
    @Bean(name = "SystemDB")
    public HikariDataSource getDataSource() {
        // !!! keep it in sync with
        // components/data/data-sources/src/main/resources/META-INF/dirigible/datasources/SystemDB.datasource
        DataSourceProperties dataSourceProperties = new DataSourceProperties();

        String systemDataSourceName = DirigibleConfig.SYSTEM_DATA_SOURCE_NAME.getStringValue();
        dataSourceProperties.setName(systemDataSourceName);
        dataSourceProperties.setDriverClassName(Configuration.get("DIRIGIBLE_DATABASE_SYSTEM_DRIVER", "org.h2.Driver"));
        dataSourceProperties.setUrl(
                Configuration.get("DIRIGIBLE_DATABASE_SYSTEM_URL", "jdbc:h2:file:./target/dirigible/h2/SystemDB;LOCK_TIMEOUT=10000"));
        dataSourceProperties.setUsername(Configuration.get("DIRIGIBLE_DATABASE_SYSTEM_USERNAME", "sa"));
        dataSourceProperties.setPassword(Configuration.get("DIRIGIBLE_DATABASE_SYSTEM_PASSWORD", ""));
        return dataSourceProperties.initializeDataSourceBuilder()
                                   .type(HikariDataSource.class)
                                   .build();
    }

    /**
     * Entity manager factory.
     *
     * @param dataSource the data source
     * @return the local container entity manager factory bean
     */
    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("SystemDB") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        String[] packages = dirigibleScanPackages.split(",");
        em.setPackagesToScan(packages);

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Properties properties = new Properties();
        String configuredDialect = Configuration.get("DIRIGIBLE_DATABASE_SYSTEM_DIALECT", "org.hibernate.dialect.H2Dialect");
        String configDDLAuto = Configuration.get("DIRIGIBLE_DATABASE_SYSTEM_DDL_AUTO", "update");

        properties.setProperty("hibernate.dialect", configuredDialect);
        properties.setProperty("hibernate.hbm2ddl.auto", configDDLAuto);
        em.setJpaProperties(properties);

        return em;
    }

    /**
     * Transaction manager.
     *
     * @param entityManagerFactory the entity manager factory
     * @return the platform transaction manager
     */
    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

}
