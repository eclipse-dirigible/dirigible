package org.eclipse.dirigible.flowable.bpmn.docs.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public class PdfExporter {

    private static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: sans-serif; font-size: 12pt; line-height: 1.5; }
                    img {
                        max-width: 100%%; /* Double the percent sign here */
                        height: auto;
                        display: block;
                        margin: 20px 0;
                    }
                    table {
                        border-collapse: collapse;
                        width: 100%%; /* Double the percent sign here */
                        margin-bottom: 20px;
                    }
                    th, td {
                        border: 1px solid #ccc;
                        padding: 8px;
                        text-align: left;
                    }
                </style>
            </head>
            <body>
                %s
            </body>
            </html>
            """;
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfExporter.class);

    public static void export(Path mdFile, File outDir, String fileName) throws IOException {
        export(mdFile.toFile(), outDir, fileName);
    }

    public static void export(File mdFile, File outDir, String fileName) throws IOException {
        File pdfFile = new File(outDir, fileName);

        MutableDataSet options = new MutableDataSet();
        // Add the TablesExtension here to support tables
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));

        Parser parser = Parser.builder(options)
                              .build();
        HtmlRenderer renderer = HtmlRenderer.builder(options)
                                            .build();

        String mdFileContent = FileUtils.readFileToString(mdFile, StandardCharsets.UTF_8);
        String rawHtml = renderer.render(parser.parse(mdFileContent));

        String wellFormedHtml = HTML_TEMPLATE.formatted(rawHtml);

        try (OutputStream os = new FileOutputStream(pdfFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            
            String baseDocumentUri = mdFile.getParentFile()
                                           .toURI()
                                           .toString();
            builder.withHtmlContent(wellFormedHtml, baseDocumentUri);
            builder.toStream(os);
            builder.run();
        }
        LOGGER.info("Exported file [{}] to pdfFile [{}]", mdFile, pdfFile);
    }
}
