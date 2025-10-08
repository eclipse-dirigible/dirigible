package org.eclipse.dirigible;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class WorkspaceApplicationRunner implements ApplicationRunner {

    public static final String WORKSPACE_OPTION_NAME = "workspace";
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceApplicationRunner.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // allows you to pass options like java -jar dirigible-application-13.0.0-SNAPSHOT-executable.jar
        // --workspace=/some/path

        LOGGER.info("Executing...");
        if (args.containsOption(WORKSPACE_OPTION_NAME)) {
            String workspacePath = args.getOptionValues(WORKSPACE_OPTION_NAME)
                                       .get(0);
            LOGGER.info("Passed option --{} with value: [{}]", WORKSPACE_OPTION_NAME, workspacePath);
        } else {
            LOGGER.info("Missing option with name [{}]", WORKSPACE_OPTION_NAME);
        }
    }
}
