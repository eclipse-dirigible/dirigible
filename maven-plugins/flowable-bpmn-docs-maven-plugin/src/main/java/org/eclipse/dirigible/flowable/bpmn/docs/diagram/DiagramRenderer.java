package org.eclipse.dirigible.flowable.bpmn.docs.diagram;

import org.eclipse.dirigible.flowable.bpmn.docs.model.ProcessDoc;
import org.eclipse.dirigible.flowable.bpmn.docs.parser.BpmnParser;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class DiagramRenderer {

    public static Path renderProcess(File bpmnFile, ProcessDoc doc, File outDir) throws Exception {

        BpmnModel model = BpmnParser.parseToBpmnModel(bpmnFile);

        DefaultProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();

        try (InputStream jpgInputStream = diagramGenerator.generateJpgDiagram(model)) {
            Path imagePath = outDir.toPath()
                                   .resolve(doc.getProcessId() + ".jpg");
            Files.copy(jpgInputStream, imagePath, StandardCopyOption.REPLACE_EXISTING);
            return imagePath;
        }
    }
}
