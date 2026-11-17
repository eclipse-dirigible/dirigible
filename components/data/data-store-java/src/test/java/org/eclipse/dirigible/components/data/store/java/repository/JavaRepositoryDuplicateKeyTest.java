/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.sdk.db.DuplicateKeyException;
import org.eclipse.dirigible.sdk.db.TargetAgreementRule;
import org.eclipse.dirigible.sdk.db.ValidationException;
import org.eclipse.dirigible.sdk.extensions.Extensions;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * dirigible #8661: a write colliding with one of an entity's business keys is refused with the
 * key's authored message by the repository itself, whoever made the write. The key mapping used to
 * live in the generated REST controllers only, so a generate action creating its second document
 * for the same key answered the driver's text as a 500. The violations below are the driver texts
 * observed on PostgreSQL and H2 (#7098, #7138), each with the statement Hibernate appends - which
 * lists EVERY column of the table and so matches every business key the entity has.
 */
class JavaRepositoryDuplicateKeyTest {

    /** SQLSTATE 23505 - a uniqueness violation, which a primary-key collision also reports. */
    private static final String UNIQUE_VIOLATION = "23505";

    /** SQLSTATE 23503 - a foreign-key violation, a different conflict on the same column. */
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private static final String HIBERNATE_INSERT =
            " [insert into VACATIONS_PUBLIC_HOLIDAY (PUBLIC_HOLIDAY_DAY,PUBLIC_HOLIDAY_DAY_OF_NOTICE,PUBLIC_HOLIDAY_NAME,PUBLIC_HOLIDAY_COMPANY,PUBLIC_HOLIDAY_ID) values (?,?,?,?,?)]";

    private static final String POSTGRES_DUPLICATE =
            "ERROR: duplicate key value violates unique constraint \"VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_key\"\n"
                    + "  Detail: Key (\"PUBLIC_HOLIDAY_DAY\")=(2026-09-07) already exists.";

    private static final String H2_DUPLICATE = "Unique index or primary key violation: "
            + "\"CONSTRAINT_INDEX_7 ON PUBLIC.VACATIONS_PUBLIC_HOLIDAY(PUBLIC_HOLIDAY_DAY NULLS FIRST) VALUES ( /* 1 */ DATE '2026-09-07' )\"; SQL statement:\n"
            + "insert into VACATIONS_PUBLIC_HOLIDAY (PUBLIC_HOLIDAY_DAY,PUBLIC_HOLIDAY_DAY_OF_NOTICE,PUBLIC_HOLIDAY_NAME,PUBLIC_HOLIDAY_ID) values (?,?,?,?) [23505-232]";

    private static final String POSTGRES_PRIMARY_KEY =
            "ERROR: duplicate key value violates unique constraint \"vacations_public_holiday_pkey\"\n"
                    + "  Detail: Key (PUBLIC_HOLIDAY_ID)=(5) already exists.";

    private static final String POSTGRES_FOREIGN_KEY =
            "ERROR: insert or update on table \"vacations_vacation\" violates foreign key constraint "
                    + "\"VACATION_PUBLIC_HOLIDAY_DAY_FK\"\n  Detail: Key (PUBLIC_HOLIDAY_DAY)=(2026-09-07) is not present in table \"vacations_public_holiday\".";

    private static final String COMPOSITE_MESSAGE = "This day is already a holiday of the company";

    private static final String DAY_MESSAGE = "A PublicHoliday with this 'Day' already exists";

    private static final String DAY_OF_NOTICE_MESSAGE = "A PublicHoliday with this 'DayOfNotice' already exists";

    private JavaEntityStore store;

    private MockedStatic<BeanProvider> beans;

    private MockedStatic<Extensions> extensions;

    private HolidayRepository repository;

    @BeforeEach
    void setUp() {
        store = mock(JavaEntityStore.class);
        beans = mockStatic(BeanProvider.class);
        beans.when(BeanProvider::isInitialzed)
             .thenReturn(true);
        beans.when(() -> BeanProvider.getBean(JavaEntityStore.class))
             .thenReturn(store);
        extensions = mockStatic(Extensions.class);
        extensions.when(() -> Extensions.find(TargetAgreementRule.class))
                  .thenReturn(List.of());
        repository = new HolidayRepository(store);
    }

    @AfterEach
    void tearDown() {
        extensions.close();
        beans.close();
    }

    @Test
    void aSaveCollidingWithAKeyIsRefusedWithThatKeysMessage() {
        Holiday holiday = new Holiday();
        when(store.save(holiday, "holidays")).thenThrow(
                violation(POSTGRES_DUPLICATE + HIBERNATE_INSERT, POSTGRES_DUPLICATE, UNIQUE_VIOLATION));

        DuplicateKeyException refused = assertThrows(DuplicateKeyException.class, () -> repository.save(holiday, "holidays"));

        assertEquals(DAY_MESSAGE, refused.getMessage());
        assertEquals("Day", refused.getField(), "the refusal keeps the field it is about");
        assertEquals(ConstraintViolationException.class, refused.getCause()
                                                                .getClass(),
                "the violation rides as the cause, for the log");
    }

    @Test
    void anUpdateAndATargetedWriteAreRefusedTheSameWay() {
        Holiday holiday = new Holiday();
        when(store.update(holiday)).thenThrow(violation(H2_DUPLICATE, H2_DUPLICATE, UNIQUE_VIOLATION));
        when(store.updateProperties(eq(Holiday.class), eq(1), any(), anyString())).thenThrow(
                violation(POSTGRES_DUPLICATE + HIBERNATE_INSERT, POSTGRES_DUPLICATE, UNIQUE_VIOLATION));

        assertEquals(DAY_MESSAGE, assertThrows(DuplicateKeyException.class, () -> repository.update(holiday)).getMessage());
        assertEquals(DAY_MESSAGE, assertThrows(DuplicateKeyException.class,
                () -> repository.updateProperties(1, Map.of("Day", "2026-09-07"), "holidays")).getMessage());
    }

    @Test
    void theCollidedKeyIsTheOneNamedEvenWhenTheStatementListsTheOthers() {
        assertEquals(DAY_MESSAGE, answerFor(POSTGRES_DUPLICATE + HIBERNATE_INSERT, POSTGRES_DUPLICATE, UNIQUE_VIOLATION));
        String dayOfNotice =
                "ERROR: duplicate key value violates unique constraint \"VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_OF_NOTICE_key\"";
        assertEquals(DAY_OF_NOTICE_MESSAGE, answerFor(dayOfNotice + HIBERNATE_INSERT, dayOfNotice, UNIQUE_VIOLATION),
                "the longer column must not be answered with the shorter one's message");
    }

    @Test
    void theH2ViolationNamingTheIndexIsRecognisedByItsColumn() {
        assertEquals(DAY_MESSAGE, answerFor(H2_DUPLICATE, H2_DUPLICATE, UNIQUE_VIOLATION));
    }

    @Test
    void aCompositeKeyAnswersWithItsAuthoredMessage() {
        String violation = "ERROR: duplicate key value violates unique constraint \"publicholiday_company_day\"";
        assertEquals(COMPOSITE_MESSAGE, answerFor(violation + HIBERNATE_INSERT, violation, UNIQUE_VIOLATION),
                "the constraint name is matched whatever case the database folded it to");
    }

    @Test
    void aPrimaryKeyCollisionIsNotAnsweredAsABusinessKeyDuplicate() {
        RuntimeException violation = violation(POSTGRES_PRIMARY_KEY + HIBERNATE_INSERT, POSTGRES_PRIMARY_KEY, UNIQUE_VIOLATION);

        assertSame(violation, repository.duplicateKeyOr(violation), "a primary-key violation is rethrown untouched");
    }

    @Test
    void aForeignKeyViolationOnTheSameColumnIsNotADuplicate() {
        RuntimeException violation = violation(POSTGRES_FOREIGN_KEY + HIBERNATE_INSERT, POSTGRES_FOREIGN_KEY, FOREIGN_KEY_VIOLATION);

        assertFalse(POSTGRES_FOREIGN_KEY.toUpperCase(Locale.ROOT)
                                        .contains("DUPLICATE"),
                "the fixture must not be a duplicate by text either");
        assertSame(violation, repository.duplicateKeyOr(violation));
    }

    @Test
    void theSqlStateIsHonouredOnItsOwnAndIsTheOnlyOtherHandle() {
        String wordless = "constraint violation on VACATIONS_PUBLIC_HOLIDAY_PUBLIC_HOLIDAY_DAY_key";
        assertEquals(DAY_MESSAGE, answerFor(wordless, wordless, UNIQUE_VIOLATION));
        RuntimeException notADuplicate = violation(wordless, wordless, "23000");
        assertSame(notADuplicate, repository.duplicateKeyOr(notADuplicate));
    }

    @Test
    void aFailureThatIsNoConstraintViolationIsLeftAlone() {
        RuntimeException failure = new IllegalStateException("connection reset", new SQLException(POSTGRES_DUPLICATE, UNIQUE_VIOLATION));

        assertSame(failure, repository.duplicateKeyOr(failure));
    }

    @Test
    void aRefusalAlreadyMadeIsNotMadeAgain() {
        DuplicateKeyException refused = new DuplicateKeyException(new ValidationException(DAY_MESSAGE),
                violation(POSTGRES_DUPLICATE, POSTGRES_DUPLICATE, UNIQUE_VIOLATION));

        assertSame(refused, repository.duplicateKeyOr(refused));
    }

    @Test
    void anEntityWithoutBusinessKeysLeavesEveryViolationAlone() {
        JavaRepository<Holiday> plain = new JavaRepository<>(Holiday.class) {
            @Override
            protected JavaEntityStore store() {
                return store;
            }
        };
        RuntimeException violation = violation(POSTGRES_DUPLICATE, POSTGRES_DUPLICATE, UNIQUE_VIOLATION);

        assertSame(violation, plain.duplicateKeyOr(violation));
    }

    private String answerFor(String hibernateMessage, String driverMessage, String sqlState) {
        RuntimeException violation = violation(hibernateMessage, driverMessage, sqlState);
        RuntimeException answered = repository.duplicateKeyOr(violation);
        return answered == violation ? null : answered.getMessage();
    }

    private static RuntimeException violation(String hibernateMessage, String driverMessage, String sqlState) {
        return new ConstraintViolationException(hibernateMessage, new SQLException(driverMessage, sqlState), null);
    }

    static class Holiday {
    }

    /** The keys a generated repository declares: two unique columns and a composite key. */
    static class HolidayRepository extends JavaRepository<Holiday> {

        private static final Map<String, Supplier<ValidationException>> KEYS =
                Map.of("PublicHoliday_Company_Day", () -> new ValidationException(COMPOSITE_MESSAGE), "PUBLIC_HOLIDAY_DAY",
                        () -> new ValidationException(DAY_MESSAGE, null, null, "Day"), "PUBLIC_HOLIDAY_DAY_OF_NOTICE",
                        () -> new ValidationException(DAY_OF_NOTICE_MESSAGE, null, null, "DayOfNotice"));

        private final JavaEntityStore store;

        HolidayRepository(JavaEntityStore store) {
            super(Holiday.class);
            this.store = store;
        }

        @Override
        protected JavaEntityStore store() {
            return store;
        }

        @Override
        protected Map<String, Supplier<ValidationException>> duplicateKeys() {
            return KEYS;
        }
    }
}
