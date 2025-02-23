package org.eclipse.dirigible.integration.tests.ui;

import io.restassured.http.ContentType;
import org.eclipse.dirigible.tests.*;
import org.eclipse.dirigible.tests.framework.Browser;
import org.eclipse.dirigible.tests.framework.BrowserFactory;
import org.eclipse.dirigible.tests.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.util.ProjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

import java.util.concurrent.TimeUnit;

@Lazy
@Component
public class JdbcTestProject {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTestProject.class);
    private static final String PROJECT_NAME = "orders-etl";
    private static final String DEFAULT_DB = "DefaultDB";

    private final BrowserFactory browserFactory;
    private final IDE ide;
    private final RestAssuredExecutor restAssuredExecutor;
    private final ProjectUtil projectUtil;

    public JdbcTestProject(BrowserFactory browserFactory, IDE ide, RestAssuredExecutor restAssuredExecutor, ProjectUtil projectUtil) {
        this.browserFactory = browserFactory;
        this.ide = ide;
        this.restAssuredExecutor = restAssuredExecutor;
        this.projectUtil = projectUtil;
    }

    public void createProject() {
        Workbench workbench = ide.openWorkbench();
        workbench.createNewProject(PROJECT_NAME);
        workbench.createFileInProject(PROJECT_NAME, "orders.table", "File");
        workbench.publishAll();

        LOGGER.info("Project '{}' created and published successfully.", PROJECT_NAME);
    }

    public void defineTables() {
        projectUtil.createFile(PROJECT_NAME + "/db/oc_order.table", """
                {
                    "name": "OC_ORDERS",
                    "type": "TABLE",
                    "columns": [
                        { "type": "INTEGER", "primaryKey": true, "identity": true, "name": "ID" },
                        { "type": "DECIMAL", "precision": "15", "scale": "4", "name": "TOTAL" },
                        { "type": "TIMESTAMP", "name": "DATEADDED" }
                    ]ад
                }
                """);

        projectUtil.createFile(PROJECT_NAME + "/db/orders.table", """
                {
                    "name": "ORDERS",
                    "type": "TABLE",
                    "columns": [
                        { "type": "INTEGER", "primaryKey": true, "identity": true, "name": "ID" },
                        { "type": "DECIMAL", "precision": "15", "scale": "4", "name": "TOTAL" },
                        { "type": "TIMESTAMP", "name": "DATEADDED" }
                    ]
                }
                """);

        publishProject();
        LOGGER.info("Database tables created successfully.");
    }

    public void createDatasource() {
        projectUtil.createFile(PROJECT_NAME + "/datasources/DefaultDB.datasource", """
                {
                    "location": "/orders-etl/datasources/DefaultDB.datasource",
                    "name": "DefaultDB",
                    "driver": "org.mariadb.jdbc.Driver",
                    "url": "jdbc:mariadb://mariadb:3306/oc_order",
                    "username": "admin",
                    "password": "admin"
                }
                """);

        publishProject();
        LOGGER.info("Datasource '{}' created and published successfully.", DEFAULT_DB);
    }

    public void verifyDataSource() {
        String query = "SELECT ID, TOTAL, DATEADDED FROM OC_ORDERS";

        await().atMost(10, TimeUnit.SECONDS)
               .untilAsserted(() -> {
                   given().contentType(ContentType.JSON)
                          .when()
                          .get("/services/sql/execute?query=" + query)
                          .then()
                          .statusCode(200)
                          .body("$.size()", greaterThanOrEqualTo(0));
               });

        LOGGER.info("Datasource verification successful.");
    }

    public void implementETL() {
        projectUtil.createFile(PROJECT_NAME + "/sync/sync-orders-jdbc.camel", """
                {
                    "trigger": {
                        "schedule": "0 * * ? * *",
                        "description": "Trigger Orders Replication"
                    },
                    "steps": [
                        { "type": "log", "message": "Starting Order Sync" },
                        { "type": "setProperty", "name": "exchangeRate", "value": "1.1" },
                        { "type": "sql", "query": "SELECT * FROM OC_ORDERS" },
                        { "type": "split", "expression": "${body}" },
                        { "type": "sql", "query": "MERGE INTO ORDERS (ID, TOTAL, DATEADDED) VALUES (:ID, :TOTAL, :DATEADDED)" },
                        { "type": "log", "message": "Order Sync Complete" }
                    ]
                }
                """);

        publishProject();
        LOGGER.info("ETL sync file created and published successfully.");
    }

    private void publishProject() {
        Workbench workbench = ide.openWorkbench();
        workbench.publishAll();
    }
}
