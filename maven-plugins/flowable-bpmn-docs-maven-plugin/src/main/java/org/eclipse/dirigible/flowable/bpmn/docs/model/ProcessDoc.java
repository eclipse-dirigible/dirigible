package org.eclipse.dirigible.flowable.bpmn.docs.model;

import java.util.ArrayList;
import java.util.List;

public class ProcessDoc {
    private final List<ElementDoc> elements = new ArrayList<>();
    private final List<SequenceDoc> flows = new ArrayList<>();
    private String processId;
    private String name;

    public ProcessDoc() {}

    public ProcessDoc(String processId, String name) {
        this.processId = processId;
        this.name = name;
    }

    public List<ElementDoc> getElements() {
        return elements;
    }

    public List<SequenceDoc> getFlows() {
        return flows;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addElement(String id, String type, String name, Object impl) {
        ElementDoc element = new ElementDoc(id, type, name, impl);
        elements.add(element);
    }
}
