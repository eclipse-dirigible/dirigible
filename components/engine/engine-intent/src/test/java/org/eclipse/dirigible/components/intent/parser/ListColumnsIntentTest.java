/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * An entity's {@code list:} is its list tables' exact column set and order, apart from the control
 * order {@code order:} gives the form (dirigible #7614). It is validated like {@code order:}.
 */
class ListColumnsIntentTest {

    /** An invoice whose list wants a different order than its form. */
    private static String invoice(String list) {
        return """
                name: sales
                entities:
                  - name: Customer
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Invoice
                    label: "{number}"
                    list: %s
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                      - { name: date, type: date }
                      - { name: due, type: date }
                      - { name: total, type: decimal }
                    relations:
                      - { name: Customer, kind: manyToOne, to: Customer }
                      - { name: lines, kind: oneToMany, to: Customer }
                """.formatted(list);
    }

    @Test
    void aListOfFieldsAndToOneRelationsParses() {
        IntentModel model = IntentParser.parse(invoice("[Number, Date, Customer, Total, Due]"));

        assertEquals(List.of("Number", "Date", "Customer", "Total", "Due"), model.getEntities()
                                                                                 .get(1)
                                                                                 .getList());
    }

    @Test
    void namesMatchCaseInsensitivelyAndTheLabelNameIsAColumn() {
        IntentParser.parse(invoice("[number, customer, Name]"));
    }

    @Test
    void anUnknownNameIsRefused() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(invoice("[Number, Balance]")));

        assertTrue(ex.getMessage()
                     .contains("entity [Invoice] list references [Balance] which is not a field or to-one relation of the entity"),
                ex.getMessage());
    }

    /** A support ticket that wants to show who opened it and when (dirigible #8376). */
    private static String ticket(boolean audited) {
        return """
                name: helpdesk
                entities:
                  - name: SupportTicket
                    audit: %s
                    list: [subject, CreatedAt, createdby]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: subject, type: string }
                """.formatted(audited);
    }

    @Test
    void anAuditedEntityListsItsAuditColumns() {
        IntentModel model = IntentParser.parse(ticket(true));

        assertEquals(List.of("subject", "CreatedAt", "createdby"), model.getEntities()
                                                                        .get(0)
                                                                        .getList());
    }

    @Test
    void aNonAuditedEntityIsToldAboutTheFlag() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(ticket(false)));

        assertTrue(ex.getMessage()
                     .contains("entity [SupportTicket] list: [CreatedAt] is an audit column, and [SupportTicket] is not audit: true"),
                ex.getMessage());
    }

    @Test
    void aNameListedTwiceIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(invoice("[Number, number]")));

        assertTrue(ex.getMessage()
                     .contains("entity [Invoice] list names [number] more than once"),
                ex.getMessage());
    }

    @Test
    void aToManyRelationHasNoColumnAndIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(invoice("[Number, lines]")));

        assertTrue(ex.getMessage()
                     .contains("entity [Invoice] list references [lines]"),
                ex.getMessage());
    }

    /** A team member whose list shows its skills (dirigible #8295) - the issue's own model. */
    private static String team(String list, String skillLabel) {
        return """
                name: team
                entities:
                  - name: Skill
                    kind: setting
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Team
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                    relations:
                      - { name: members, kind: oneToMany, to: TeamMember }
                  - name: TeamMember
                    list: %s
                    fields:
                      - { name: id,       type: integer, primaryKey: true, generated: true }
                      - { name: email,    type: string }
                      - { name: joinedOn, type: date }
                    relations:
                      - { name: Team,   kind: manyToOne, to: Team, composition: true }
                      - { name: skills, kind: oneToMany, to: TeamMemberSkill }
                  - name: TeamMemberSkill
                %s
                    fields:
                      - { name: id,        type: integer, primaryKey: true, generated: true }
                      - { name: grantedBy, type: string }
                    relations:
                      - { name: TeamMember, kind: manyToOne, to: TeamMember, composition: true }
                      - { name: skill,      kind: manyToOne, to: Skill }
                      - { name: tags,       kind: oneToMany, to: Skill }
                """.formatted(list, skillLabel);
    }

    @Test
    void aCollectionPathIsAListColumn() {
        IntentModel model = IntentParser.parse(team("[email, skills.skill, joinedOn]", ""));

        assertEquals(List.of("email", "skills.skill", "joinedOn"), model.getEntities()
                                                                        .get(2)
                                                                        .getList());
        // a field of the child, matched case-insensitively, is a collection path too
        IntentParser.parse(team("[email, Skills.GrantedBy]", ""));
    }

    @Test
    void aBareCollectionNeedsTheChildsLabel() {
        IntentParser.parse(team("[email, skills]", "    label: \"{grantedBy}\""));

        assertRefused(team("[email, skills]", ""),
                "entity [TeamMember] list references [skills], a collection whose entity [TeamMemberSkill] declares no label:");
    }

    @Test
    void anUnknownOrDeeperPathIsRefused() {
        assertRefused(team("[email, skills.level]", ""),
                "entity [TeamMember] list references [skills.level] whose [level] is not a field or to-one relation of [TeamMemberSkill]");
        assertRefused(team("[email, talents.skill]", ""),
                "entity [TeamMember] list references [talents.skill] whose [talents] is not a oneToMany relation of the entity");
        assertRefused(team("[email, skills.skill.name]", ""),
                "entity [TeamMember] list references [skills.skill.name] - a collection path is one hop deep");
        assertRefused(team("[email, skills.tags]", ""),
                "entity [TeamMember] list references [skills.tags] whose [tags] is itself a collection of [TeamMemberSkill]");
    }

    private static void assertRefused(String yaml, String message) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));

        assertTrue(ex.getMessage()
                     .contains(message),
                ex.getMessage());
    }

    @Test
    void aBlankEntryIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(invoice("[Number, \"\"]")));

        assertTrue(ex.getMessage()
                     .contains("entity [Invoice] list has a blank entry"),
                ex.getMessage());
    }
}
