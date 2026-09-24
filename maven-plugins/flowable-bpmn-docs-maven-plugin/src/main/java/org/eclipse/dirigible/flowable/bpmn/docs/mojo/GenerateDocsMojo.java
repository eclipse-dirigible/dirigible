package org.eclipse.dirigible.flowable.bpmn.docs.mojo;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.dirigible.flowable.bpmn.docs.ai.GeminiBpmnDocGenerator;
import org.eclipse.dirigible.flowable.bpmn.docs.diagram.DiagramRenderer;
import org.eclipse.dirigible.flowable.bpmn.docs.export.MarkdownExporter;
import org.eclipse.dirigible.flowable.bpmn.docs.export.PdfExporter;
import org.eclipse.dirigible.flowable.bpmn.docs.model.ProcessDoc;
import org.eclipse.dirigible.flowable.bpmn.docs.parser.BpmnParser;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GenerateDocsMojo extends AbstractMojo {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateDocsMojo.class);

    @Parameter(property = "bpmn.dir", defaultValue = "${project.basedir}/src/main/resources")
    private File bpmnDirectory;

    @Parameter(property = "bpmn.output", defaultValue = "${project.build.directory}/bpmn-docs")
    private File outputDirectory;

    @Parameter(property = "gemini.api.key", defaultValue = "")
    private String geminiApiKey;

    public GenerateDocsMojo() {

    }

    public GenerateDocsMojo(File bpmnDirectory, File outputDirectory, String geminiApiKey) {
        this.bpmnDirectory = bpmnDirectory;
        this.outputDirectory = outputDirectory;
        this.geminiApiKey = geminiApiKey;
    }

    @Override
    public void execute() throws MojoExecutionException {
        LOGGER.info("Output dir {}", outputDirectory);
        LOGGER.info("BPMN dir {}", bpmnDirectory);
        LOGGER.info("Gemini API key is blank: {}", StringUtils.isBlank(geminiApiKey));

        outputDirectory.mkdirs();

        List<File> bpmnFiles = findBpmnFiles();

        for (File bpmnFile : bpmnFiles) {
            documentBpmnFile(bpmnFile);
        }
    }

    private @NonNull List<File> findBpmnFiles() {
        LOGGER.info("Checking dir [{}] for BPMN files...", bpmnDirectory);
        try (Stream<Path> paths = Files.walk(bpmnDirectory.toPath())) {
            List<File> files = paths.filter(Files::isRegularFile)
                                    .filter(p -> p.toString()
                                                  .endsWith(".bpmn")
                                            || p.toString()
                                                .endsWith(".bpmn20.xml"))
                                    .map(Path::toFile)
                                    .collect(Collectors.toList());

            LOGGER.info("Found {} BPMN files: {}", files.size(), files);
            return files;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to check for BPM files in " + bpmnDirectory, ex);
        }
    }

    private void documentBpmnFile(File bpmnFile) throws MojoExecutionException {
        LOGGER.info("Documenting BPMN file {}", bpmnFile);
        try {
            ProcessDoc doc = BpmnParser.parseBpmn(bpmnFile);
            // generate process diagram
            // also, the generated image is used in the markdown
            DiagramRenderer.renderProcess(bpmnFile, doc, outputDirectory);

            generateProcessDocs(doc);
            generateAIProcessDocs(bpmnFile, doc);
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to generate BPM doc for bpmnFile " + bpmnFile, ex);
        }
    }

    private void generateProcessDocs(ProcessDoc doc) throws IOException {
        String generatedMarkdown = MarkdownExporter.export(doc);
        Path generatedMarkdownPath = writeToFile(outputDirectory, doc.getProcessId() + "-generated.md", generatedMarkdown);

        PdfExporter.export(generatedMarkdownPath.toFile(), outputDirectory, doc.getProcessId() + "-generated.pdf");
    }

    private Path writeToFile(File outDir, String fileName, String content) {
        Path outFilePath = outDir.toPath()
                                 .resolve(fileName);
        try {
            Files.writeString(outFilePath, content);
            LOGGER.info("Saved file {}", outFilePath);
            return outFilePath;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write content to " + outFilePath, ex);
        }
    }

    private void generateAIProcessDocs(File file, ProcessDoc doc) throws Exception {
        if (StringUtils.isNotBlank(geminiApiKey)) {
            LOGGER.info("Generating gemini doc for file [{}]", file);
            GeminiBpmnDocGenerator geminiBpmnDocGenerator = new GeminiBpmnDocGenerator(geminiApiKey);

            Path processImage = DiagramRenderer.renderProcess(file, doc, outputDirectory);
            String aiMarkdown = geminiBpmnDocGenerator.generateBpmnDoc(file.toPath(), processImage);
            Path aiMarkdownPath = writeToFile(outputDirectory, doc.getProcessId() + "-ai.md", aiMarkdown);

            PdfExporter.export(aiMarkdownPath, outputDirectory, doc.getProcessId() + "-ai.pdf");
        } else {
            LOGGER.info("Skipping generating gemini doc for file [{}] since API key is not configured", file);
        }
    }

}
