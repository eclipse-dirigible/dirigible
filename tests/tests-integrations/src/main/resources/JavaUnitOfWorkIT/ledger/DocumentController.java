/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package ledger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.data.store.java.repository.Criteria;
import org.eclipse.dirigible.components.data.store.java.repository.UnitOfWork;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.sdk.http.PathParam;

/**
 * The write-then-aggregate pattern every generated document repository runs: a line save re-sums the
 * header's total from a query over its lines and persists the sum through the targeted
 * {@code updateProperties} mutation. Inside a unit of work all of it shares one session, so the query
 * must see the lines the block just wrote and the mutation must land on the header the block just
 * inserted - or the document commits with lines and a total of zero (issue #7096).
 */
@Controller
public class DocumentController {

    private final DocumentRepository documents;

    private final LineRepository lines;

    private final EntryRepository entries;

    public DocumentController(DocumentRepository documents, LineRepository lines, EntryRepository entries) {
        this.documents = documents;
        this.lines = lines;
        this.entries = entries;
    }

    /** Header + two lines + a re-sum after each, as one unit; reports what the block itself observed. */
    @Get("/document/unit/{tag}")
    public String documentInsideUnit(@PathParam("tag") String tag) {
        return UnitOfWork.call(() -> {
            Document header = new Document();
            header.name = tag;
            header.total = BigDecimal.ZERO;
            Document saved = documents.save(header);
            lines.save(line(saved.id, new BigDecimal("5.00")));
            recalculate(saved.id);
            lines.save(line(saved.id, new BigDecimal("7.00")));
            Recalculated last = recalculate(saved.id);
            return "id=" + saved.id + " header=" + last.headerFound + " lines=" + last.lines + " updated=" + last.updated;
        });
    }

    /**
     * The same document, written the way a generated create-from writes it: a targeted flip on another
     * entity first (the source's status), then header and lines each recording a create event in the
     * outbox, then the re-sum after every line.
     */
    @Get("/document/unit/events/{tag}")
    public String documentInsideUnitWithEvents(@PathParam("tag") String tag) {
        return UnitOfWork.call(() -> {
            Entry source = new Entry();
            source.name = tag;
            source.amount = 1;
            Entry flipped = entries.save(source, "uow-source");
            entries.updateProperty(flipped.id, "amount", 2);
            Document header = new Document();
            header.name = tag;
            header.total = new BigDecimal("0.00");
            Document saved = documents.save(header, "uow-document");
            lines.save(line(saved.id, new BigDecimal("5.00")), "uow-line");
            recalculate(saved.id);
            lines.save(line(saved.id, new BigDecimal("7.00")), "uow-line");
            Recalculated last = recalculate(saved.id);
            return "id=" + saved.id + " header=" + last.headerFound + " lines=" + last.lines + " updated=" + last.updated;
        });
    }

    /** The committed total, read outside any unit - what the browser sees after the create-from. */
    @Get("/document/{id}/total")
    public String committedTotal(@PathParam("id") Integer id) {
        Document document = documents.findById(id);
        if (document == null) {
            return "missing";
        }
        return document.total == null ? "null"
                : document.total.stripTrailingZeros()
                                .toPlainString();
    }

    private Recalculated recalculate(Integer documentId) {
        // Reload the header created earlier in this same unit (read-your-own-writes), sum the lines the
        // unit just wrote (a query by foreign key), and persist the sum through the targeted mutation -
        // the exact shape a generated document repository runs on every line change.
        Document header = documents.findById(documentId);
        List<Line> current = lines.findAll(Criteria.create()
                                                   .eq("document", documentId));
        BigDecimal sum = BigDecimal.ZERO;
        for (Line line : current) {
            if (line.amount != null) {
                sum = sum.add(line.amount);
            }
        }
        int updated = documents.updateProperties(documentId, Map.of("total", sum));
        return new Recalculated(header != null, current.size(), updated);
    }

    private record Recalculated(boolean headerFound, int lines, int updated) {}

    private static Line line(Integer document, BigDecimal amount) {
        Line line = new Line();
        line.document = document;
        line.amount = amount;
        return line;
    }
}
