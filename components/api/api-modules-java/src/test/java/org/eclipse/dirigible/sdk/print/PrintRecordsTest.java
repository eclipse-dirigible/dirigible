/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.sdk.db.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The hop a print payload takes past a record of another model (dirigible #7752): the record's own
 * to-one references, read from what its class declares rather than from anything the caller names.
 */
class PrintRecordsTest {

    /** A referenced record: a city, itself referencing a country. */
    public static class City {
        public Integer Id;
        public String Name;
        public LocalDate Founded;
        @Reference(repository = "org.eclipse.dirigible.sdk.print.PrintRecordsTest$CountryRepository", label = "Name")
        public Integer Country;
    }

    /** Counts its loads, so the tests can tell a read from a reuse. */
    public static class CityRepository {
        static int loads;

        public City findById(Object id) {
            loads++;
            if (!Integer.valueOf(7)
                        .equals(id)) {
                return null;
            }
            City city = new City();
            city.Id = 7;
            city.Name = "Sofia";
            city.Founded = LocalDate.of(1879, 4, 3);
            city.Country = 34;
            return city;
        }
    }

    /** Never loaded by a one-hop expansion that starts at a customer. */
    public static class CountryRepository {
        static int loads;

        public Object findById(Object id) {
            loads++;
            return null;
        }
    }

    /** The record of another model, as a feeder holds it. */
    public static class Customer {
        public static final String KIND = "customer";
        public String Name;
        @Reference(repository = "org.eclipse.dirigible.sdk.print.PrintRecordsTest$CityRepository", label = "Name")
        public Integer City;
        @Reference(repository = "org.eclipse.dirigible.sdk.print.PrintRecordsTest$CityRepository")
        public Integer BirthPlace;
        @Reference(repository = "gen.not_installed.data.settings.CurrencyRepository", label = "Code")
        public Integer Currency;
    }

    @BeforeEach
    void resetLoads() {
        CityRepository.loads = 0;
        CountryRepository.loads = 0;
    }

    @Test
    @SuppressWarnings("unchecked")
    void aDeclaredReferenceBecomesTheReferencedRecord() {
        Customer customer = new Customer();
        customer.Name = "Acme";
        customer.City = 7;
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Name", "Acme");
        node.put("City", 7);

        PrintRecords.expandReferences(customer, node, new HashMap<>());

        Map<String, Object> city = (Map<String, Object>) node.get("City");
        assertEquals("Sofia", city.get("Name"));
        assertEquals("Sofia", city.get("__label"), "a bare {{document.Customer.City}} prints the label");
        assertEquals("1879-04-03", city.get("Founded"), "a date is its ISO string, as for a record the feeder owns");
        assertEquals("Acme", node.get("Name"), "the record's own fields are untouched");
    }

    @Test
    @SuppressWarnings("unchecked")
    void theHopIsExactlyOne() {
        Customer customer = new Customer();
        customer.City = 7;
        Map<String, Object> node = new LinkedHashMap<>();

        PrintRecords.expandReferences(customer, node, new HashMap<>());

        Map<String, Object> city = (Map<String, Object>) node.get("City");
        assertEquals(34, city.get("Country"), "the referenced record's own reference stays a key");
        assertEquals(0, CountryRepository.loads, "and is never loaded - the depth is bounded by construction");
    }

    @Test
    @SuppressWarnings("unchecked")
    void aRecordReferencedManyTimesIsLoadedOnce() {
        Map<String, Map<String, Object>> loaded = new HashMap<>();
        Map<String, Object> first = new LinkedHashMap<>();
        Map<String, Object> second = new LinkedHashMap<>();
        for (Map<String, Object> node : new Map[] {first, second}) {
            Customer customer = new Customer();
            customer.City = 7;
            customer.BirthPlace = 7;
            PrintRecords.expandReferences(customer, node, loaded);
        }

        assertEquals(1, CityRepository.loads, "one read per distinct record of a payload, not one per row");
        assertEquals("Sofia", ((Map<String, Object>) second.get("City")).get("Name"));
        assertNotSame(first.get("City"), second.get("City"), "each node gets its own copy");
        assertTrue(!((Map<String, Object>) first.get("BirthPlace")).containsKey("__label"), "no declared label, no label key");
    }

    @Test
    void aMissingRecordKeepsItsKeyAndIsNotAskedForAgain() {
        Map<String, Map<String, Object>> loaded = new HashMap<>();
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("City", 99);
        for (int row = 0; row < 3; row++) {
            Customer customer = new Customer();
            customer.City = 99;
            PrintRecords.expandReferences(customer, node, loaded);
        }

        assertEquals(99, node.get("City"), "a dangling reference prints what it printed before");
        assertEquals(1, CityRepository.loads, "the miss is remembered for the payload");
    }

    @Test
    void aReferenceIntoAModelThatIsNotInstalledKeepsItsKey() {
        Customer customer = new Customer();
        customer.Currency = 3;
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("Currency", 3);

        PrintRecords.expandReferences(customer, node, new HashMap<>());

        assertEquals(3, node.get("Currency"), "an unresolvable repository costs the payload nothing");
    }

    @Test
    void anUnsetReferenceAndANullRecordAreLeftAlone() {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("City", null);

        PrintRecords.expandReferences(new Customer(), node, new HashMap<>());
        PrintRecords.expandReferences(null, node, new HashMap<>());

        assertEquals(1, node.size());
        assertEquals(0, CityRepository.loads);
    }
}
