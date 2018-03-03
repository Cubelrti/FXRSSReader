package parser;

import com.rometools.rome.feed.synd.SyndEntry;

import java.util.Date;

/**
 * The type Rss item.
 */
public class RSSItem {
    /**
     * The Name.
     */
    public String name;
    /**
     * The Content.
     */
    public String content;
    /**
     * The Published date.
     */
    public Date publishedDate;
    /**
     * The Author.
     */
    public String author;
    /**
     * The Raw content.
     */
    public String rawContent;
    /**
     * The Source.
     */
    public String source;

    /**
     * Instantiates a new Rss item.
     *
     * @param entry the entry
     */
    public RSSItem(SyndEntry entry) {
        name = entry.getTitle();
        publishedDate = entry.getPublishedDate();
        if(publishedDate == null) {
            publishedDate = new Date();
        }
        rawContent = entry.getDescription().getValue();
        source = entry.getLink();
        author = entry.getAuthor();
        initContent();
    }

    private void initContent() {
        // parsing title to content
        final String title = "<h1> "+ name + "</h1>\n";
        final String _date = "<span>"+ publishedDate +" </span>";
        final String breaker = "<hr/>";
        final String openInBrowser = "\n<a id=\"external\" href=\""+source+"\"> Open in local browser</a>";
        this.content = title + _date + breaker + wrapInDiv(rawContent) + openInBrowser;
    }

    private String wrapInDiv(String content){
        return "<div>" + content + "</div>";
    }

    /**
     * Instantiates a new Rss item.
     *
     * @param name          the name
     * @param publishedDate the published date
     * @param author        the author
     * @param rawContent    the raw content
     * @param source        the source
     */
    public RSSItem(String name, Date publishedDate, String author, String rawContent, String source) {
        this.name = name;
        this.publishedDate = publishedDate;
        this.author = author;
        this.rawContent = rawContent;
        this.source = source;
        initContent();
    }

    @Override
    public String toString() {
        return name;
    }
}
