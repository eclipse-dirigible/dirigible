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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Reads an ISO 20022 camt.053 bank-to-customer statement into one record per entry ({@code Ntry}),
 * or - with {@link #readTransactions(byte[])} - one record per transaction of an entry
 * ({@code Ntry/NtryDtls/TxDtls}).
 *
 * <p>
 * Every record carries the keys of {@link #KEYS} - the vocabulary {@link Mt940} uses, plus the
 * counterparty a camt entry names, the entry's booking {@code status} ({@code BOOK}, {@code PDNG},
 * {@code INFO}), whether it is a {@code reversal} of an earlier entry, and how many
 * {@code transactions} it books. Values are canonical: an amount is a plain decimal, negative for a
 * debit ({@code DBIT}), a date is ISO {@code yyyy-MM-dd} and a flag is {@code true} /
 * {@code false}. A key the entry does not carry is absent from its record.
 *
 * <p>
 * Elements are matched by their local name, so every published version of the message
 * ({@code camt.053.001.02} through {@code .001.08} and later) reads alike. The file's own XML
 * declaration decides its encoding. A DOCTYPE is refused outright: a statement has no use for one,
 * and an external entity in an uploaded file must never be resolved.
 */
public final class Camt053 {

    /** Every key a record may carry. */
    public static final Set<String> KEYS = Set.of("statementId", "sequence", "account", "currency", "lineNumber", "valueDate",
            "bookingDate", "mark", "amount", "transactionType", "reference", "bankReference", "details", "counterpartyName",
            "counterpartyAccount", "status", "reversal", "transactions", "transaction");

    /** The keys only {@link #readTransactions(byte[])} produces. */
    public static final Set<String> TRANSACTION_KEYS = Set.of("transaction");

    private static final String NOT_PROVIDED = "NOTPROVIDED";

    private Camt053() {}

    /**
     * Read the entries of a camt.053 document - one record per {@code Ntry}. An entry booking several
     * transactions (a batch) is one record of the entry's amount, with the reference and counterparty
     * of its first transaction; its {@code transactions} key says how many it books.
     *
     * @param content the file's bytes
     * @return one record per entry, in document order
     * @throws IllegalArgumentException when the file is not a well-formed camt.053 statement or an
     *         entry lacks its amount or direction
     */
    public static List<Map<String, String>> read(byte[] content) {
        return read(content, false);
    }

    /**
     * Read the transactions of a camt.053 document - one record per {@code Ntry/NtryDtls/TxDtls}, so a
     * batch-booked entry yields one record per payer. Each record carries its own amount (the
     * transaction's {@code Amt}, or {@code AmtDtls/TxAmt/Amt}), reference, counterparty and remittance,
     * the entry's {@code lineNumber}, and its 1-based {@code transaction} position within the entry. An
     * entry with at most one transaction is one record of the entry's amount, exactly as
     * {@link #read(byte[])} reads it.
     *
     * @param content the file's bytes
     * @return one record per transaction, in document order
     * @throws IllegalArgumentException when the file is not a well-formed camt.053 statement, an entry
     *         lacks its amount or direction, a transaction of a batch has no amount, or the amounts of
     *         a batch do not add up to its entry's amount
     */
    public static List<Map<String, String>> readTransactions(byte[] content) {
        return read(content, true);
    }

    private static List<Map<String, String>> read(byte[] content, boolean perTransaction) {
        Element root = parse(content).getDocumentElement();
        Element message = child(root, "BkToCstmrStmt");
        if (message == null) {
            throw new IllegalArgumentException(
                    "the file is not a camt.053 bank-to-customer statement (no BkToCstmrStmt under " + root.getLocalName() + ")");
        }
        List<Map<String, String>> records = new ArrayList<>();
        int entries = 0;
        for (Element statement : children(message, "Stmt")) {
            Map<String, String> header = new LinkedHashMap<>();
            putIfPresent(header, "statementId", text(child(statement, "Id")));
            putIfPresent(header, "sequence", text(child(statement, "ElctrncSeqNb")));
            Element account = child(statement, "Acct");
            putIfPresent(header, "account", accountId(child(account, "Id")));
            putIfPresent(header, "currency", text(child(account, "Ccy")));
            int lineNumber = 0;
            for (Element entry : children(statement, "Ntry")) {
                lineNumber++;
                entries++;
                Map<String, String> record = new LinkedHashMap<>(header);
                record.put("lineNumber", Integer.toString(lineNumber));
                List<Element> transactions = transactions(entry);
                boolean debit = readEntry(entry, record, entries, transactions.size());
                if (!perTransaction || transactions.size() <= 1) {
                    Element details = transactions.isEmpty() ? null : transactions.get(0);
                    putIfPresent(record, "reference", reference(entry, details));
                    putIfPresent(record, "details", remittance(entry));
                    counterparty(record, details, debit);
                    if (perTransaction) {
                        record.put("transaction", "1");
                    }
                    records.add(record);
                } else {
                    records.addAll(readBatch(entry, record, transactions, debit, entries));
                }
            }
        }
        if (records.isEmpty()) {
            throw new IllegalArgumentException("the file holds no camt.053 statement entry (Ntry)");
        }
        return records;
    }

    /**
     * Fill the entry-level keys of a record.
     *
     * @return whether the entry is a debit
     */
    private static boolean readEntry(Element entry, Map<String, String> record, int number, int transactions) {
        Element amount = child(entry, "Amt");
        String direction = text(child(entry, "CdtDbtInd"));
        if (amount == null || text(amount) == null || direction == null) {
            throw new IllegalArgumentException("entry " + number + " has no Amt or no CdtDbtInd");
        }
        boolean debit = debit(direction, "entry " + number);
        BigDecimal value = amount(amount, "entry " + number);
        record.put("amount", (debit ? value.negate() : value).toPlainString());
        record.put("mark", debit ? "D" : "C");
        String currency = amount.getAttribute("Ccy");
        if (!currency.isBlank()) {
            record.put("currency", currency);
        }
        putIfPresent(record, "status", status(child(entry, "Sts")));
        record.put("reversal", Boolean.toString(reversal(entry, number)));
        record.put("transactions", Integer.toString(transactions));
        putIfPresent(record, "bookingDate", date(child(entry, "BookgDt")));
        putIfPresent(record, "valueDate", date(child(entry, "ValDt")));
        putIfPresent(record, "bankReference", text(child(entry, "AcctSvcrRef")));
        putIfPresent(record, "transactionType", transactionType(child(entry, "BkTxCd")));
        return debit;
    }

    /**
     * One record per transaction of a batch-booked entry. Every transaction must name its amount, and
     * the amounts must add up to the entry's: a part that cannot be told apart is never guessed.
     */
    private static List<Map<String, String>> readBatch(Element entry, Map<String, String> entryRecord, List<Element> transactions,
            boolean entryDebit, int number) {
        List<Map<String, String>> records = new ArrayList<>(transactions.size());
        BigDecimal total = BigDecimal.ZERO;
        int position = 0;
        for (Element details : transactions) {
            position++;
            String subject = "entry " + number + " transaction " + position;
            Element amount = child(details, "Amt");
            if (amount == null) {
                amount = child(child(child(details, "AmtDtls"), "TxAmt"), "Amt");
            }
            if (amount == null || text(amount) == null) {
                throw new IllegalArgumentException(subject + " has no amount (Amt or AmtDtls/TxAmt/Amt) - the entry books "
                        + transactions.size() + " transactions, so its parts cannot be told apart");
            }
            String direction = text(child(details, "CdtDbtInd"));
            boolean debit = direction == null ? entryDebit : debit(direction, subject);
            BigDecimal value = amount(amount, subject);
            BigDecimal signed = debit ? value.negate() : value;
            total = total.add(signed);
            Map<String, String> record = new LinkedHashMap<>(entryRecord);
            record.put("amount", signed.toPlainString());
            record.put("mark", debit ? "D" : "C");
            record.put("transaction", Integer.toString(position));
            String bankReference = text(child(child(details, "Refs"), "AcctSvcrRef"));
            if (bankReference != null) {
                record.put("bankReference", bankReference);
            }
            putIfPresent(record, "reference", reference(entry, details));
            putIfPresent(record, "details", transactionRemittance(entry, details));
            counterparty(record, details, debit);
            records.add(record);
        }
        BigDecimal entryAmount = new BigDecimal(entryRecord.get("amount"));
        if (total.compareTo(entryAmount) != 0) {
            throw new IllegalArgumentException("entry " + number + " books " + entryAmount.toPlainString() + ", but its "
                    + transactions.size() + " transactions add up to " + total.toPlainString());
        }
        return records;
    }

    /**
     * The counterparty is whoever is on the other side: the debtor of a credit, the creditor of a
     * debit.
     */
    private static void counterparty(Map<String, String> record, Element details, boolean debit) {
        if (details == null) {
            return;
        }
        Element parties = child(details, "RltdPties");
        String party = debit ? "Cdtr" : "Dbtr";
        putIfPresent(record, "counterpartyName", text(first(child(parties, party), "Nm")));
        Element partyAccount = child(parties, party + "Acct");
        putIfPresent(record, "counterpartyAccount", accountId(child(partyAccount, "Id")));
    }

    /** Every {@code TxDtls} of the entry, across all of its {@code NtryDtls} blocks. */
    private static List<Element> transactions(Element entry) {
        List<Element> transactions = new ArrayList<>();
        for (Element details : children(entry, "NtryDtls")) {
            transactions.addAll(children(details, "TxDtls"));
        }
        return transactions;
    }

    private static boolean debit(String direction, String subject) {
        if ("DBIT".equals(direction)) {
            return true;
        }
        if ("CRDT".equals(direction)) {
            return false;
        }
        throw new IllegalArgumentException(subject + " has an unknown CdtDbtInd [" + direction + "]");
    }

    private static BigDecimal amount(Element amount, String subject) {
        try {
            return new BigDecimal(text(amount));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(subject + " has an invalid amount [" + text(amount) + "]", e);
        }
    }

    /**
     * The booking status: {@code Sts/Cd} (camt.053.001.08 on), {@code Sts/Prtry}, or the text of
     * {@code Sts} itself (up to .001.07).
     */
    private static String status(Element status) {
        if (status == null) {
            return null;
        }
        String code = text(child(status, "Cd"));
        if (code != null) {
            return code;
        }
        String proprietary = text(child(status, "Prtry"));
        if (proprietary != null) {
            return proprietary;
        }
        return child(status, "*") == null ? text(status) : null;
    }

    /** {@code RvslInd}: absent means the entry is no reversal. */
    private static boolean reversal(Element entry, int number) {
        String indicator = text(child(entry, "RvslInd"));
        if (indicator == null || "false".equals(indicator) || "0".equals(indicator)) {
            return false;
        }
        if ("true".equals(indicator) || "1".equals(indicator)) {
            return true;
        }
        throw new IllegalArgumentException("entry " + number + " has an invalid RvslInd [" + indicator + "]");
    }

    private static String reference(Element entry, Element details) {
        String endToEnd = details == null ? null : text(first(details, "EndToEndId"));
        if (endToEnd != null && !NOT_PROVIDED.equals(endToEnd)) {
            return endToEnd;
        }
        return text(child(entry, "NtryRef"));
    }

    /**
     * The unstructured remittance lines of the entry, or its additional information when it has none.
     */
    private static String remittance(Element entry) {
        List<String> lines = new ArrayList<>();
        NodeList unstructured = entry.getElementsByTagNameNS("*", "Ustrd");
        for (int i = 0; i < unstructured.getLength(); i++) {
            String line = text((Element) unstructured.item(i));
            if (line != null) {
                lines.add(line);
            }
        }
        if (!lines.isEmpty()) {
            return String.join(" ", lines);
        }
        String additional = text(child(entry, "AddtlNtryInf"));
        return additional != null ? additional : text(first(entry, "AddtlTxInf"));
    }

    /**
     * The unstructured remittance lines of one transaction, or its additional information, or the
     * entry's when the transaction has neither.
     */
    private static String transactionRemittance(Element entry, Element details) {
        List<String> lines = new ArrayList<>();
        NodeList unstructured = details.getElementsByTagNameNS("*", "Ustrd");
        for (int i = 0; i < unstructured.getLength(); i++) {
            String line = text((Element) unstructured.item(i));
            if (line != null) {
                lines.add(line);
            }
        }
        if (!lines.isEmpty()) {
            return String.join(" ", lines);
        }
        String additional = text(child(details, "AddtlTxInf"));
        return additional != null ? additional : text(child(entry, "AddtlNtryInf"));
    }

    /**
     * A proprietary code when the bank gives one, otherwise the ISO domain/family/sub-family triple.
     */
    private static String transactionType(Element code) {
        if (code == null) {
            return null;
        }
        String proprietary = text(child(child(code, "Prtry"), "Cd"));
        if (proprietary != null) {
            return proprietary;
        }
        Element domain = child(code, "Domn");
        String domainCode = text(child(domain, "Cd"));
        if (domainCode == null) {
            return null;
        }
        Element family = child(domain, "Fmly");
        String familyCode = text(child(family, "Cd"));
        String subFamily = text(child(family, "SubFmlyCd"));
        StringBuilder type = new StringBuilder(domainCode);
        if (familyCode != null) {
            type.append('/')
                .append(familyCode);
        }
        if (subFamily != null) {
            type.append('/')
                .append(subFamily);
        }
        return type.toString();
    }

    /** {@code Dt}, or the date part of {@code DtTm}. */
    private static String date(Element holder) {
        String date = text(child(holder, "Dt"));
        if (date != null) {
            return date.length() > 10 ? date.substring(0, 10) : date;
        }
        String dateTime = text(child(holder, "DtTm"));
        return dateTime != null && dateTime.length() >= 10 ? dateTime.substring(0, 10) : null;
    }

    private static String accountId(Element id) {
        String iban = text(child(id, "IBAN"));
        return iban != null ? iban : text(child(child(id, "Othr"), "Id"));
    }

    private static Document parse(byte[] content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            // The default handler prints every report to stderr; here each one rejects the file instead.
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            return builder.parse(new ByteArrayInputStream(content));
        } catch (SAXException e) {
            throw new IllegalArgumentException("the file is not well-formed XML: " + e.getMessage(), e);
        } catch (ParserConfigurationException | IOException e) {
            throw new IllegalStateException("cannot read the XML of the file", e);
        }
    }

    private static void putIfPresent(Map<String, String> record, String key, String value) {
        if (value != null) {
            record.put(key, value);
        }
    }

    /**
     * The first direct child element with the local name ({@code *} for any), or {@code null} (also for
     * a null parent).
     */
    private static Element child(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && ("*".equals(localName) || localName.equals(element.getLocalName()))) {
                return element;
            }
        }
        return null;
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> elements = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && localName.equals(element.getLocalName())) {
                elements.add(element);
            }
        }
        return elements;
    }

    /** The first descendant element with the local name, or {@code null} (also for a null parent). */
    private static Element first(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        NodeList found = parent.getElementsByTagNameNS("*", localName);
        return found.getLength() == 0 ? null : (Element) found.item(0);
    }

    private static String text(Element element) {
        if (element == null) {
            return null;
        }
        String text = element.getTextContent()
                             .strip();
        return text.isEmpty() ? null : text;
    }
}
