# Flowable docs maven plugin

## Usage

### Maven

- Add the following to the pom

```xml

<build>
    <plugins>
        <plugin>
            <groupId>org.eclipse.dirigible</groupId>
            <artifactId>flowable-bpmn-docs-maven-plugin</artifactId>
            <version>13.0.0-SNAPSHOT</version>
            <configuration>
                <bpmnDirectory>${project.basedir}/my-bpmns</bpmnDirectory>
                <outputDirectory>${project.build.directory}/ai-docs</outputDirectory>
                <geminiApiKey>${env.GEMINI_API_KEY}</geminiApiKey>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                    <phase>prepare-package</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

- Execute with the following

```shell
mvn clean install -Dgemini.api.key=YOUR_KEY_HERE
```

## Run the goal directly

- Using full coordinates:

```shell
mvn flowable-docs:generate \
    -Dbpmn.dir=./custom-folder \
    -Dbpmn.output=./docs-output \
    -Dgemini.api.key=YOUR_API_KEY
```

- Using the Goal Prefix (shorthand)

```shell
mvn flowable-docs:generate -Dgemini.api.key=YOUR_API_KEY
```
