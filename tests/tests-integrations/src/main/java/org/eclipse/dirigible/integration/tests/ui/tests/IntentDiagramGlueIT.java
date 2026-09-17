/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * Browser test for the Intent Editor's "Glue &amp; Outputs" diagram, over the two constructs that
 * used to be drawn by nothing at all: {@code resolves} (the effective-dated register lookup) and
 * {@code generates} (the create-from). A model whose whole automation lives in those two rendered
 * an ER diagram and nothing else, so the picture said the model did almost nothing - and the defect
 * class the picture exists to expose (two appending rules on one transition) stayed invisible.
 *
 * <p>
 * The fixture is written straight into the workspace rather than cloned from a sample repository:
 * the assertions are about the renderer, so the intent that feeds it belongs next to them, and the
 * test needs no external repository to be merged first.
 *
 * <p>
 * Only a browser can catch this. {@code intent-diagrams.js} is framework-free JavaScript rendering
 * through mxGraph, so every service behind it stays green whether or not a card is drawn.
 */
public class IntentDiagramGlueIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "intent-diagram-glue-test";
    private static final String INTENT_FILE = "app.intent";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/workspace/" + PROJECT;

    /**
     * The diagram container of the "Glue &amp; Outputs" section, so the ER section cannot satisfy an
     * assertion.
     */
    private static final String GLUE_DIAGRAM =
            "//h4[contains(@class, 'intent-section-title') and contains(text(), 'Glue')]/following-sibling::div[1]";

    /**
     * A fine identifies its driver from the vehicle-assignment register valid on the violation date,
     * then mints a declaration per identification and offers a notice on demand - one register lookup
     * and both create-from triggers (event-driven appending, and button-only) in one small model.
     */
    private static final String INTENT = """
            name: fines
            description: Traffic fines with driver identification
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Vehicle
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: plate, type: string }
              - name: Driver
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: VehicleAssignment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: validFrom, type: date }
                  - { name: validTo, type: date }
                relations:
                  - { name: vehicle, kind: manyToOne, to: Vehicle }
                  - { name: driver, kind: manyToOne, to: Driver }
              - name: Fine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                  - { name: violationAt, type: timestamp }
                  - { name: resolution, type: string, readOnly: true }
                relations:
                  - { name: vehicle, kind: manyToOne, to: Vehicle }
                  - { name: driver, kind: manyToOne, to: Driver }
                  - { name: Status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
              - name: Declaration
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Fine, kind: manyToOne, to: Fine }
              - name: Notice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Fine, kind: manyToOne, to: Fine }
            seeds:
              - name: fineStatuses
                entity: FineStatus
                rows:
                  - { id: 1, name: NEW }
                  - { id: 2, name: IDENTIFIED }
                  - { id: 3, name: UNRESOLVED }
            resolves:
              - name: identifyDriver
                event: { onCreate: Fine }
                set: driver
                from: VehicleAssignment
                match: { vehicle: vehicle }
                between: { start: validFrom, end: validTo, value: violationAt }
                outcome: resolution
                found: { setStatus: IDENTIFIED }
                notFound: { setStatus: UNRESOLVED }
                ambiguous: { setStatus: UNRESOLVED }
            generates:
              - name: declarationFromFine
                from: Fine
                to: Declaration
                event: { onTransition: Fine, when: "Status == IDENTIFIED", mode: append }
                map: { Fine: id, Note: note }
              - name: noticeFromFine
                from: Fine
                to: Notice
                label: "Create Notice"
                map: { Fine: id, Note: note }
            """;

    @Autowired
    private IRepository repository;

    @Test
    void theGlueDiagramDrawsRegisterLookupsAndCreateFroms() {
        repository.createResource(PROJECT_PATH + "/" + INTENT_FILE, INTENT.getBytes(StandardCharsets.UTF_8));

        ide.openHomePage();
        Workbench workbench = ide.openWorkbench();
        workbench.openFile(PROJECT, INTENT_FILE);

        // The frame sweep enters the editor's iframe; the Monaco source pane visible = editor loaded. It
        // is deliberately the sweep's target rather than the diagram: the sweep is a single pass, while
        // the diagram only appears after the debounced /parse round-trip, which Selenide polls for below.
        browser.findElementInAllFrames(By.cssSelector(".intent-monaco .monaco-editor"), Condition.visible);

        // The editor parsed the buffer and drew the section this test is about - its container exists only
        // once renderGlue has run, so this is the wait for the debounced /parse round-trip. (Deliberately
        // not an `//svg` step: XPath is namespace-aware, so it never matches an SVG element in an HTML
        // document - the CSS descendant selector the sibling tests use does.)
        Selenide.$(By.xpath(GLUE_DIAGRAM))
                .shouldBe(Condition.visible, Duration.ofSeconds(30));

        // The register lookup: its own card, the relation it fills, and the register it reads - which is
        // drawn as a node of its own plus the dashed `reads` edge, so the read direction is on the
        // picture. Nothing else in this section mentions the register, so both can only come from here.
        glueDiagramDrew("identifyDriver");
        glueDiagramDrew("sets driver");
        glueDiagramDrew("VehicleAssignment");
        glueDiagramDrew("reads");

        // The event-driven create-from: its target, its trigger WITH the status guard, and its
        // cardinality. The guard and the `append` badge are what make two rules on one transition
        // distinguishable at a glance - the whole reason for drawing these.
        glueDiagramDrew("declarationFromFine");
        glueDiagramDrew("\u2192 Declaration");
        glueDiagramDrew("on transition Status == 2");
        glueDiagramDrew("append");

        // ...and the create-from nobody triggers automatically says so, rather than claiming a
        // cardinality it does not have.
        glueDiagramDrew("noticeFromFine");
        glueDiagramDrew("button");
    }

    /**
     * Assert the "Glue &amp; Outputs" diagram drew a label carrying the given text.
     *
     * <p>
     * Deliberately {@code exist} rather than {@code visible}: the diagram pane scrolls, so whether a
     * given card is inside the viewport is a fact about the pane's scroll position and the window size,
     * not about the renderer this test is exercising. The section container itself is asserted visible.
     *
     * @param text the label text, matched as a substring of one text node
     */
    private void glueDiagramDrew(String text) {
        Selenide.$(By.xpath(GLUE_DIAGRAM + "//*[contains(text(), '" + text + "')]"))
                .should(Condition.exist);
    }
}
