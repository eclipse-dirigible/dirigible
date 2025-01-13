## Setup
This setup instruments OTEL Collector, Jaeger, Prometheus, Loki, OpenSearch and Grafana locally.

### Profiles
Depending on  the different scenarios you want to install, you can use different Docker profiles.

__Prerequisites:__
```shell
export MAVEN_OPTS="-Xms1024m -Xmx4096m"
cd <path_to_dirigible_git_repo>
```

- Delete OpenTelemetry stack
    ```shell
    cd open-telemetry
    COMPOSE_PROFILES=dirigible-local-agent,dirigible-local-spring-starter,dirigible-latest-image-with-agent,opensearch docker compose down
    ```

- Start OpenTelemetry **default stack without Dirigible**
    ```shell
    cd open-telemetry
    docker compose up --detach
    ```

- Start OpenTelemetry **default stack + OpenSearch**
    - You may want to
      - add opensearch exporter for the logs in `otel-collector-config.yaml`
      - change the jaeger trace to opensearch logs in `grafana/provisioning/datasources/jaeger.yaml`
    - Start using 
    ```shell
    cd open-telemetry
    docker compose stop
    docker compose --profile opensearch up --detach
    ```

- Start OpenTelemetry stack with **latest Dirigible image and OpenTelemetry agent**
    ```shell
    cd open-telemetry
    docker compose stop
    docker compose --profile dirigible-latest-image-with-agent up --detach --build
    ```
  
- Start OpenTelemetry stack with **Dirigible from local sources and OpenTelemetry agent**
    ```shell
    cd <path_to_dirigible_git_repo>
  
    mvn -T 1C package -D skipTests -D maven.test.skip=true -D maven.javadoc.skip=true -D license.skip=true

    cd open-telemetry
    docker compose stop
    docker compose --profile dirigible-local-agent up --detach --build
    ```

- Start OpenTelemetry stack with **Dirigible from local sources and OpenTelemetry Spring Starter**
    ```shell
    cd <path_to_dirigible_git_repo>
  
    mvn -P open-telemetry -T 1C package -D skipTests -D maven.test.skip=true -D maven.javadoc.skip=true -D license.skip=true

    cd open-telemetry
    docker compose stop
    docker compose --profile dirigible-local-spring-starter up --detach --build
    ```
  
### Tool URLs

| Tool                  | URL                           | Use for                    |
|-----------------------|-------------------------------|----------------------------|
| Prometheus            | http://localhost:16686        | Traces                     |
| Jaeger Web UI         | http://localhost:9090         | Metrics                    |
| Loki                  | http://localhost:3100         | Log aggregation system     |
| Grafana               | http://localhost:3000/grafana | Visualize all in one       | 
| OpenSearch            | http://localhost:9200         | Search and analytics suite |
| OpenSearch Dashboards | http://localhost:5601         | OpenSearch UI              |

#### Run OpenTelemetry sample application
The application is located in [this GitHub repository](https://github.com/dirigiblelabs/sample-open-telemetry).<br>
Steps:
   - Open Dirigible UI
   - Open the git perspective and clone the project
   - Go to the workbench perspective and publish the project
   - To test traces and metrics:
      - Trigger BPM process using POST to http://localhost:8080/services/ts/open-telemetry-sample-project/bpm/process-trigger-service.ts
      - Call Camel HTTP route using GET to http://localhost:8080/services/integrations/test-camel-route
      - Check traces for cron Camel route using `SpringCronConsumer.run`
      - Script execution and HTTP Client call using GET to http://localhost:8080/services/ts/open-telemetry-sample-project/api/test-api.ts/data
      - Check traces for quartz job `defined.test-job`

## Usefull
- Export Grafana dashboards
  - GET datasources http://localhost:3000/grafana/api/datasources
  - Convert response JSON to YAML and add it to datasources folder
- Get all Prometheus metrics from http://localhost:9090/api/v1/label/__name__/values
- Get all Jaeger traces from http://localhost:16686/api/operations?service=eclipse-dirigible-agent
