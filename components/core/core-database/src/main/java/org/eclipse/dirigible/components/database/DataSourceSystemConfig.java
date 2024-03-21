/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible
 * contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database;

import java.util.Properties;

import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;

import org.eclipse.dirigible.commons.config.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

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
    @Bean(name = "SystemDB")
    public HikariDataSource getDataSource() {
        DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setName("SystemDB");
        dataSourceProperties.setDriverClassName(Configuration.get("DIRIGIBLE_DATABASE_SYSTEM_DRIVER", "org.h2.Driver"));
        dataSourceProperties.setUrl(Configuration.get("DIRIGIBLE_DATABASE_SYSTEM_URL", "jdbc:h2:file:./target/dirigible/h2/SystemDB"));
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
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("SystemDB") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        String[] packages = dirigibleScanPackages.split(",");
        em.setPackagesToScan(packages);

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        em.setJpaProperties(properties);

        return em;
    }

    /**
     * Transaction manager.
     *
     * @param entityManagerFactory the entity manager factory
     * @return the platform transaction manager
     */
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

}
