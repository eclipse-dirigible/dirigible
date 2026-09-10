package org.eclipse.dirigible.flowable.bpmn.docs.export;

import org.eclipse.dirigible.flowable.bpmn.docs.model.ElementDoc;
import org.eclipse.dirigible.flowable.bpmn.docs.model.ProcessDoc;
import org.eclipse.dirigible.flowable.bpmn.docs.model.SequenceDoc;

import java.io.IOException;

public class MarkdownExporter {

    public static String export(ProcessDoc doc) throws IOException {

        StringBuilder md = new StringBuilder();
        md.append("# ")
          .append(doc.getName())
          .append("\n\n");
        md.append("![Diagram](")
          .append(doc.getProcessId())
          .append(".jpg)\n\n");

        md.append("## Elements\n");
        for (ElementDoc e : doc.getElements()) {
            md.append("### ")
              .append(e.getType())
              .append(": ")
              .append(e.getName())
              .append("\n");
            e.getMetadata()
             .forEach((k, v) -> md.append("- ")
                                  .append(k)
                                  .append(": ")
                                  .append(v)
                                  .append("\n"));
            md.append("\n");
        }

        md.append("## Sequence Flows\n");
        for (SequenceDoc f : doc.getFlows()) {
            md.append("- ")
              .append(f.source())
              .append(" → ")
              .append(f.target())
              .append("\n");
        }

        return md.toString();
    }
}
