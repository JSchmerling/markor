/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.content.Context;
import android.text.TextUtils;

import com.vladsch.flexmark.ext.admonition.AdmonitionExtension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.ins.InsExtension;
import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.toc.internal.TocOptions;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.AttributeProviderFactory;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.superscript.SuperscriptExtension;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.builder.Extension;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.util.GsContextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import other.com.vladsch.flexmark.ext.katex.FlexmarkKatexExtension;

@SuppressWarnings({"unchecked", "WeakerAccess"})
public class MarkdownTextConverter extends TextConverterBase {
    //########################
    //## Extensions
    //########################
    public static final String EXT_MARKDOWN__TXT = ".txt";
    public static final String EXT_MARKDOWN__MD_TXT = ".md.txt";
    public static final String EXT_MARKDOWN__MD = ".md";
    public static final String EXT_MARKDOWN__MARKDOWN = ".markdown";
    public static final String EXT_MARKDOWN__MKD = ".mkd";
    public static final String EXT_MARKDOWN__MDOWN = ".mdown";
    public static final String EXT_MARKDOWN__MKDN = ".mkdn";
    public static final String EXT_MARKDOWN__MDWN = ".mdwn";
    public static final String EXT_MARKDOWN__MDX = ".mdx";
    public static final String EXT_MARKDOWN__TEXT = ".text";
    public static final String EXT_MARKDOWN__RMD = ".rmd";

    public static final String MD_EXTENSIONS_PATTERN_LIST = "((md)|(markdown)|(mkd)|(mdown)|(mkdn)|(txt)|(mdwn)|(mdx)|(text)|(rmd))";
    public static final Pattern PATTERN_HAS_FILE_EXTENSION_FOR_THIS_FORMAT = Pattern.compile("((?i).*\\." + MD_EXTENSIONS_PATTERN_LIST + "$)");
    public static final Pattern MD_EXTENSION_PATTERN = Pattern.compile("((?i)\\." + MD_EXTENSIONS_PATTERN_LIST + "$)");
    public static final String[] MD_EXTENSIONS = new String[]{
            EXT_MARKDOWN__MD, EXT_MARKDOWN__MARKDOWN, EXT_MARKDOWN__MKD, EXT_MARKDOWN__MDOWN,
            EXT_MARKDOWN__MKDN, EXT_MARKDOWN__TXT, EXT_MARKDOWN__MDWN, EXT_MARKDOWN__TEXT,
            EXT_MARKDOWN__RMD, EXT_MARKDOWN__MD_TXT, EXT_MARKDOWN__MDX
    };

    //########################
    //## Injected CSS / JS / HTML
    //########################
    public static final String HTML_PRESENTATION_BEAMER_SLIDE_START_DIV = "<!-- Presentation slide NO --> <div class='slide_pNO slide'><div class='slide_body'>";
    public static final String TOKEN_SITE_DATE_JEKYLL = "{{ site.time | date: '%x' }}";

    private static final String CSS_PREFIX = "<link rel='stylesheet' href='file:///android_asset/";
    private static final String CSS_POSTFIX = "'/>";
    private static final String JS_PREFIX = "<script src='file:///android_asset/";
    private static final String JS_POSTFIX = "'></script>";

    private static final String CSS_MARKDOWN = CSS_PREFIX + "markdown.css" + CSS_POSTFIX;

    public static final String HTML_KATEX_INCLUDE = CSS_PREFIX + "katex/katex.min.css" + CSS_POSTFIX +
            JS_PREFIX + "katex/katex.min.js" + JS_POSTFIX +
            JS_PREFIX + "katex/katex-render.js" + JS_POSTFIX +
            JS_PREFIX + "katex/mhchem.min.js" + JS_POSTFIX;
    public static final String HTML_MERMAID_INCLUDE = JS_PREFIX + "mermaid/mermaid.min.js" + JS_POSTFIX;
    public static final String HTML_FRONTMATTER_CONTAINER_S = "<div class='front-matter-container'>";
    public static final String HTML_FRONTMATTER_CONTAINER_E = "</div>";
    public static final String HTML_FRONTMATTER_ITEM_CONTAINER_S = "<div class='front-matter-item front-matter-container-{{ attrName }}'>";
    public static final String HTML_FRONTMATTER_ITEM_CONTAINER_E = "</div>";
    public static final String HTML_TOKEN_ITEM_S = "<span class='{{ scope }}-item-{{ attrName }}'>";
    public static final String HTML_TOKEN_ITEM_E = "</span>";
    public static final String HTML_TOKEN_DELIMITER = "<span class='{{ scope }}-delimiter-{{ attrName }} delimiter'></span>";
    public static final String YAML_FRONTMATTER_SCOPES = "post"; //, page, site";
    public static final Pattern YAML_FRONTMATTER_TOKEN_PATTERN = Pattern.compile("\\{\\{\\s+(?:" + YAML_FRONTMATTER_SCOPES.replaceAll(",\\s*", "|") + ")\\.[A-Za-z0-9]+\\s+\\}\\}");

    public static final String HTML_ADMONITION_INCLUDE = CSS_PREFIX + "flexmark/admonition.css" + CSS_POSTFIX +
            JS_PREFIX + "flexmark/admonition.js" + JS_POSTFIX;

    //########################
    //## Converter library
    //########################
    // See https://github.com/vsch/flexmark-java/wiki/Extensions#tables
    private static final List<Extension> flexmarkExtensions = Arrays.asList(
            StrikethroughSubscriptExtension.create(),
            AutolinkExtension.create(),
            InsExtension.create(),
            FlexmarkKatexExtension.KatexExtension.create(),
            JekyllTagExtension.create(),
            JekyllFrontMatterExtension.create(),
            SuperscriptExtension.create(),        // https://github.com/vsch/flexmark-java/wiki/Extensions#superscript
            TablesExtension.create(),
            TaskListExtension.create(),
            EmojiExtension.create(),
            AnchorLinkExtension.create(),
            TocExtension.create(),                // https://github.com/vsch/flexmark-java/wiki/Table-of-Contents-Extension
            SimTocExtension.create(),             // https://github.com/vsch/flexmark-java/wiki/Table-of-Contents-Extension
            WikiLinkExtension.create(),           // https://github.com/vsch/flexmark-java/wiki/Extensions#wikilinks
            YamlFrontMatterExtension.create(),
            TypographicExtension.create(),        // https://github.com/vsch/flexmark-java/wiki/Typographic-Extension
            GitLabExtension.create(),             // https://github.com/vsch/flexmark-java/wiki/Extensions#gitlab-flavoured-markdown
            AdmonitionExtension.create(),         // https://github.com/vsch/flexmark-java/wiki/Extensions#admonition
            FootnoteExtension.create(),            // https://github.com/vsch/flexmark-java/wiki/Footnotes-Extension#overview
            LineNumberIdExtension.create()
    );
    public static final Parser flexmarkParser = Parser.builder().extensions(flexmarkExtensions).build();
    public static final HtmlRenderer flexmarkRenderer = HtmlRenderer.builder().extensions(flexmarkExtensions).build();

    //########################
    //## Others
    //########################
    private static String toDashChars = " -_"; // See HtmlRenderer.HEADER_ID_GENERATOR_TO_DASH_CHARS.getFrom(document)
    private static final Pattern linkPattern = Pattern.compile("\\[(.*?)\\]\\((.*?)(\\s+\".*\")?\\)");


    //########################
    //## Methods
    //########################
    @Override
    public String convertMarkup(String markup, Context context, boolean lightMode, boolean enableLineNumbers, File file) {
        final AppSettings as = AppSettings.get(context);
        String converted, onLoadJs = "", head = "";
        final MutableDataSet options = new MutableDataSet();

        options.set(Parser.EXTENSIONS, flexmarkExtensions);

        options.set(Parser.SPACE_IN_LINK_URLS, true); // Allow links like [this](some filename with spaces.md)

        // options.set(HtmlRenderer.SOFT_BREAK, "<br />\n"); // Add linefeed to HTML break

        options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY); // Use unicode (OS/browser images)

        // GitLab extension
        options.set(GitLabExtension.RENDER_BLOCK_MATH, false);

        // GFM table parsing
        options.set(TablesExtension.WITH_CAPTION, false)
                .set(TablesExtension.COLUMN_SPANS, true)
                .set(TablesExtension.MIN_HEADER_ROWS, 0)
                .set(TablesExtension.MAX_HEADER_ROWS, 1)
                .set(TablesExtension.APPEND_MISSING_COLUMNS, false)
                .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                .set(WikiLinkExtension.LINK_ESCAPE_CHARS, "")
                .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);

        // Add id to headers
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true)
                .set(HtmlRenderer.HEADER_ID_GENERATOR_RESOLVE_DUPES, true)
                .set(AnchorLinkExtension.ANCHORLINKS_SET_ID, false)
                .set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "header_no_underline");

        head += CSS_MARKDOWN;

        // Presentations
        final boolean enablePresentationBeamer = markup.contains("\nclass:beamer") || markup.contains("\nclass: beamer");

        // Front matter
        String fmaText = "";
        final List<String> fmaAllowedAttributes = as.getMarkdownShownYamlFrontMatterKeys();
        Map<String, List<String>> fma = Collections.EMPTY_MAP;
        if (!enablePresentationBeamer && markup.startsWith("---")) {
            Matcher hasTokens = YAML_FRONTMATTER_TOKEN_PATTERN.matcher(markup);
            if (!fmaAllowedAttributes.isEmpty() || hasTokens.find()) {
                // Read YAML attributes
                fma = extractYamlAttributes(markup);
            }

            // Assemble YAML front-matter block
            if (!fmaAllowedAttributes.isEmpty()) {
                for (Map.Entry<String, List<String>> entry : fma.entrySet()) {
                    String attrName = entry.getKey();
                    if (!(fmaAllowedAttributes.contains(attrName) || fmaAllowedAttributes.contains("*"))) {
                        continue;
                    }
                    //noinspection StringConcatenationInLoop
                    fmaText += HTML_FRONTMATTER_ITEM_CONTAINER_S.replace("{{ attrName }}", attrName) + "{{ post." + attrName + " }}\n" + HTML_FRONTMATTER_ITEM_CONTAINER_E + "\n";
                }
            }
        }

        // Table of contents
        final String parentFolderName = file != null && file.getParentFile() != null && !TextUtils.isEmpty(file.getParentFile().getName()) ? file.getParentFile().getName() : "";
        final boolean isInBlogFolder = parentFolderName.equals("_posts") || parentFolderName.equals("blog") || parentFolderName.equals("post");
        if (!enablePresentationBeamer) {
            if (!markup.contains("[TOC]: #") && (isInBlogFolder || as.isMarkdownTableOfContentsEnabled()) && (markup.contains("#") || markup.contains("<h"))) {
                final String tocToken = "[TOC]: # ''\n  \n";
                if (markup.startsWith("---") && !markup.contains("[TOC]")) {
                    // 1st group: match opening YAML block delimiter ('---'), optionally followed by whitespace, excluding newline
                    // 2nd group: match YAML block contents, excluding surrounding newlines
                    // 3rd group: match closing YAML block delimiter ('---' or '...'), excluding newline(s)
                    markup = markup.replaceFirst("(?ms)(^-{3}\\s*?$)\n+(.*?)\n+(^[.-]{3}\\s*?$)\n+", "$1\n$2\n$3\n\n" + tocToken + "\n");
                }

                if (!markup.contains("[TOC]")) {
                    markup = tocToken + markup;
                }
            }


            options.set(TocExtension.LEVELS, TocOptions.getLevels(as.getMarkdownTableOfContentLevels()))
                    .set(TocExtension.TITLE, context.getString(R.string.table_of_contents))
                    .set(TocExtension.DIV_CLASS, "markor-table-of-contents toc")
                    .set(TocExtension.LIST_CLASS, "markor-table-of-contents-list")
                    .set(TocExtension.BLANK_LINE_SPACER, false);
        }

        // Enable Math / KaTex
        if (markup.contains("$")) {
            if (as.isMarkdownMathEnabled()) {
                head += HTML_KATEX_INCLUDE;
            } else {
                markup = markup.replace("$", "\\$");
            }
        }

        // Enable View (block) code syntax highlighting
        if (markup.contains("```")) {
            head += getViewHlPrismIncludes(GsContextUtils.instance.isDarkModeEnabled(context) ? "-tomorrow" : "", enableLineNumbers);
            onLoadJs += "usePrismCodeBlock();";
            if (as.getDocumentWrapState(file.getAbsolutePath())) {
                onLoadJs += "wrapCodeBlockWords();";
            }
        }

        // Enable Mermaid
        if (markup.contains("```mermaid")) {
            head += HTML_MERMAID_INCLUDE
                    + "<script>mermaid.initialize({theme:'"
                    + (GsContextUtils.instance.isDarkModeEnabled(context) ? "dark" : "default")
                    + "',logLevel:5,securityLevel:'loose'});</script>";
        }

        // Enable flexmark Admonition support
        if (markup.contains("!!!") || markup.contains("???")) {
            head += HTML_ADMONITION_INCLUDE;
        }

        // Jekyll: Replace {{ site.baseurl }} with ..--> usually used in Jekyll blog _posts folder which is one folder below repository root, for reference to e.g. pictures in assets folder
        markup = markup.replace("{{ site.baseurl }}", "..").replace(TOKEN_SITE_DATE_JEKYLL, TOKEN_POST_TODAY_DATE);

        // Notable: They use a home brewed syntax for referencing attachments: @attachment/f.png = ../attachements/f.jpg -- https://github.com/gsantner/markor/issues/1252
        markup = markup.replace("](@attachment/", "](../attachements/");

        if (as.isMarkdownNewlineNewparagraphEnabled()) {
            markup = markup.replace("\n", "  \n");
        }

        // Replace space in url with %20, see #1365
        markup = escapeSpacesInLink(markup);

        // Replace tokens in note with corresponding YAML attribute values
        markup = replaceTokens(markup, fma);
        if (!TextUtils.isEmpty(fmaText)) {
            fmaText = replaceTokens(fmaText, fma);
            fmaText = HTML_FRONTMATTER_CONTAINER_S + fmaText + HTML_FRONTMATTER_CONTAINER_E + "\n";
        }

        ////////////
        // Markup parsing - afterwards = HTML
        Document document = flexmarkParser.parse(markup);
        converted = fmaText + flexmarkRenderer.withOptions(options).render(document);

        // After render changes: Fixes for Footnotes (converter creates footnote + <br> + ref#(click) --> remove line break)
        if (converted.contains("footnote-")) {
            converted = converted.replace("</p>\n<a href=\"#fnref-", "<a href=\"#fnref-").replace("class=\"footnote-backref\">&#8617;</a>", "class=\"footnote-backref\"> &#8617;</a></p>");
        }

        // After render changes: Presentations with Beamer
        if (enablePresentationBeamer) {
            int c = 1;
            for (int ihtml = 0; (ihtml = converted.indexOf("<hr />", ihtml)) >= 0 && ihtml < converted.length() + 5; c++) {
                String ins = HTML_PRESENTATION_BEAMER_SLIDE_START_DIV.replace("NO", Integer.toString(c));
                converted = converted.substring(0, ihtml) + (c > 1 ? "</div></div>" : "") + ins + converted.substring(ihtml + "<hr />".length());
                if (converted.contains(ins + "\n<h1 ")) {
                    converted = converted.replace(ins, ins.replace("slide_body", "slide_body slide_title").replace("slide'", "slide_type_title slide'"));
                }
            }
            // Final Slide
            if (c > 1) {
                converted = converted.replace(HTML_PRESENTATION_BEAMER_SLIDE_START_DIV.replace("NO", Integer.toString(c - 1)), "</div></div> <!-- Final presentation slide -->");
            }
        }

        if (enableLineNumbers) {
            // For Prism line numbers plugin
            onLoadJs += "enableLineNumbers(); adjustLineNumbers();";
        }

        // Deliver result
        return putContentIntoTemplate(context, converted, lightMode, file, onLoadJs, head);
    }

    private String escapeSpacesInLink(final String markup) {
        final Matcher matcher = linkPattern.matcher(markup);
        if (!matcher.find()) {
            return markup;
        }
        // 1) Walk through the text till finding a link in markdown syntax
        // 2) Add all text-before-link to buffer
        // 3) Extract [title](link to somehere)
        // 4) Add [title](link%20to%20somewhere) to buffer
        // 5) Do till the end and add all text & links of original-text to buffer
        final StringBuilder sb = new StringBuilder(markup.length() + 64);
        int previousEnd = 0;
        do {
            final String url = matcher.group(2);
            final String title = matcher.group(3);
            if (url == null) {
                return markup;
            }
            sb.append(markup.substring(previousEnd, matcher.start())).append(String.format("[%s](%s%s)", matcher.group(1),
                    url.trim().replace(" ", "%20"),
                    (title != null ? title : ""))
            );
            previousEnd = matcher.end();
        } while (matcher.find());
        sb.append(markup.substring(previousEnd));

        return sb.toString();
    }

    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend"})
    private String getViewHlPrismIncludes(final String theme, final boolean isLineNumbersEnabled) {
        final StringBuilder sb = new StringBuilder(1000);
        sb.append(CSS_PREFIX + "prism/themes/prism" + theme + ".min.css" + CSS_POSTFIX);
        sb.append(CSS_PREFIX + "prism/prism-markor.css" + CSS_POSTFIX);
        sb.append(CSS_PREFIX + "prism/plugins/toolbar/prism-toolbar.css" + CSS_POSTFIX);

        sb.append(JS_PREFIX + "prism/prism.js" + JS_POSTFIX);
        sb.append(JS_PREFIX + "prism/components.js" + JS_POSTFIX);
        sb.append(JS_PREFIX + "prism/prism-markor.js" + JS_POSTFIX);
        sb.append(JS_PREFIX + "prism/plugins/autoloader/prism-autoloader.min.js" + JS_POSTFIX);
        sb.append(JS_PREFIX + "prism/plugins/toolbar/prism-toolbar.min.js" + JS_POSTFIX);
        sb.append(JS_PREFIX + "prism/plugins/copy-to-clipboard/prism-copy-to-clipboard.min.js" + JS_POSTFIX);

        if (isLineNumbersEnabled) {
            sb.append(CSS_PREFIX + "prism/plugins/line-numbers/prism-line-numbers-markor.css" + CSS_POSTFIX);
            sb.append(JS_PREFIX + "prism/plugins/line-numbers/prism-line-numbers.min.js" + JS_POSTFIX);
            sb.append(JS_PREFIX + "prism/plugins/line-numbers/prism-line-numbers-markor.js" + JS_POSTFIX);
        }

        return sb.toString();
    }

    @Override
    protected boolean isFileOutOfThisFormat(final File file, final String name, final String ext) {
        return (MarkdownTextConverter.PATTERN_HAS_FILE_EXTENSION_FOR_THIS_FORMAT.matcher(name).matches() && !name.endsWith(".txt")) || name.endsWith(".md.txt");
    }

    private Map<String, List<String>> extractYamlAttributes(final String markup) {
        final Parser parser = Parser.builder().extensions(Collections.singleton(YamlFrontMatterExtension.create())).build();
        final AbstractYamlFrontMatterVisitor visitor = new AbstractYamlFrontMatterVisitor();
        visitor.visit(parser.parse(markup));
        return visitor.getData();
    }

    private String replaceTokens(final String markup, final Map<String, List<String>> fma) {
        String markupReplaced = markup;

        for (Map.Entry<String, List<String>> entry : fma.entrySet()) {
            String attrName = entry.getKey();
            List<String> attrValue = entry.getValue();
            List<String> attrValueOut = new ArrayList<>();

            if (attrName.equals("tags") && attrValue.size() == 1) {
                // It's not a real tag list, but rather a string of comma-separated strings
                // replaceFirst: [tag1,tag2,tag3] -> "[tag1" "tag2" "tag3]" -> "tag1" "tag2" "tag3"
                // LinkedHashSet in between keeps order, but eliminates later duplicates
                attrValue = new ArrayList<>(new LinkedHashSet<>(Arrays.asList(
                        attrValue.get(0).replaceFirst("^\\[", "").replaceFirst("]$", "").split(",\\s*")))
                );
            }

            for (String v : attrValue) {
                // Strip surrounding single or double quotes
                v = v.replaceFirst("^(['\"])(.*)\\1", "$2");
                v = TextUtils.htmlEncode(v)
                        .replaceAll("(?<!-)---(?!-)", "&mdash;")
                        .replaceAll("(?<!-)--(?!-)", "&ndash;")
                        .trim();
                attrValueOut.add(HTML_TOKEN_ITEM_S + v + HTML_TOKEN_ITEM_E);
            }
            String tokenValue = TextUtils.join(HTML_TOKEN_DELIMITER, attrValueOut).replace("{{ attrName }}", attrName);

            // Replace "{{ <scope>>.<key> }}" tokens in note body
            for (String scope : YAML_FRONTMATTER_SCOPES.split(",\\s*")) {
                String token = "{{ " + scope + "." + attrName + " }}";
                markupReplaced = markupReplaced.replace(token, tokenValue.replace("{{ scope }}", scope));
            }
        }

        return markupReplaced;
    }

    // Extension to add line numbers to headings
    // ---------------------------------------------------------------------------------------------

    private static class LineNumberIdProvider implements AttributeProvider {
        @Override
        public void setAttributes(Node node, AttributablePart part, Attributes attributes) {
            final Document document = node.getDocument();
            final int lineNumber = document.getLineNumber(node.getStartOffset());
            attributes.addValue("line", "" + lineNumber);
        }
    }

    private static class LineNumberIdProviderFactory implements AttributeProviderFactory {

        @Override
        public Set<Class<? extends AttributeProviderFactory>> getAfterDependents() {
            return null;
        }

        @Override
        public Set<Class<? extends AttributeProviderFactory>> getBeforeDependents() {
            return null;
        }

        @Override
        public boolean affectsGlobalScope() {
            return false;
        }

        @Override
        public AttributeProvider create(LinkResolverContext context) {
            return new LineNumberIdProvider();
        }
    }

    private static class LineNumberIdExtension implements HtmlRenderer.HtmlRendererExtension {
        @Override
        public void rendererOptions(MutableDataHolder options) {
        }

        @Override
        public void extend(HtmlRenderer.Builder rendererBuilder, String rendererType) {
            rendererBuilder.attributeProviderFactory(new LineNumberIdProviderFactory());
        }

        public static HtmlRenderer.HtmlRendererExtension create() {
            return new LineNumberIdExtension();
        }
    }
}
