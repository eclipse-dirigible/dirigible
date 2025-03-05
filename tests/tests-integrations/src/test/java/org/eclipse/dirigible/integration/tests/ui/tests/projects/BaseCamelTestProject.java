package org.eclipse.dirigible.integration.tests.ui.tests.projects;

import ch.qos.logback.classic.Level;
import org.assertj.db.api.Assertions;
import org.assertj.db.type.AssertDbConnection;
import org.assertj.db.type.AssertDbConnectionFactory;
import org.assertj.db.type.Table;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.tests.EdmView;
import org.eclipse.dirigible.tests.IDE;
import org.eclipse.dirigible.tests.logging.LogsAsserter;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class BaseCamelTestProject extends BaseTestProject {

    @Autowired
    private DataSourcesManager dataSourcesManager;

    protected BaseCamelTestProject(String projectResourcesFolder, IDE ide, ProjectUtil projectUtil, EdmView edmView) {
        super(projectResourcesFolder, ide, projectUtil, edmView);
    }

    protected void assertLogContainsMessage(LogsAsserter logAsserter, String message, Level level) {
        await().atMost(30, TimeUnit.SECONDS)
               .pollInterval(1, TimeUnit.SECONDS)
               .until(() -> logAsserter.containsMessage(message, level));
    }

    protected void assertDatabaseETLCompletion() {
        DataSource dataSource = dataSourcesManager.getDefaultDataSource();
        AssertDbConnection connection = AssertDbConnectionFactory.of(dataSource)
                                                                 .create();

        Table ordersTable = connection.table("\"ORDERS\"")
                                      .build();

        Assertions.assertThat(ordersTable)
                  .hasNumberOfRows(2)
                  .row(0)
                  .value("ID")
                  .isEqualTo(1)
                  .value("TOTAL")
                  .isEqualTo(92)
                  .row(1)
                  .value("ID")
                  .isEqualTo(2)
                  .value("TOTAL")
                  .isEqualTo(230.46);
    }

    @Override
    public void verify() {

    }
}
