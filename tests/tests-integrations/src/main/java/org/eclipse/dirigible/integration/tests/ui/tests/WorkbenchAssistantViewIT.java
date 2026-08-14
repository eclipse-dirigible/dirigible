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
import java.util.List;

import org.eclipse.dirigible.components.intent.conversation.ConversationRole;
import org.eclipse.dirigible.components.intent.conversation.ConversationSurface;
import org.eclipse.dirigible.components.intent.conversation.IntentConversationService;
import org.eclipse.dirigible.components.intent.conversation.IntentConversationService.ConversationKey;
import org.eclipse.dirigible.components.intent.conversation.IntentConversationService.MessageDraft;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.Workbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

import org.openqa.selenium.By;

/**
 * Browser test for the Workbench's Assistant view. The HTTP-only {@code IntentEngineIT} covers the
 * assist endpoint, but it cannot catch the two failures that only exist in a browser: the AngularJS
 * module failing to bootstrap inside the view iframe (a missing platform-links category or an
 * unresolvable dependency leaves the services green and the pane dead), and the view never learning
 * which file it is helping with - it tracks the open editor over the layout hub, so a wrong or
 * missing topic shows up as a permanently empty pane and nowhere else.
 *
 * <p>
 * No AI is involved: the conversation is seeded through the service <em>before</em> the view is
 * ever opened, so a bubble carrying it can only have been restored from the server.
 */
public class WorkbenchAssistantViewIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "assistant-view-test";
    private static final String FOLDER = "custom";
    private static final String FILE = "OrderNumber.java";
    private static final String FILE_PATH = FOLDER + "/" + FILE;
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/workspace/" + PROJECT;

    /** Seeded into the stored conversation, so the chat pane can only show it by restoring it. */
    private static final String RESTORED_MESSAGE = "generate the number from the company prefix";

    private static final String SOURCE = """
            package custom;

            public class OrderNumber {
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private IntentConversationService conversationService;

    @Test
    void assistantView_tracks_the_open_java_file_and_restores_its_conversation() {
        repository.createResource(PROJECT_PATH + "/" + FILE_PATH, SOURCE.getBytes(StandardCharsets.UTF_8));
        conversationService.append(new ConversationKey(PROJECT, ConversationSurface.WORKBENCH, FILE_PATH),
                List.of(new MessageDraft(ConversationRole.USER, RESTORED_MESSAGE)));

        ide.openHomePage();
        Workbench workbench = ide.openWorkbench();

        // The assistant is the Workbench's right-region view and opens expanded, so it is already
        // listening when the file below is opened - the flow a developer with the pane open has.
        browser.findElementInAllFrames(By.cssSelector(".as-pane"), Condition.visible);

        Object bootstrapped = Selenide.executeJavaScript(
                "var el = document.querySelector('[ng-app=\"assistant\"]');" + "return !!(el && angular.element(el).injector());");
        Assertions.assertTrue(Boolean.TRUE.equals(bootstrapped), "assistant AngularJS module failed to bootstrap inside the view iframe.");

        Selenide.switchTo()
                .defaultContent();
        workbench.expandProject(PROJECT);
        workbench.openFile(FOLDER);
        workbench.openFile(FILE);

        // The view found the file it is helping with, and brought back what was said about it before.
        browser.findElementInAllFrames(By.cssSelector(".as-target-file"), Condition.visible);
        Selenide.$(By.xpath("//span[contains(@class, 'as-target-file') and contains(text(), '" + FILE_PATH + "')]"))
                .shouldBe(Condition.visible);
        Selenide.$(By.xpath("//div[contains(@class, 'as-msg') and contains(text(), '" + RESTORED_MESSAGE + "')]"))
                .shouldBe(Condition.visible);
    }
}
