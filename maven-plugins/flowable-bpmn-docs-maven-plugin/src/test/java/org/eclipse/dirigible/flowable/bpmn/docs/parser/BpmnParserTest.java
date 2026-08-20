package org.eclipse.dirigible.flowable.bpmn.docs.parser;

import org.eclipse.dirigible.flowable.bpmn.docs.model.ProcessDoc;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class BpmnParserTest {

    @Test
    void parseBpmn() {

        File file = loadTestFile();
        ProcessDoc processDoc = BpmnParser.parseBpmn(file);

        assertThat(processDoc).isNotNull();
    }

    private File loadTestFile() {
        String filePath = BpmnParserTest.class.getResource("/loan-payoff-process.bpmn")
                                              .getFile();
        return new File(filePath);
    }
}
