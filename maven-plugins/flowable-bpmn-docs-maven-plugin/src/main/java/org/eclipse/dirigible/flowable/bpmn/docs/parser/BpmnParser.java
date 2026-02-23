package org.eclipse.dirigible.flowable.bpmn.docs.parser;

import org.eclipse.dirigible.flowable.bpmn.docs.model.ProcessDoc;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.common.engine.impl.util.io.BytesStreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class BpmnParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(BpmnParser.class);

    public static ProcessDoc parseBpmn(File file) {
        BpmnModel bpmnModel = parseToBpmnModel(file);
        return parseBpmn(bpmnModel);
    }

    public static BpmnModel parseToBpmnModel(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            BpmnXMLConverter converter = new BpmnXMLConverter();
            InputStreamProvider isp = new BytesStreamSource(Files.readAllBytes(file.toPath()));
            // false = do not validate schema, false = do not set location info
            return converter.convertToBpmnModel(isp, false, false);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load bpmn file " + file, ex);
        }
    }

    public static ProcessDoc parseBpmn(BpmnModel model) {
        List<Process> processes = model.getProcesses();
        LOGGER.info("Found [{}] processes", processes.size());
        if (processes.isEmpty()) {
            throw new IllegalStateException("No processes found in model " + model);
        }
        ProcessDoc doc = new ProcessDoc();
        Process process = processes.get(0); // parse first process

        LOGGER.info("Parsing process {}", process);
        doc.setProcessId(process.getId());
        doc.setName(process.getName());

        for (FlowElement element : process.getFlowElements()) {
            String type = element.getClass()
                                 .getSimpleName();
            String id = element.getId();
            String name = element.getName();

            // Handle ServiceTask specifically
            if (element instanceof UserTask) {
                doc.addElement(id, type, name, null);
            } else if (element instanceof ServiceTask serviceTask) {
                String implType = serviceTask.getImplementationType();
                String impl = serviceTask.getImplementation();

                // delegateExpression, class, or expression
                if ("delegateExpression".equals(implType)) {
                    doc.addElement(id, type, name, impl); // store delegateExpression
                } else if ("class".equals(implType)) {
                    doc.addElement(id, type, name, impl); // store Java class
                } else if ("expression".equals(implType)) {
                    doc.addElement(id, type, name, impl); // store expression
                }
            } else {
                // Generic handling for other elements
                doc.addElement(id, type, name, null);
            }
        }
        return doc;
    }
}
