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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.PermissionSupport;
import org.eclipse.dirigible.components.intent.model.ActionIntent;
import org.eclipse.dirigible.components.intent.model.ActionPromptIntent;
import org.eclipse.dirigible.components.intent.model.ActionPromptOptionIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * An action that calls the application's own endpoint (issue #8090): the request, its dialog, the
 * advisory from-status guard and the permission token that decides who is offered the button.
 */
class CallActionIntentTest {

    static final String YAML = """
            name: helpdesk
            entities:
              - name: TicketStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Ticket
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string }
                relations:
                  - { name: Status, kind: manyToOne, to: TicketStatus, function: EntityStatus, init: 1 }
              - name: Note
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string }
            actions:
              - name: EscalateTicket
                forEntity: Ticket
                label: Escalate
                icon: arrow-up
                fromStatus: [OPEN, ON_HOLD]
                can: Ticket:escalate
                prompt:
                  - { name: level, type: integer, label: Level, required: true }
                  - { name: note,  type: text,    label: Note }
                call:
                  method: POST
                  path: /services/java/helpdesk/custom/api/TicketApi/tickets/{id}/escalations
                  body: { kind: MANUAL, level: "{level}", note: "{note}", reason: "escalated: {note}" }
                success: Escalation requested
            permissions:
              - role: SupportLead
                can: [Ticket:read, Ticket:escalate]
            seeds:
              - name: ticket-statuses
                entity: TicketStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: ON_HOLD }
                  - { id: 3, name: CLOSED }
            """;

    @Test
    void aCallActionParses() {
        ActionIntent action = IntentParser.parse(YAML)
                                          .getActions()
                                          .get(0);

        assertEquals("POST", action.getCall()
                                   .getMethod());
        assertEquals("/services/java/helpdesk/custom/api/TicketApi/tickets/{id}/escalations", action.getCall()
                                                                                                    .getPath());
        assertEquals(Map.of("kind", "MANUAL", "level", "{level}", "note", "{note}", "reason", "escalated: {note}"), action.getCall()
                                                                                                                          .getBody());
        assertEquals(List.of("level", "note"), action.getPrompt()
                                                     .stream()
                                                     .map(p -> p.getName())
                                                     .toList());
        assertEquals("Ticket:escalate", action.getCan());
        assertEquals("Escalation requested", action.getSuccess());
    }

    @Test
    void theMethodDefaultsToPost() {
        ActionIntent action = IntentParser.parse(YAML.replace("      method: POST\n", ""))
                                          .getActions()
                                          .get(0);

        assertEquals("POST", action.getCall()
                                   .getMethod());
    }

    @Test
    void anUnknownMethodIsRefused() {
        assertIssue(YAML.replace("method: POST", "method: PATCH"), "call method [PATCH] is not one of [DELETE, POST, PUT]");
    }

    @Test
    void callAndPageAreExclusive() {
        assertIssue(YAML.replace("    success: Escalation requested", "    success: Escalation requested\n    page: /services/web/x.html"),
                "declares both page and call");
    }

    @Test
    void anUnknownPathPlaceholderIsRefused() {
        assertIssue(YAML.replace("tickets/{id}/escalations", "tickets/{ticketId}/escalations"),
                "call path names [{ticketId}] - {id} (the clicked record's key) is the only placeholder");
    }

    @Test
    void aPathOutsideServicesIsRefused() {
        assertIssue(YAML.replace("path: /services/java/helpdesk", "path: https://evil.example/java/helpdesk"),
                "must be an absolute same-origin path under /services/");
    }

    @Test
    void aBodyPlaceholderMustBeAPrompt() {
        assertIssue(YAML.replace("kind: MANUAL", "kind: \"{kind}\""), "call body names [{kind}], which is not a prompt input");
    }

    @Test
    void aPathIdNeedsEntityScope() {
        assertIssue(YAML.replace("    fromStatus: [OPEN, ON_HOLD]\n", "    scope: page\n"),
                "call path names {id} but the action has scope [page]");
    }

    @Test
    void fromStatusNamesAreResolved() {
        assertEquals(List.of(1, 2), IntentParser.parse(YAML)
                                                .getActions()
                                                .get(0)
                                                .getFromStatus());
        assertIssue(YAML.replace("[OPEN, ON_HOLD]", "[OPEN, PENDING]"), "names [PENDING], which is not a seeded status of [TicketStatus]");
    }

    @Test
    void fromStatusOnAnEntityWithoutStatusIsRefused() {
        String onNote = YAML.replace("forEntity: Ticket", "forEntity: Note");
        assertIssue(onNote, "action [EscalateTicket] fromStatus names the status [OPEN]");
        assertIssue(onNote.replace("[OPEN, ON_HOLD]", "[1, 2]"), "declares fromStatus but [Note] has no `function: EntityStatus` relation");
    }

    @Test
    void aTokenNoRoleListsIsAnIssue() {
        assertIssue(YAML.replace("can: Ticket:escalate", "can: Ticket:escalte"),
                "can [Ticket:escalte] is listed by no role under permissions - the button would be shown to nobody");
    }

    @Test
    void aPromptInputNeedsAKnownType() {
        assertIssue(YAML.replace("type: integer", "type: number"), "prompt input [level] has type [number]");
    }

    @Test
    void promptAndConfirmAreExclusive() {
        assertIssue(YAML.replace("    success: Escalation requested", "    success: Escalation requested\n    confirm: Sure?"),
                "declares both prompt and confirm");
    }

    @Test
    void aPageActionRefusesTheKeysOnlyACallUses() {
        String page = """
                name: helpdesk
                entities:
                  - name: Note
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                actions:
                  - { name: OpenNote, forEntity: Note, page: /services/web/helpdesk/note.html, confirm: "Sure?", success: Done }
                """;
        assertIssue(page, "action [OpenNote] declares [confirm, success], which only a call: action uses");
    }

    @Test
    void aTokenBoundByAnActionIsNoAdvisory() {
        IntentModel model = IntentParser.parse(YAML);

        assertTrue(PermissionSupport.gates(model)
                                    .advisories()
                                    .isEmpty(),
                "Ticket:escalate gates the Escalate button: " + PermissionSupport.gates(model)
                                                                                 .advisories());
        assertTrue(PermissionSupport
                                    .gates(IntentParser.parse(YAML.replace("can: [Ticket:read, Ticket:escalate]",
                                            "can: [Ticket:read, Ticket:escalate, Ticket:close]")))
                                    .advisories()
                                    .stream()
                                    .anyMatch(advisory -> advisory.contains("Ticket:close")),
                "a token no action binds is still reported");
    }

    /** {@code primary: true} (#8119): opt-in, and kept on the model for the generators. */
    @Test
    void aPrimaryEntityActionParses() {
        assertTrue(IntentParser
                               .parse(YAML.replace("    success: Escalation requested",
                                       "    success: Escalation requested\n    primary: true"))
                               .getActions()
                               .get(0)
                               .isPrimary());
        assertFalse(IntentParser.parse(YAML)
                                .getActions()
                                .get(0)
                                .isPrimary(),
                "primary is opt-in");
    }

    /** The filled button is a record's, so a page-scope action cannot be primary (#8119). */
    @Test
    void aPrimaryPageActionIsRefused() {
        assertIssue(
                YAML.replace("    success: Escalation requested", "    success: Escalation requested\n    primary: true\n    scope: page"),
                "declares primary but has scope [page]");
    }

    /**
     * {@code destructive: true} (#8364): opt-in, on a call or a page action, kept for the generators.
     */
    @Test
    void aDestructiveEntityActionParses() {
        assertTrue(IntentParser
                               .parse(YAML.replace("    success: Escalation requested",
                                       "    success: Escalation requested\n    destructive: true"))
                               .getActions()
                               .get(0)
                               .isDestructive());
        assertTrue(IntentParser
                               .parse(YAML.replace("    success: Escalation requested",
                                       "    success: Escalation requested\n  - { name: PurgeNotes, forEntity: Ticket, destructive: true,"
                                               + " page: /services/web/helpdesk/purge.html }"))
                               .getActions()
                               .get(1)
                               .isDestructive(),
                "a page: action may be destructive too");
        assertFalse(IntentParser.parse(YAML)
                                .getActions()
                                .get(0)
                                .isDestructive(),
                "destructive is opt-in");
    }

    /** A destructive action is never the record's filled button (#8364). */
    @Test
    void aDestructivePrimaryActionIsRefused() {
        assertIssue(
                YAML.replace("    success: Escalation requested",
                        "    success: Escalation requested\n    primary: true\n    destructive: true"),
                "action [EscalateTicket] declares destructive and primary - a destructive action is never the record's filled button");
    }

    /**
     * The destructive group is a record menu's, so a page-scope action cannot be destructive (#8364).
     */
    @Test
    void aDestructivePageScopeActionIsRefused() {
        assertIssue(
                YAML.replace("    success: Escalation requested",
                        "    success: Escalation requested\n  - { name: PurgeAll, forEntity: Ticket, scope: page, destructive: true,"
                                + " page: /services/web/helpdesk/purge.html }"),
                "action [PurgeAll] declares destructive but has scope [page] - the destructive group is a record menu's");
    }

    /** The "Invite member" example of #8296: a dialog that says what it acts on. */
    static final String INVITE = """
            name: teams
            entities:
              - name: Team
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: founded, type: date }
                relations:
                  - { name: Lead, kind: manyToOne, to: Person }
              - name: Person
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            actions:
              - name: InviteMember
                forEntity: Team
                label: Invite member
                description: "Invite a person to {name}. They get an email with a link to the team's workspace."
                submit: Invite
                prompt:
                  - { name: email, type: email, label: Email, required: true, help: "name@company.com" }
                  - name: roles
                    type: multiselect
                    label: Roles
                    required: true
                    default: [Member]
                    options:
                      - { value: Member, help: Uses the team's workspace }
                      - { value: Lead,   help: Manages the team's members }
                call:
                  path: /services/java/teams/custom/api/TeamApi/teams/{id}/invitations
                  body: { email: "{email}", roles: "{roles}" }
                success: Invitation sent
            """;

    private static final String OPTIONS = """
                    options:
                      - { value: Member, help: Uses the team's workspace }
                      - { value: Lead,   help: Manages the team's members }
            """;

    private static final String EMAIL = "{ name: email, type: email, label: Email, required: true, help: \"name@company.com\" }";

    @Test
    void theInviteExampleParses() {
        ActionIntent action = IntentParser.parse(INVITE)
                                          .getActions()
                                          .get(0);

        assertEquals("Invite a person to {name}. They get an email with a link to the team's workspace.", action.getDescription());
        assertEquals("Invite", action.getSubmit());
        assertEquals("name@company.com", action.getPrompt()
                                               .get(0)
                                               .getHelp());
        ActionPromptIntent roles = action.getPrompt()
                                         .get(1);
        assertTrue(roles.isMultiselect());
        assertEquals(List.of("Member"), roles.getDefaultValue());
        assertEquals(List.of("Member", "Lead"), roles.getOptions()
                                                     .stream()
                                                     .map(ActionPromptOptionIntent::caption)
                                                     .toList(),
                "an option without a label is captioned by its value");
        assertEquals("Manages the team's members", roles.getOptions()
                                                        .get(1)
                                                        .getHelp());
    }

    @Test
    void aBareOptionIsItsValueAndItsLabel() {
        ActionPromptIntent roles = IntentParser.parse(INVITE.replace(OPTIONS, "        options: [Member, Lead]\n"))
                                               .getActions()
                                               .get(0)
                                               .getPrompt()
                                               .get(1);

        assertEquals(List.of("Member", "Lead"), roles.getOptions()
                                                     .stream()
                                                     .map(ActionPromptOptionIntent::getValue)
                                                     .toList());
        assertEquals("Lead", roles.getOptions()
                                  .get(1)
                                  .caption());
    }

    @Test
    void aMultiselectNeedsOptionsAndAKnownDefault() {
        assertIssue(INVITE.replace(OPTIONS, ""), "action [InviteMember] prompt input [roles] is a multiselect without options");
        assertIssue(INVITE.replace("default: [Member]", "default: [Member, Owner]"),
                "action [InviteMember] prompt input [roles] default names [Owner], which is not one of its options [Member, Lead]");
        assertIssue(INVITE.replace("value: Lead,", "value: Member,"), "prompt input [roles] lists the option [Member] twice");
        assertIssue(INVITE.replace("default: [Member]", "default: Member"),
                "prompt input [roles] default [Member] does not fit its type [multiselect] - expected a list of its option values");
    }

    @Test
    void optionsOnAnotherTypeAreRefused() {
        assertIssue(INVITE.replace("type: multiselect", "type: string")
                          .replace("        default: [Member]\n", ""),
                "action [InviteMember] prompt input [roles] declares options but has type [string] - only a multiselect chooses from options");
    }

    @Test
    void aDescriptionPlaceholderMustNameAPropertyOfTheEntity() {
        assertIssue(INVITE.replace("Invite a person to {name}.", "Invite a person to {title}."),
                "action [InviteMember] description names [{title}], which is not a field or to-one relation of [Team]");
        assertIssue(INVITE.replace("    submit: Invite\n", "    submit: Invite\n    scope: page\n")
                          .replace("teams/{id}/invitations", "invitations"),
                "action [InviteMember] description names [{name}] but the action has scope [page]");
        // A to-one relation is a property too; the confirm text is checked the same way.
        String withLead = INVITE.replace("Invite a person to {name}.", "Invite a person to {name}, led by {Lead}.");
        assertEquals("Invite", IntentParser.parse(withLead)
                                           .getActions()
                                           .get(0)
                                           .getSubmit());
        String confirm = INVITE.replace("    prompt:\n", "    confirm: Archive {nickname}?\n    prompt:\n");
        assertIssue(confirm, "action [InviteMember] confirm names [{nickname}], which is not a field or to-one relation of [Team]");
    }

    @Test
    void theNameALabelGeneratesIsAProperty() {
        String labelled = INVITE.replace("  - name: Team\n    fields:", "  - name: Team\n    label: \"{title}\"\n    fields:")
                                .replace("      - { name: name, type: string }\n      - { name: founded",
                                        "      - { name: title, type: string }\n      - { name: founded");

        assertEquals("Invite", IntentParser.parse(labelled)
                                           .getActions()
                                           .get(0)
                                           .getSubmit());
    }

    @Test
    void aDefaultMustFitItsType() {
        assertIssue(INVITE.replace("help: \"name@company.com\" }", "help: \"name@company.com\", default: 5 }"),
                "action [InviteMember] prompt input [email] default [5] does not fit its type [email] - expected a string");
        String typed = INVITE.replace(EMAIL, EMAIL
                + "\n      - { name: seats, type: integer, default: 1.5 }\n      - { name: lead, type: boolean, default: yes please }\n"
                + "      - { name: from, type: date, default: tomorrow }");
        assertIssue(typed, "prompt input [seats] default [1.5] does not fit its type [integer] - expected a whole number");
        assertIssue(typed, "prompt input [lead] default [yes please] does not fit its type [boolean] - expected true or false");
        assertIssue(typed, "prompt input [from] default [tomorrow] does not fit its type [date] - expected a date (yyyy-MM-dd)");
        assertIssue(INVITE.replace("help: \"name@company.com\" }", "help: \"name@company.com\", default: \"{nickname}\" }"),
                "action [InviteMember] prompt input [email] default names [{nickname}], which is not a field or to-one relation of [Team]");
    }

    @Test
    void aDefaultOfItsTypeOrFromTheRecordParses() {
        ActionIntent action = IntentParser.parse(INVITE.replace(EMAIL,
                "{ name: email, type: email, default: \"{name}\" }\n      - { name: seats, type: integer, default: 3 }\n"
                        + "      - { name: share, type: decimal, default: 0.5 }\n      - { name: lead, type: boolean, default: true }\n"
                        + "      - { name: from, type: date, default: 2026-11-02 }\n      - { name: since, type: date, default: \"{founded}\" }"))
                                          .getActions()
                                          .get(0);

        assertEquals(List.of("{name}", 3L, 0.5, true, "2026-11-02", "{founded}"), action.getPrompt()
                                                                                        .stream()
                                                                                        .filter(input -> !input.isMultiselect())
                                                                                        .map(ActionPromptIntent::getDefaultValue)
                                                                                        .toList(),
                "an unquoted date is kept as its ISO text");
    }

    /**
     * The "Change skills" example of #8323: a dialog that opens with the record's child rows ticked.
     */
    static final String SKILLS = """
            name: team
            entities:
              - name: Skill
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Team
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: members, kind: oneToMany, to: TeamMember }
              - name: TeamMember
                list: [name, skills.skill]
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                relations:
                  - { name: team,   kind: manyToOne, to: Team, composition: true }
                  - { name: skills, kind: oneToMany, to: MemberSkill }
              - name: MemberSkill
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: member, kind: manyToOne, to: TeamMember, composition: true }
                  - { name: skill,  kind: manyToOne, to: Skill }
            actions:
              - name: ChangeSkills
                forEntity: TeamMember
                description: "Change the skills of {name}."
                prompt:
                  - name: skills
                    type: multiselect
                    required: true
                    default: "{skills.skill}"
                    options: [Java, SQL, Go]
                call:
                  method: POST
                  path: /services/java/team/custom/api/members/{id}/skills
                  body: { skills: "{skills}" }
            """;

    @Test
    void aCollectionPathDefaultNeedsTheListedColumn() {
        assertEquals("{skills.skill}", IntentParser.parse(SKILLS)
                                                   .getActions()
                                                   .get(0)
                                                   .getPrompt()
                                                   .get(0)
                                                   .getDefaultValue());
        assertIssue(SKILLS.replace("list: [name, skills.skill]", "list: [name]"),
                "action [ChangeSkills] prompt input [skills] default names [{skills.skill}], a collection path [TeamMember] does not list"
                        + " - add it to `list:`");
        String string = SKILLS.replace("type: multiselect", "type: string")
                              .replace("        options: [Java, SQL, Go]\n", "");
        assertIssue(string, "action [ChangeSkills] prompt input [skills] default names [{skills.skill}] - a collection path default fits"
                + " a multiselect only - input [skills] has type [string]");
    }

    @Test
    void aDescriptionMayNameACollectionPath() {
        String described = SKILLS.replace("Change the skills of {name}.", "{name} has {skills.skill}.");
        assertEquals("{name} has {skills.skill}.", IntentParser.parse(described)
                                                               .getActions()
                                                               .get(0)
                                                               .getDescription());
        assertIssue(described.replace("list: [name, skills.skill]", "list: [name]"),
                "action [ChangeSkills] description names [{skills.skill}], a collection path [TeamMember] does not list");
        // A bare collection is the path list: writes for the child's label.
        String bare = SKILLS.replace("  - name: MemberSkill\n", "  - name: MemberSkill\n    label: \"{skill.name}\"\n")
                            .replace("list: [name, skills.skill]", "list: [name, skills]")
                            .replace("Change the skills of {name}.", "{name} has {skills}.")
                            .replace("default: \"{skills.skill}\"", "default: \"{skills}\"");
        assertEquals("{skills}", IntentParser.parse(bare)
                                             .getActions()
                                             .get(0)
                                             .getPrompt()
                                             .get(0)
                                             .getDefaultValue());
    }

    @Test
    void descriptionAndSubmitAreACallsOnly() {
        String page = """
                name: helpdesk
                entities:
                  - name: Note
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                actions:
                  - { name: OpenNote, forEntity: Note, page: /services/web/helpdesk/note.html, description: Opens it, submit: Open }
                """;
        assertIssue(page, "action [OpenNote] declares [description, submit], which only a call: action uses");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException thrown = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(thrown.getIssues()
                         .stream()
                         .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + thrown.getIssues());
    }
}
