package parser;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;

import java.util.ArrayList;

/**
 * The type Rss feed.
 */
public class RSSFeed {
    /**
     * The Name.
     */
    public String name;
    /**
     * The Rss items.
     */
    public ArrayList<RSSItem> rssItems = new ArrayList<>();

    /**
     * Instantiates a new Rss feed.
     *
     * @param feed   the feed
     * @param source the source
     */
    public RSSFeed(SyndFeed feed, String source) {
        this.name = feed.getTitle();
        this.src = source;
        for(SyndEntry entry: feed.getEntries()){
          this.rssItems.add(new RSSItem(entry));
        }
    }

    /**
     * Instantiates a new Rss feed.
     *
     * @param name     the name
     * @param src      the src
     * @param rssItems the rss items
     */
    public RSSFeed(String name, String src, ArrayList<RSSItem> rssItems) {
        this.name = name;
        this.rssItems = rssItems;
        this.src = src;
    }

    /**
     * The Source.
     */
    public String src;

    @Override
    public String toString() {
        return name;
    }
}
