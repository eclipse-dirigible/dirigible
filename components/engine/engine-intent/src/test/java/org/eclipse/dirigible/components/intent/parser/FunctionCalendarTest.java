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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * {@code function: Calendar} - the presentation-role alias for {@code view: calendar}: previously a
 * reserved value rejected with "not yet available", now first-class since the calendar template
 * exists. Same rendering, same required {@code calendar:} block; other reserved roles stay gated.
 */
class FunctionCalendarTest {

    private static String entity(String extra) {
        return """
                name: bookings
                entities:
                  - name: Appointment
                %s
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: subject, type: string }
                      - { name: startsAt, type: timestamp }
                """.formatted(extra);
    }

    @Test
    void functionCalendarSelectsTheCalendarRendering() {
        IntentModel model = IntentParser.parse(entity("""
                    function: Calendar
                    calendar: { start: startsAt, title: subject }
                """));
        assertTrue(model.getEntities()
                        .get(0)
                        .isCalendar(),
                "function: Calendar must select the calendar renderer like view: calendar");
    }

    @Test
    void functionCalendarStillRequiresTheCalendarBlock() {
        IntentValidationException error =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(entity("    function: Calendar")));
        assertTrue(error.getMessage()
                        .contains("requires calendar.start"),
                error.getMessage());
    }

    @Test
    void functionCalendarConflictingWithAnotherViewIsRejected() {
        IntentValidationException error = assertThrows(IntentValidationException.class, () -> IntentParser.parse(entity("""
                    function: Calendar
                    view: slots
                    slots: { start: startsAt }
                """)));
        assertTrue(error.getMessage()
                        .contains("the role implies view: calendar"),
                error.getMessage());
    }

    @Test
    void otherReservedRolesStayGated() {
        IntentValidationException error =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(entity("    function: Board")));
        assertTrue(error.getMessage()
                        .contains("reserved for an upcoming template"),
                error.getMessage());
    }

    /** The generated repository reads the slots window (#8097), so one it cannot read is refused. */
    @Test
    void aSlotsWindowTheServerCannotReadIsRejected() {
        IntentValidationException error = assertThrows(IntentValidationException.class, () -> IntentParser.parse(entity("""
                    view: slots
                    slots: { start: startsAt, open: "9am", step: 0, resource: subject }
                """)));
        assertTrue(error.getMessage()
                        .contains("slots time [9am] is not HH:MM"),
                error.getMessage());
        assertTrue(error.getMessage()
                        .contains("slots.step [0] must be at least 1 minute"),
                error.getMessage());
        assertTrue(error.getMessage()
                        .contains("slots.resource [subject] is not a manyToOne/oneToOne relation"),
                error.getMessage());
    }
}
