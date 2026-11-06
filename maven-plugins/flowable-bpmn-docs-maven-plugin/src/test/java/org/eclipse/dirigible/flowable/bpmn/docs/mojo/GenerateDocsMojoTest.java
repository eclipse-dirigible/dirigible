package org.eclipse.dirigible.flowable.bpmn.docs.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class GenerateDocsMojoTest {

    @Test
    void execute() throws MojoExecutionException, URISyntaxException {
        URL resource = getClass().getClassLoader()
                                 .getResource(".");
        File resourceFolder = new File(resource.toURI());

        File bpmnDirectory = resourceFolder;
        File outputDirectory = resourceFolder;
        String geminiApiKey = ""; // not configured

        GenerateDocsMojo mojo = new GenerateDocsMojo(bpmnDirectory, outputDirectory, geminiApiKey);

        mojo.execute();

        assertResourceFileGenerated("loan-payoff-generated.md");
        assertResourceFileGenerated("loan-payoff-generated.pdf");
    }

    private void assertResourceFileGenerated(String fileName) throws URISyntaxException {
        URL resource = getClass().getClassLoader()
                                 .getResource(fileName);
        assertThat(resource).describedAs("Missing resource " + fileName)
                            .isNotNull();

        File resourceFile = new File(resource.toURI());
        assertThat(resourceFile).exists();
    }
}
