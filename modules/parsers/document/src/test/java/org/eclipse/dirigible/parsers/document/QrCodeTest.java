/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.parsers.document.binding.DataBinder;
import org.eclipse.dirigible.parsers.document.layout.LayoutException;
import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.eclipse.dirigible.parsers.document.parser.TagRegistry;
import org.eclipse.dirigible.parsers.document.renderer.ImageResolver;
import org.eclipse.dirigible.parsers.document.renderer.QrCodeEncoder;
import org.eclipse.dirigible.parsers.document.renderer.QrCodeLevel;
import org.eclipse.dirigible.parsers.document.renderer.XslFoRenderer;
import org.junit.Test;

/**
 * The built-in {@code <qrcode>}: its payload keeps the line breaks of the template through parsing
 * and binding, and the renderer hands it to the host's encoder.
 */
public class QrCodeTest {

    /** The EPC069-12 payment code of an invoice, indented the way a template is written. */
    private static final String EPC_TEMPLATE = """
            <document>
                <page>
                    <qrcode size="90" level="M">
                        BCD
                        002
                        1
                        SCT
                        {{document.BankAccount.Bic}}
                        {{document.Company.LegalName}}
                        {{document.BankAccount.Iban}}
                        EUR{{document.Balance:0.00}}


                        Invoice {{document.Number}}
                    </qrcode>
                </page>
            </document>
            """;

    private static final String EPC_PAYLOAD =
            "BCD\n002\n1\nSCT\nUNCRBGSF\nМетафор ООД\nBG80BNBG96611020345678\nEUR1250.50\n\n\nInvoice SI-42";

    private final DocumentParser parser = new DocumentParser();
    private final DataBinder binder = new DataBinder();

    @Test
    public void qrcodeIsABuiltInTag() {
        Node qrCode = onlyChild(parser.parse("<document><qrcode value=\"{{document.Uuid}}\"/></document>"));

        assertTrue(qrCode instanceof QrCodeNode);
        assertEquals("qrcode", qrCode.tag());
        assertEquals("{{document.Uuid}}", ((QrCodeNode) qrCode).payload());
    }

    @Test
    public void theTextKeepsItsLinesWithoutTheIndentationOfTheTemplate() {
        QrCodeNode qrCode = qrCode(parser.parse(EPC_TEMPLATE));

        assertEquals("""
                BCD
                002
                1
                SCT
                {{document.BankAccount.Bic}}
                {{document.Company.LegalName}}
                {{document.BankAccount.Iban}}
                EUR{{document.Balance:0.00}}


                Invoice {{document.Number}}""", qrCode.text());
    }

    @Test
    public void theTextMayStartRightAfterTheOpeningTag() {
        QrCodeNode qrCode = (QrCodeNode) onlyChild(parser.parse("<document><qrcode>BCD\n002\n\nlast</qrcode></document>"));

        assertEquals("BCD\n002\n\nlast", qrCode.text());
    }

    @Test
    public void windowsAndOldMacLineEndingsBecomeLineFeeds() {
        QrCodeNode qrCode = (QrCodeNode) onlyChild(parser.parse("<document><qrcode>\r\n\ta\r\n\r\n\tb\rc\r\n</qrcode></document>"));

        assertEquals("a\n\nb\nc", qrCode.text());
    }

    /**
     * Only the ONE blank line the formatting puts at each end is dropped: a second one is the author's
     * own, e.g. the trailing empty elements of a payment code.
     */
    @Test
    public void onlyTheFormattingLineAtEachEndIsDropped() {
        QrCodeNode qrCode = (QrCodeNode) onlyChild(parser.parse("<document><qrcode>\n\n  a\n  b\n\n</qrcode></document>"));

        assertEquals("\na\nb\n", qrCode.text());
    }

    @Test
    public void spacesInsideALineAndEntitiesAreContent() {
        QrCodeNode qrCode =
                (QrCodeNode) onlyChild(parser.parse("<document><qrcode>\n  Fish  &amp;  Chips &lt;1&gt; \n</qrcode></document>"));

        assertEquals("Fish  &  Chips <1>", qrCode.text());
    }

    @Test
    public void everyOtherTagStillCollapsesItsText() {
        Node text = onlyChild(parser.parse("<document><text>\n  two\n  lines\n</text></document>"));

        assertEquals("two lines", text.text());
    }

    @Test
    public void reRegisteringATagTheUsualWayCollapsesItsTextAgain() {
        TagRegistry registry = TagRegistry.builtIn();
        assertTrue(registry.preservesLines("qrcode"));
        registry.register("qrcode");

        Node custom = onlyChild(new DocumentParser(registry).parse("<document><qrcode>\n a\n b\n</qrcode></document>"));

        assertFalse(registry.preservesLines("qrcode"));
        assertTrue(custom instanceof CustomNode);
        assertEquals("a b", custom.text());
    }

    @Test
    public void bindingResolvesPlaceholdersAndFormatsAndKeepsEveryLine() {
        QrCodeNode bound = qrCode(binder.bind(parser.parse(EPC_TEMPLATE), epcData()));

        assertEquals(EPC_PAYLOAD, bound.payload());
        assertEquals("the two empty lines before the remittance text are content", 11, bound.payload()
                                                                                            .split("\n", -1).length);
    }

    @Test
    public void aLineWhosePlaceholderResolvesEmptyStaysAnEmptyLine() {
        QrCodeNode bound = (QrCodeNode) onlyChild(binder.bind(
                parser.parse("<document><qrcode>\n  a\n  {{document.Missing}}\n  b\n  {{document.Missing}}\n</qrcode></document>"),
                Map.of()));

        assertEquals("a\n\nb\n", bound.payload());
    }

    @Test
    public void theValueAttributeIsTheSingleLineShortFormAndWinsOverTheText() {
        QrCodeNode bound = (QrCodeNode) onlyChild(binder.bind(
                parser.parse("<document><qrcode value=\"https://example.com/i/{{document.Number}}\">ignored</qrcode></document>"),
                Map.of("document", Map.of("Number", "SI-42"))));

        assertEquals("https://example.com/i/SI-42", bound.payload());
    }

    @Test
    public void aBlankValueFallsBackToTheText() {
        QrCodeNode bound = (QrCodeNode) onlyChild(
                binder.bind(parser.parse("<document><qrcode value=\"{{document.Missing}}\">text</qrcode></document>"), Map.of()));

        assertEquals("text", bound.payload());
    }

    @Test
    public void theRendererHandsThePayloadAndTheLevelToTheEncoder() {
        List<String> encoded = new ArrayList<>();
        String fo = render(EPC_TEMPLATE.replace("level=\"M\"", "level=\"q\" align=\"right\""), epcData(), (payload, level) -> {
            encoded.add(level + ":" + payload);
            return "data:image/png;base64,QUJD";
        });

        assertEquals(List.of("Q:" + EPC_PAYLOAD), encoded);
        assertTrue(fo, fo.contains(
                "<fo:block text-align=\"right\"><fo:external-graphic src=\"data:image/png;base64,QUJD\" content-width=\"90pt\" content-height=\"90pt\"/></fo:block>"));
    }

    @Test
    public void theLevelDefaultsToMediumAndTheSizeToNinetyPoints() {
        List<QrCodeLevel> levels = new ArrayList<>();
        String fo = render("<document><qrcode value=\"x\"/></document>", Map.of(), (payload, level) -> {
            levels.add(level);
            return "data:image/png;base64,QUJD";
        });

        assertEquals(List.of(QrCodeLevel.M), levels);
        assertTrue(fo.contains("content-width=\"90pt\" content-height=\"90pt\""));
    }

    @Test
    public void anEmptyPayloadRendersNothingAndNeverReachesTheEncoder() {
        String fo = render("""
                <document>
                    <text>Kept</text>
                    <qrcode value="{{document.Missing}}"/>
                    <qrcode>
                        {{document.Missing}}
                    </qrcode>
                    <qrcode/>
                </document>
                """, Map.of(), (payload, level) -> {
            throw new AssertionError("a blank payload must not reach the encoder: [" + payload + "]");
        });

        assertFalse("no graphic may be emitted", fo.contains("external-graphic"));
        assertTrue("the rest of the document still renders", fo.contains(">Kept</fo:block>"));
    }

    @Test
    public void aPayloadTheEncoderDeclinesRendersNothing() {
        String fo = render("<document><text>Kept</text><qrcode value=\"x\"/></document>", Map.of(), (payload, level) -> null);

        assertFalse(fo.contains("external-graphic"));
        assertTrue(fo.contains(">Kept</fo:block>"));
    }

    @Test
    public void aRendererWithoutAHostEncoderRendersNoQrCode() {
        Node bound = binder.bind(parser.parse("<document><text>Kept</text><qrcode value=\"x\"/></document>"), Map.of());

        String fo = new XslFoRenderer().renderBound(bound);

        assertFalse(fo.contains("external-graphic"));
        assertTrue(fo.contains(">Kept</fo:block>"));
    }

    @Test
    public void rendersInsideARowAStackAndAFooterAndUnderAnIf() {
        String template = """
                <document>
                    <row>
                        <stack width="*"><text>Pay by scanning</text></stack>
                        <qrcode width="100" value="row"/>
                    </row>
                    <stack><qrcode value="stack"/></stack>
                    <if source="document.Payable"><qrcode value="shown"/></if>
                    <if source="document.Paid"><qrcode value="hidden"/></if>
                    <footer><qrcode value="footer"/></footer>
                </document>
                """;
        List<String> payloads = new ArrayList<>();

        String fo = render(template, Map.of("document", Map.of("Payable", true, "Paid", false)), (payload, level) -> {
            payloads.add(payload);
            return "data:image/png;base64," + payload;
        });

        assertEquals(List.of("row", "stack", "shown", "footer"), payloads);
        assertTrue(fo, fo.contains("<fo:table-cell><fo:block><fo:external-graphic src=\"data:image/png;base64,row\""));
        assertTrue(fo.contains("<fo:table-column column-width=\"100pt\"/>"));
    }

    @Test
    public void anInvalidLevelIsALayoutErrorAtTheAttribute() {
        try {
            render("<document>\n<qrcode level=\"X\" value=\"x\"/>\n</document>", Map.of(), QrCodeEncoder.NONE);
        } catch (LayoutException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .contains("Invalid level: 'X' (expected L, M, Q or H)"));
            assertEquals(2, e.getPosition()
                             .line());
            return;
        }
        fail("Expected LayoutException");
    }

    @Test
    public void anInvalidSizeIsALayoutErrorEvenWhenThePayloadIsEmpty() {
        for (String size : List.of("0", "-5", "50%", "big")) {
            try {
                render("<document><qrcode size=\"" + size + "\"/></document>", Map.of(), QrCodeEncoder.NONE);
            } catch (LayoutException e) {
                assertTrue(e.getMessage(), e.getMessage()
                                            .contains("Invalid size: '" + size + "' (expected a positive number of points)"));
                continue;
            }
            fail("Expected LayoutException for size " + size);
        }
    }

    private String render(String template, Map<String, Object> data, QrCodeEncoder encoder) {
        return new XslFoRenderer(ImageResolver.PASS_THROUGH, encoder).renderBound(binder.bind(parser.parse(template), data));
    }

    private static Map<String, Object> epcData() {
        return Map.of("document", Map.of("Number", "SI-42", "Balance", 1250.5, "Company", Map.of("LegalName", "Метафор ООД"), "BankAccount",
                Map.of("Bic", "UNCRBGSF", "Iban", "BG80BNBG96611020345678")));
    }

    private static QrCodeNode qrCode(Node document) {
        return (QrCodeNode) onlyChild(onlyChild(document));
    }

    private static Node onlyChild(Node node) {
        assertEquals(1, node.children()
                            .size());
        return node.children()
                   .get(0);
    }
}
