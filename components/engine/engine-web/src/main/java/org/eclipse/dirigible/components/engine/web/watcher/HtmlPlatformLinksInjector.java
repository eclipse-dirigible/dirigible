package org.eclipse.dirigible.components.engine.web.watcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlPlatformLinksInjector {

    private static final Logger logger = LoggerFactory.getLogger(HtmlPlatformLinksInjector.class);

    private final List<PlatformAsset> assets;

    public HtmlPlatformLinksInjector(List<PlatformAsset> assets) {
        this.assets = assets;
    }

    public String processHtml(String html) {

        Document doc = Jsoup.parse(html);
        doc.outputSettings()
           .prettyPrint(true);

        // VALID HTML placeholder
        Elements placeholders = doc.select("meta[name=platform-links]");

        for (Element placeholder : placeholders) {

            String categoryAttr = placeholder.attr("category");

            if (categoryAttr == null || categoryAttr.isBlank()) {
                placeholder.remove();
                continue;
            }

            Set<String> requestedCategories = Arrays.stream(categoryAttr.split(","))
                                                    .map(String::trim)
                                                    .filter(s -> !s.isEmpty())
                                                    .collect(Collectors.toSet());

            Set<String> knownCategories = assets.stream()
                                                .map(PlatformAsset::getCategory)
                                                .collect(Collectors.toSet());

            requestedCategories.forEach(cat -> {
                if (!knownCategories.contains(cat)) {
                    logger.error("Unknown platform category: {}", cat);
                }
            });

            List<PlatformAsset> selectedAssets = assets.stream()
                                                       .filter(a -> requestedCategories.contains(a.getCategory()))
                                                       .sorted(Comparator.comparing(PlatformAsset::getPath))
                                                       .collect(Collectors.toList());

            List<Node> nodes = buildNodes(doc, selectedAssets);

            for (Node node : nodes) {
                placeholder.before(node);
            }

            placeholder.remove();
        }

        return doc.outerHtml();
    }

    private List<Node> buildNodes(Document doc, List<PlatformAsset> assets) {

        List<Node> nodes = new ArrayList<>();

        for (PlatformAsset asset : assets) {

            if (asset.getType() == PlatformAsset.Type.CSS && !doc.select("link[href=\"" + asset.getPath() + "\"]")
                                                                 .isEmpty()) {
                continue;
            }

            if (asset.getType() == PlatformAsset.Type.SCRIPT && !doc.select("script[src=\"" + asset.getPath() + "\"]")
                                                                    .isEmpty()) {
                continue;
            }

            switch (asset.getType()) {

                case CSS -> {
                    Element link = doc.createElement("link");
                    link.attr("rel", "stylesheet");
                    link.attr("href", asset.getPath());
                    nodes.add(link);
                }

                case PRELOAD -> {
                    Element preload = doc.createElement("link");
                    preload.attr("rel", "preload");
                    preload.attr("href", asset.getPath());
                    nodes.add(preload);
                }

                case SCRIPT -> {
                    Element script = doc.createElement("script");
                    script.attr("src", asset.getPath());

                    if (asset.isModule()) {
                        script.attr("type", "module");
                    }

                    if (asset.isDefer()) {
                        script.attr("defer", "defer");
                    }

                    nodes.add(script);
                }
            }
        }

        return nodes;
    }
}
