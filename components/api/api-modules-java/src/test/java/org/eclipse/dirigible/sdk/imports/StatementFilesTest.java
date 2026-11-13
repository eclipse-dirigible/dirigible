/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The statement readers over real sample files (#7856): a windows-1251 bank CSV with a {@code ;}
 * separator and {@code 1.234,56} amounts, an MT940 file with two statements in their SWIFT
 * envelopes, a camt.053.001.08 statement, and a camt.053.001.02 statement with a batch-booked, a
 * pending and a reversal entry.
 */
class StatementFilesTest {

    private static final List<String> CSV_COLUMNS = List.of("Дата", "Референция", "Основание", "Сума");

    @Test
    void a_windows_1251_csv_with_a_comma_decimal_reads_every_value_exactly() {
        String text = ImportText.decode(sample("bank-statement-bg.csv"), "windows-1251");
        List<Map<String, String>> records = DelimitedText.read(text, ';', true, 1, CSV_COLUMNS);

        assertEquals(3, records.size(), "the blank line is skipped, the preamble line is not a record");
        ImportValues values = ImportValues.of(',', ".", "dd.MM.yyyy", null);
        assertEquals(LocalDate.of(2026, 3, 15), values.date(records.get(0)
                                                                   .get("Дата")));
        assertEquals(new BigDecimal("1234.56"), values.decimal(records.get(0)
                                                                      .get("Сума")));
        assertEquals("Плащане по фактура 1001", values.text(records.get(0)
                                                                   .get("Основание")));
        assertEquals("Такса; обслужване", records.get(1)
                                                 .get("Основание"),
                "a quoted field keeps the separator it holds");
        assertEquals(new BigDecimal("-12.50"), values.decimal(records.get(1)
                                                                     .get("Сума")));
        assertEquals(new BigDecimal("1000000.01"), values.decimal(records.get(2)
                                                                         .get("Сума")));
    }

    @Test
    void the_same_csv_read_as_utf_8_is_rejected_instead_of_garbled() {
        IllegalArgumentException refused =
                assertThrows(IllegalArgumentException.class, () -> ImportText.decode(sample("bank-statement-bg.csv"), "UTF-8"));
        assertTrue(refused.getMessage()
                          .contains("not valid UTF-8"),
                refused.getMessage());
    }

    @Test
    void a_column_the_import_reads_must_be_in_the_file() {
        String text = ImportText.decode(sample("bank-statement-bg.csv"), "windows-1251");
        IllegalArgumentException refused =
                assertThrows(IllegalArgumentException.class, () -> DelimitedText.read(text, ';', true, 1, List.of("Amount")));
        assertTrue(refused.getMessage()
                          .contains("no column [Amount]"),
                refused.getMessage());
    }

    @Test
    void a_csv_without_a_header_is_read_by_position_and_a_utf_8_byte_order_mark_is_dropped() {
        byte[] withBom = ((char) 0xFEFF + "2026-01-02,\"a, b\",3\n2026-01-03,c,4\n").getBytes(StandardCharsets.UTF_8);
        List<Map<String, String>> records = DelimitedText.read(ImportText.decode(withBom, "UTF-8"), ',', false, 0, List.of("1", "2", "3"));

        assertEquals(2, records.size());
        assertEquals("2026-01-02", records.get(0)
                                          .get("1"));
        assertEquals("a, b", records.get(0)
                                    .get("2"));
        IllegalArgumentException refused =
                assertThrows(IllegalArgumentException.class, () -> DelimitedText.read("a,b\n", ',', false, 0, List.of("3")));
        assertTrue(refused.getMessage()
                          .contains("no column 3"),
                refused.getMessage());
    }

    @Test
    void an_unclosed_quote_is_refused_with_its_line() {
        IllegalArgumentException refused =
                assertThrows(IllegalArgumentException.class, () -> DelimitedText.read("a;b\n1;\"open\n", ';', true, 0, List.of("a")));
        assertTrue(refused.getMessage()
                          .contains("line 2"),
                refused.getMessage());
    }

    @Test
    void a_number_with_an_undeclared_separator_is_refused_rather_than_guessed() {
        ImportValues commaDecimal = ImportValues.of(',', "", null, null);
        assertThrows(IllegalArgumentException.class, () -> commaDecimal.decimal("1.234,56"));
        assertEquals(new BigDecimal("1234.56"), commaDecimal.decimal("1 234,56"));
        assertNull(commaDecimal.decimal("  "));
        assertThrows(IllegalArgumentException.class, () -> commaDecimal.integer("1,5"));
        assertEquals(Boolean.TRUE, commaDecimal.bool("Yes"));
        assertThrows(IllegalArgumentException.class, () -> commaDecimal.date("15.03.2026"));
        assertThrows(IllegalArgumentException.class, () -> ImportValues.of(',', ",", null, null));
    }

    @Test
    void an_mt940_file_maps_every_statement_line_with_its_details() {
        List<Map<String, String>> records = Mt940.read(ImportText.decode(sample("statement.mt940"), "UTF-8"));

        assertEquals(3, records.size());
        Map<String, String> credit = records.get(0);
        assertEquals("STMT-2026-03", credit.get("statementId"));
        assertEquals("BG80BNBG96611020345678", credit.get("account"));
        assertEquals("00042/001", credit.get("sequence"));
        assertEquals("BGN", credit.get("currency"));
        assertEquals("1", credit.get("lineNumber"));
        assertEquals("2026-03-15", credit.get("valueDate"));
        assertEquals("2026-03-15", credit.get("bookingDate"));
        assertEquals("C", credit.get("mark"));
        assertEquals("1234.56", credit.get("amount"));
        assertEquals("NTRF", credit.get("transactionType"));
        assertEquals("INV/2026/1001", credit.get("reference"), "a single slash belongs to the customer reference");
        assertEquals("BANK-REF-1", credit.get("bankReference"));
        assertEquals("PAYMENT FROM ACME", credit.get("supplementary"));
        assertEquals("Плащане по фактура 1001\nACME OOD", credit.get("details"));

        Map<String, String> debit = records.get(1);
        assertEquals("2", debit.get("lineNumber"));
        assertEquals("-12.5", debit.get("amount"));
        assertFalse(debit.containsKey("bankReference"));

        Map<String, String> reversal = records.get(2);
        assertEquals("STMT-2026-04", reversal.get("statementId"));
        assertEquals("1", reversal.get("lineNumber"), "line numbers restart with every statement");
        assertEquals("-100.00", reversal.get("amount"), "a reversed credit is a debit");
        assertFalse(reversal.containsKey("bookingDate"), "no entry date is not invented");
        assertFalse(reversal.containsKey("details"));
        assertTrue(Mt940.KEYS.containsAll(credit.keySet()));
    }

    @Test
    void an_mt940_line_that_cannot_be_read_rejects_the_file() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> Mt940.read(":20:S\n:61:2613XXC10,00NTRF\n"));
        assertTrue(refused.getMessage()
                          .contains("statement line 1"),
                refused.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Mt940.read(":20:S\n:62F:C260331BGN1,00\n"));
    }

    @Test
    void a_camt_053_statement_maps_every_entry_with_its_counterparty() {
        List<Map<String, String>> records = Camt053.read(sample("statement.camt053.xml"));

        assertEquals(2, records.size());
        Map<String, String> credit = records.get(0);
        assertEquals("CAMT-2026-03", credit.get("statementId"));
        assertEquals("42", credit.get("sequence"));
        assertEquals("BG80BNBG96611020345678", credit.get("account"));
        assertEquals("BGN", credit.get("currency"));
        assertEquals("1", credit.get("lineNumber"));
        assertEquals("1234.56", credit.get("amount"));
        assertEquals("C", credit.get("mark"));
        assertEquals("2026-03-15", credit.get("bookingDate"));
        assertEquals("BANK-REF-1", credit.get("bankReference"));
        assertEquals("PMNT/RCDT/ESCT", credit.get("transactionType"));
        assertEquals("INV-1001", credit.get("reference"));
        assertEquals("Плащане по фактура 1001", credit.get("details"));
        assertEquals("ACME OOD", credit.get("counterpartyName"));
        assertEquals("BG18RZBB91550123456789", credit.get("counterpartyAccount"));

        Map<String, String> debit = records.get(1);
        assertEquals("2", debit.get("lineNumber"));
        assertEquals("-12.50", debit.get("amount"));
        assertEquals("2026-03-16", debit.get("bookingDate"), "the date part of a DtTm");
        assertEquals("FEE", debit.get("transactionType"));
        assertFalse(debit.containsKey("reference"), "NOTPROVIDED is no reference");
        assertEquals("Такса обслужване", debit.get("details"));
        assertEquals("Bank", debit.get("counterpartyName"), "the creditor is the other side of a debit");
        assertTrue(Camt053.KEYS.containsAll(credit.keySet()));
    }

    @Test
    void a_camt_053_entry_carries_its_status_its_reversal_flag_and_how_many_transactions_it_books() {
        List<Map<String, String>> current = Camt053.read(sample("statement.camt053.xml"));
        assertEquals("BOOK", current.get(0)
                                    .get("status"),
                "Sts/Cd of camt.053.001.08");
        assertEquals("false", current.get(0)
                                     .get("reversal"),
                "no RvslInd is no reversal");
        assertEquals("1", current.get(0)
                                 .get("transactions"));

        List<Map<String, String>> records = Camt053.read(sample("statement-batch.camt053.xml"));
        assertEquals(3, records.size(), "one record per entry, a batch included");
        Map<String, String> batch = records.get(0);
        assertEquals("BOOK", batch.get("status"), "the text of Sts up to camt.053.001.07");
        assertEquals("300.00", batch.get("amount"), "the entry amount");
        assertEquals("3", batch.get("transactions"), "a hook can tell a batch entry apart");
        assertEquals("INV-2001", batch.get("reference"));
        assertFalse(batch.containsKey("transaction"), "only a record per transaction has a position");

        assertEquals("PDNG", records.get(1)
                                    .get("status"));
        Map<String, String> reversal = records.get(2);
        assertEquals("true", reversal.get("reversal"));
        assertEquals("-40.00", reversal.get("amount"));
        assertEquals("0", reversal.get("transactions"), "an entry without details books no named transaction");
        assertEquals("Return of credit", reversal.get("details"));
    }

    @Test
    void a_batch_entry_read_per_transaction_yields_one_record_per_payer() {
        List<Map<String, String>> records = Camt053.readTransactions(sample("statement-batch.camt053.xml"));

        assertEquals(5, records.size(), "three transactions of the batch, then the pending entry and the reversal");
        String[][] expected = {{"100.00", "INV-2001", "Alpha Ltd", "DE89370400440532013000", "Invoice 2001"},
                {"120.00", "INV-2002", "Beta GmbH", "DE44500105175407324931", "Invoice 2002"},
                {"80.00", "BATCH-1", "Gamma SA", null, "Payment without invoice number"}};
        for (int i = 0; i < expected.length; i++) {
            Map<String, String> part = records.get(i);
            assertEquals("1", part.get("lineNumber"), "every part keeps its entry's line");
            assertEquals(Integer.toString(i + 1), part.get("transaction"));
            assertEquals("3", part.get("transactions"));
            assertEquals(expected[i][0], part.get("amount"));
            assertEquals(expected[i][1], part.get("reference"), "NOTPROVIDED falls back to the entry reference");
            assertEquals(expected[i][2], part.get("counterpartyName"));
            assertEquals(expected[i][3], part.get("counterpartyAccount"));
            assertEquals(expected[i][4], part.get("details"));
            assertEquals("C", part.get("mark"));
            assertEquals("BOOK", part.get("status"));
            assertTrue(Camt053.KEYS.containsAll(part.keySet()), part.keySet()
                                                                    .toString());
        }
        assertEquals("BANK-BATCH-1-1", records.get(0)
                                              .get("bankReference"),
                "a transaction's own servicer reference");
        assertEquals("BANK-BATCH-1", records.get(1)
                                            .get("bankReference"));

        Map<String, String> pending = records.get(3);
        assertEquals("2", pending.get("lineNumber"));
        assertEquals("1", pending.get("transaction"));
        assertEquals("55.00", pending.get("amount"), "a single transaction keeps the entry amount");
        assertEquals("Delta Inc", pending.get("counterpartyName"));
        assertEquals("1", records.get(4)
                                 .get("transaction"),
                "an entry without details is still one record");
    }

    @Test
    void a_batch_whose_parts_do_not_add_up_or_lack_an_amount_is_refused() {
        String batch = new String(sample("statement-batch.camt053.xml"), StandardCharsets.UTF_8);
        byte[] wrongSum = batch.replace("<Amt Ccy=\"EUR\">80.00</Amt>", "<Amt Ccy=\"EUR\">81.00</Amt>")
                               .getBytes(StandardCharsets.UTF_8);
        IllegalArgumentException mismatch = assertThrows(IllegalArgumentException.class, () -> Camt053.readTransactions(wrongSum));
        assertTrue(mismatch.getMessage()
                           .contains("add up to 301.00"),
                mismatch.getMessage());
        assertEquals(3, Camt053.read(wrongSum)
                               .size(),
                "per entry, the parts are not read");

        byte[] noAmount = batch.replace("<AmtDtls><TxAmt><Amt Ccy=\"EUR\">120.00</Amt></TxAmt></AmtDtls>", "")
                               .getBytes(StandardCharsets.UTF_8);
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, () -> Camt053.readTransactions(noAmount));
        assertTrue(missing.getMessage()
                          .contains("entry 1 transaction 2 has no amount"),
                missing.getMessage());
    }

    @Test
    void a_camt_file_with_a_doctype_is_refused_and_other_xml_is_not_a_statement() {
        String withEntity = """
                <?xml version="1.0"?>
                <!DOCTYPE Document [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <Document><BkToCstmrStmt><Stmt><Id>&secret;</Id></Stmt></BkToCstmrStmt></Document>
                """;
        assertThrows(IllegalArgumentException.class, () -> Camt053.read(withEntity.getBytes(StandardCharsets.UTF_8)));
        IllegalArgumentException notStatement = assertThrows(IllegalArgumentException.class,
                () -> Camt053.read("<Document><BkToCstmrDbtCdtNtfctn/></Document>".getBytes(StandardCharsets.UTF_8)));
        assertTrue(notStatement.getMessage()
                               .contains("not a camt.053"),
                notStatement.getMessage());
    }

    @Test
    void the_checksum_identifies_identical_bytes() {
        assertEquals(ImportText.checksum(sample("statement.mt940")), ImportText.checksum(sample("statement.mt940")));
        assertEquals(64, ImportText.checksum(new byte[0])
                                   .length());
    }

    private static byte[] sample(String name) {
        try (InputStream content = StatementFilesTest.class.getResourceAsStream("/imports/" + name)) {
            if (content == null) {
                throw new IllegalStateException("missing sample file " + name);
            }
            return content.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
