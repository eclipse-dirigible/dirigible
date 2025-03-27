package org.eclipse.dirigible.components.data.sources.config;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class TransactionManagerProvider {

    private final DataSourcesManager dataSourcesManager;

    TransactionManagerProvider(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    public PlatformTransactionManager getDefaultDbTransactionManager() {
        return BeanProvider.getBean("defaultDbTransactionManager", PlatformTransactionManager.class);
    }
}
