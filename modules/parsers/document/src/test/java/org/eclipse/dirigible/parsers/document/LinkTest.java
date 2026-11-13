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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.dirigible.parsers.document.binding.DataBinder;
import org.eclipse.dirigible.parsers.document.layout.LayoutException;
import org.eclipse.dirigible.parsers.document.parser.DocumentParser;
import org.eclipse.dirigible.parsers.document.parser.ParseException;
import org.eclipse.dirigible.parsers.document.renderer.ImageResolver;
import org.eclipse.dirigible.parsers.document.renderer.XslFoRenderer;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The {@code href} attribute: which targets are a link, when a refused one is reported - a literal
 * where the template is parsed, every value once the data is bound - and what the renderer emits.
 */
public class LinkTest {

    private static final String FO = "http://www.w3.org/1999/XSL/Format";

    private static final List<String> REFUSED = List.of("javascript:alert(1)", "JavaScript:alert(1)", "vbscript:x", "file:///etc/passwd",
            "data:text/html,x", "ftp://example.com/a", "//example.com", "/invoices/1", "invoices/1", "example.com", "www.example.com",
            "#top", "?a=b", "https:example.com", "https:/example.com", "https:///example.com", "http://?x", "https:", "mailto",
            "java\tscript:alert(1)", "ht\ntps://example.com", "x https://example.com", "{https://example.com}");

    private final DocumentParser parser = new DocumentParser();
    private final DataBinder binder = new DataBinder();

    // --- the target ---------------------------------------------------------------------------

    @Test
    public void theFourKindsOfLinkAreAcceptedWhateverTheCaseOfTheScheme() {
        for (String target : List.of("https://example.com", "http://example.com/a?b=c#d", "HTTPS://Example.com/Path", "Http://example.com",
                "mailto:office@example.com", "MailTo:office@example.com?subject=Invoice", "tel:+35929876543", "TEL:112",
                "https://[2001:db8::1]:8443/a", "https://user:secret@example.com:8443/a")) {
            assertEquals(target, LinkTarget.parse(target));
        }
    }

    @Test
    public void everyOtherTargetIsRefused() {
        for (String target : REFUSED) {
            try {
                LinkTarget.parse(target);
            } catch (IllegalArgumentException e) {
                assertEquals("Invalid href: '" + target + "' (expected an http://, https://, mailto: or tel: link)", e.getMessage());
                continue;
            }
            fail("Expected [" + target + "] to be refused");
        }
    }

    @Test
    public void aBlankValueAndABareSchemeAreNoLink() {
        for (String target : new String[] {null, "", "   ", "\n\t", "mailto:", "tel:", "https://", "HTTP://", "  mailto:  "}) {
            assertEquals("[" + target + "]", "", LinkTarget.parse(target));
        }
    }

    @Test
    public void theValueIsTrimmed() {
        assertEquals("https://example.com/a", LinkTarget.parse("  https://example.com/a\n"));
    }

    @Test
    public void charactersOutsideTheUriSyntaxArePercentEncoded() {
        assertEquals("https://example.com/a%20b?q=1&n=%22x%22&p=%281%29&s=%27%7By%7D%27&t=%3Cz%3E%5C%5E%60%7C#frag",
                LinkTarget.parse("https://example.com/a b?q=1&n=\"x\"&p=(1)&s='{y}'&t=<z>\\^`|#frag"));
        assertEquals("tel:+359%202%20987%2065%2043", LinkTarget.parse("tel:+359 2 987 65 43"));
        assertEquals("https://example.com/a%0Ab%09c", LinkTarget.parse("https://example.com/a\nb\tc"));
    }

    @Test
    public void cyrillicIsEncodedAsUtf8AndAnInternationalHostAsPunycode() {
        assertEquals("https://xn--e1afmkfd.xn--90ae/%D0%BF%D1%8A%D1%82", LinkTarget.parse("https://пример.бг/път"));
        assertEquals("https://user@xn--e1afmkfd.xn--90ae:8443/x", LinkTarget.parse("https://user@пример.бг:8443/x"));
        assertEquals("http://xn--e1afmkfd.xn--90ae", LinkTarget.parse("http://пример.бг"));
        assertEquals("mailto:%D0%BE%D1%84%D0%B8%D1%81@example.com?subject=Hello%20World",
                LinkTarget.parse("mailto:офис@example.com?subject=Hello World"));
        assertEquals("https://example.com/%F0%9F%99%82", LinkTarget.parse("https://example.com/🙂"));
    }

    @Test
    public void percentEscapesAreKeptAndAStrayPercentIsEncoded() {
        assertEquals("https://example.com/a%20b%2Fc%25zz%25", LinkTarget.parse("https://example.com/a%20b%2Fc%zz%"));
    }

    // --- parsing ------------------------------------------------------------------------------

    @Test
    public void hrefIsAnAttributeLikeAnyOther() {
        Node text = onlyChild(parser.parse("<document><text href=\"https://example.com\">example.com</text></document>"));

        assertEquals("https://example.com", text.attributes()
                                                .get("href"));
        assertEquals("example.com", text.text());
    }

    @Test
    public void aRefusedLiteralIsAParseErrorAtTheAttribute() {
        for (String element : List.of("<text href=\"%s\">x</text>", "<image src=\"logo.png\" href=\"%s\"/>",
                "<qrcode value=\"x\" href=\"%s\"/>", "<table source=\"items\"><column href=\"%s\">x</column></table>")) {
            String template = "<document>\n  " + String.format(element, "javascript:alert(1)") + "\n</document>";
            try {
                parser.parse(template);
            } catch (ParseException e) {
                assertTrue(e.getMessage(), e.getMessage()
                                            .startsWith(
                                                    "Invalid href: 'javascript:alert(1)' (expected an http://, https://, mailto: or tel: link) at line 2, column "));
                assertEquals(2, e.getLine());
                assertEquals(template.split("\n")[1].indexOf("href") + 1, e.getColumn());
                continue;
            }
            fail("Expected ParseException for " + element);
        }
    }

    @Test
    public void everyRefusedLiteralFailsTheParse() {
        for (String target : REFUSED) {
            try {
                parser.parse("<document><text href=\"" + target.replace("\"", "&quot;") + "\">x</text></document>");
            } catch (ParseException e) {
                continue;
            }
            fail("Expected ParseException for [" + target + "]");
        }
    }

    @Test
    public void aValueWithAPlaceholderIsJudgedOnlyAfterBinding() {
        parser.parse("<document><text href=\"{{document.Site}}\">x</text><text href=\"javascript:{{document.Code}}\">y</text></document>");
    }

    @Test
    public void aBlankOrSchemeOnlyLiteralParses() {
        parser.parse("<document><text href=\"\">x</text><text href=\"mailto:\">y</text></document>");
    }

    @Test
    public void hrefOnAnElementThatTakesNoLinkIsLeftAlone() {
        String fo = render("<document><section href=\"anything\"><field label=\"A\" href=\"b\">c</field></section></document>", Map.of());

        assertFalse(fo, fo.contains("basic-link"));
    }

    // --- binding ------------------------------------------------------------------------------

    @Test
    public void placeholdersAndFormatsResolveInHref() {
        Node bound = onlyChild(binder.bind(parser.parse(
                "<document><text href=\"https://example.com/i/{{document.Number}}?d={{document.Date:yyyyMMdd}}&amp;n={{document.Local|document.Name}}\">View online</text></document>"),
                Map.of("document", Map.of("Number", "SI-42", "Date", "2026-11-13", "Name", "Acme"))));

        assertEquals("https://example.com/i/SI-42?d=20261113&n=Acme", bound.attributes()
                                                                           .get("href"));
    }

    @Test
    public void anUnresolvedPlaceholderLeavesABlankHref() {
        Node bound = onlyChild(binder.bind(parser.parse("<document><text href=\"{{document.Site}}\">x</text></document>"), Map.of()));

        assertEquals("", bound.attributes()
                              .get("href"));
    }

    // --- rendering ----------------------------------------------------------------------------

    @Test
    public void aTextWithHrefIsABasicLinkAroundItsText() {
        String fo =
                render("<document><text align=\"center\" href=\"https://example.com\">Issued with Example - example.com</text></document>",
                        Map.of());

        assertTrue(fo, fo.contains(
                "<fo:block text-align=\"center\"><fo:basic-link external-destination=\"url('https://example.com')\">Issued with Example - example.com</fo:basic-link></fo:block>"));
    }

    @Test
    public void aLinkChangesNothingElseInTheOutput() {
        String plain = render("<document><text style=\"caption\">example.com</text></document>", Map.of());
        String linked = render("<document><text style=\"caption\" href=\"https://example.com\">example.com</text></document>", Map.of());

        assertEquals(plain, linked.replace("<fo:basic-link external-destination=\"url('https://example.com')\">", "")
                                  .replace("</fo:basic-link>", ""));
    }

    @Test
    public void theLinkStyleIsOptInAndNeedsNoHref() {
        String fo = render("<document><text style=\"link\">example.com</text></document>", Map.of());

        assertTrue(fo, fo.contains("<fo:block color=\"#0563C1\" text-decoration=\"underline\">example.com</fo:block>"));
    }

    @Test
    public void aBlankHrefAfterBindingRendersTheElementWithoutALink() {
        String fo = render("""
                <document>
                    <text href="{{document.Site}}">site</text>
                    <text href="mailto:{{document.Email}}">mail {{document.Email}}</text>
                    <text href="   ">blank</text>
                    <image src="logo.png" href="{{document.Site}}"/>
                </document>
                """, Map.of("document", Map.of("Site", "  ")));

        assertFalse(fo, fo.contains("basic-link"));
        assertTrue(fo.contains("<fo:block>site</fo:block>"));
        assertTrue(fo.contains("<fo:block>mail </fo:block>"));
        assertTrue(fo.contains("<fo:block>blank</fo:block>"));
        assertTrue(fo.contains("<fo:block><fo:external-graphic src=\"logo.png\"/></fo:block>"));
    }

    @Test
    public void aHrefBuiltFromTheDataIsTheLink() {
        String fo = render("<document><text href=\"mailto:{{document.Email}}\">{{document.Email}}</text></document>",
                Map.of("document", Map.of("Email", "office@example.com")));

        assertTrue(fo,
                fo.contains("<fo:basic-link external-destination=\"url('mailto:office@example.com')\">office@example.com</fo:basic-link>"));
    }

    @Test
    public void aSchemeSmuggledInThroughTheDataIsALayoutErrorAtTheAttribute() {
        for (String target : REFUSED) {
            try {
                render("<document>\n<text align=\"center\" href=\"{{document.Site}}\">site</text>\n</document>",
                        Map.of("document", Map.of("Site", target)));
            } catch (LayoutException e) {
                assertEquals("Invalid href: '" + target + "' (expected an http://, https://, mailto: or tel: link) at line 2, column 22",
                        e.getMessage());
                assertEquals(2, e.getPosition()
                                 .line());
                assertEquals(22, e.getPosition()
                                  .column());
                continue;
            }
            fail("Expected LayoutException for [" + target + "]");
        }
    }

    /** A bound value that itself carries braces must not be mistaken for a still-unbound one. */
    @Test
    public void aDataValueThatLooksLikeAPlaceholderIsStillJudged() {
        try {
            render("<document><text href=\"{{document.Site}}\">site</text></document>",
                    Map.of("document", Map.of("Site", "javascript:alert('{{x}}')")));
        } catch (LayoutException e) {
            assertTrue(e.getMessage(), e.getMessage()
                                        .startsWith("Invalid href: 'javascript:alert('{{x}}')'"));
            return;
        }
        fail("Expected LayoutException");
    }

    @Test
    public void theDataCannotChangeTheSchemeTheTemplateWrote() {
        String fo = render("<document><text href=\"https://{{document.Site}}\">site</text></document>",
                Map.of("document", Map.of("Site", "javascript:alert(1)")));

        assertTrue(fo, fo.contains("external-destination=\"url('https://javascript:alert%281%29')\""));
    }

    /** The renderer is the authority: a bound tree that never went through the parser is judged too. */
    @Test
    public void aRefusedLiteralIsALayoutErrorWhenTheTreeWasNotParsed() {
        SourcePosition position = new SourcePosition(3, 7);
        for (Node element : List.<Node>of(new TextNode(position, href("file:///etc/passwd", position), List.of(), "x"),
                new ImageNode(position, href("file:///etc/passwd", position), List.of(), ""),
                new QrCodeNode(position, href("file:///etc/passwd", position), List.of(), ""))) {
            try {
                new XslFoRenderer().renderBound(new DocumentNode(position, Attributes.EMPTY, List.of(element), ""));
            } catch (LayoutException e) {
                assertEquals(position, e.getPosition());
                continue;
            }
            fail("Expected LayoutException for <" + element.tag() + ">");
        }
    }

    @Test
    public void theTargetIsEscapedForXmlForXsltAndForTheUrlNotation() throws Exception {
        String fo = render("<document><text href=\"https://example.com/търси?q={{document.Q}}&amp;n=1\">A &amp; B</text></document>",
                Map.of("document", Map.of("Q", "a&b \"c\" (d) {e} 'f' <g>")));
        String target = "https://example.com/%D1%82%D1%8A%D1%80%D1%81%D0%B8?q=a&b%20%22c%22%20%28d%29%20%7Be%7D%20%27f%27%20%3Cg%3E&n=1";

        assertTrue(fo, fo.contains("external-destination=\"url('" + target.replace("&", "&amp;") + "')\">A &amp; B</fo:basic-link>"));
        List<Element> links = links(fo);
        assertEquals(1, links.size());
        assertEquals("url('" + target + "')", links.get(0)
                                                   .getAttribute("external-destination"));
        assertEquals("A & B", links.get(0)
                                   .getTextContent());
    }

    @Test
    public void linksRenderInTheFlowARowAStackAFooterAndUnderAnIf() throws Exception {
        String fo = render("""
                <document>
                    <page>
                        <text href="https://example.com/page">page</text>
                        <row>
                            <text width="*" href="https://example.com/row">row</text>
                            <stack width="*"><text href="https://example.com/stack">stack</text></stack>
                        </row>
                        <section><text href="tel:+35929876543">section</text></section>
                        <if source="document.Online"><text href="https://example.com/if">if</text></if>
                        <if source="document.Offline"><text href="https://example.com/hidden">hidden</text></if>
                        <footer><text align="center" href="https://example.com/footer">footer</text></footer>
                    </page>
                </document>
                """, Map.of("document", Map.of("Online", true)));

        List<String> targets = new ArrayList<>();
        for (Element link : links(fo)) {
            targets.add(link.getAttribute("external-destination") + " " + link.getTextContent());
        }
        assertEquals(List.of("url('https://example.com/page') page", "url('https://example.com/row') row",
                "url('https://example.com/stack') stack", "url('tel:+35929876543') section", "url('https://example.com/if') if",
                "url('https://example.com/footer') footer"), targets);
        assertTrue(fo, fo.contains(
                "<fo:table-cell><fo:block><fo:basic-link external-destination=\"url('https://example.com/row')\">row</fo:basic-link></fo:block>"));
    }

    @Test
    public void aColumnHrefLinksEachCellFromItsOwnRowAndNeverTheHeader() throws Exception {
        String fo = render("""
                <document>
                    <table source="items">
                        <column label="Item" href="{{Url}}">{{Name}}</column>
                        <column label="Amount" align="right">{{Amount}}</column>
                    </table>
                </document>
                """, Map.of("items", List.of(Map.of("Name", "Widget", "Amount", "100.00", "Url", "https://example.com/p/1"),
                Map.of("Name", "Gadget", "Amount", "23.45"))));

        List<Element> links = links(fo);
        assertEquals(1, links.size());
        assertEquals("url('https://example.com/p/1')", links.get(0)
                                                            .getAttribute("external-destination"));
        assertEquals("Widget", links.get(0)
                                    .getTextContent());
        assertTrue(fo, fo.contains("<fo:table-cell padding=\"2pt\"><fo:block>Gadget</fo:block></fo:table-cell>"));
        assertTrue(fo, fo.contains("<fo:block font-weight=\"bold\">Item</fo:block>"));
    }

    @Test
    public void aRefusedCellTargetIsALayoutErrorAtTheColumn() {
        try {
            render("<document>\n<table source=\"items\">\n<column href=\"{{Url}}\">{{Name}}</column>\n</table>\n</document>",
                    Map.of("items", List.of(Map.of("Name", "Widget", "Url", "javascript:alert(1)"))));
        } catch (LayoutException e) {
            assertEquals(3, e.getPosition()
                             .line());
            return;
        }
        fail("Expected LayoutException");
    }

    @Test
    public void anImageAndAQrCodeAreLinkedAroundTheirGraphic() {
        String template = """
                <document>
                    <image src="logo.png" width="120" align="right" href="https://example.com"/>
                    <qrcode value="{{document.Url}}" href="{{document.Url}}"/>
                </document>
                """;
        String fo = new XslFoRenderer(ImageResolver.PASS_THROUGH, (payload, level) -> "data:image/png;base64,QUJD").renderBound(
                binder.bind(parser.parse(template), Map.of("document", Map.of("Url", "https://example.com/i/SI-42"))));

        assertTrue(fo, fo.contains(
                "<fo:block text-align=\"right\"><fo:basic-link external-destination=\"url('https://example.com')\"><fo:external-graphic src=\"logo.png\" content-width=\"120pt\"/></fo:basic-link></fo:block>"));
        assertTrue(fo, fo.contains(
                "<fo:block><fo:basic-link external-destination=\"url('https://example.com/i/SI-42')\"><fo:external-graphic src=\"data:image/png;base64,QUJD\" content-width=\"90pt\" content-height=\"90pt\"/></fo:basic-link></fo:block>"));
    }

    @Test
    public void anImageOrQrCodeThatRendersNothingLeavesNoLinkBehind() {
        String fo = render("""
                <document>
                    <image src="{{document.Logo}}" href="https://example.com"/>
                    <qrcode value="x" href="https://example.com"/>
                    <text href="https://example.com"></text>
                </document>
                """, Map.of());

        assertFalse(fo, fo.contains("basic-link"));
    }

    private String render(String template, Map<String, Object> data) {
        return new XslFoRenderer().renderBound(binder.bind(parser.parse(template), data));
    }

    private static Attributes href(String value, SourcePosition position) {
        return Attributes.of(List.of(new Attributes.Attribute("href", value, position)));
    }

    /** The {@code fo:basic-link} elements of the stylesheet, read back by a real XML parser. */
    private static List<Element> links(String fo) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        NodeList nodes = factory.newDocumentBuilder()
                                .parse(new InputSource(new StringReader(fo)))
                                .getElementsByTagNameNS(FO, "basic-link");
        List<Element> links = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            links.add((Element) nodes.item(i));
        }
        return links;
    }

    private static Node onlyChild(Node node) {
        assertEquals(1, node.children()
                            .size());
        return node.children()
                   .get(0);
    }
}
