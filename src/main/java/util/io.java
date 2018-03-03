package util;

import parser.RSSFeed;
import parser.RSSItem;

import javax.swing.plaf.nimbus.State;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

public class io {
    private Connection connection = null;
    public void connect(String connUrl){
        try{
            //Class.forName("org.postgresql.Driver");
            connection = DriverManager
                    .getConnection(connUrl,"postgres","admin");
            System.out.println("Database connect successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to connect database!");
        }
    }
    public void createTable(){
        if(connection == null) return;
        try {
            Statement statement = connection.createStatement();
            String CREATE_FEEDS_TABLE_STRING = "CREATE TABLE IF NOT EXISTS FEEDS " +
                    "(ID             SERIAL             ," +
                    " NAME           TEXT    UNIQUE     , " +
                    " SRC            TEXT    NOT NULL) ";
            String CREATE_ITEM_TABLE_STRING = "CREATE TABLE IF NOT EXISTS ITEMS " +
                    "(ID             SERIAL          ," +
                    " TITLE          TEXT    UNIQUE  ," +
                    " PUBLISHED    TIMESTAMP NOT NULL," +
                    " SRC            TEXT            ," +
                    " RAWCONTENT     TEXT    NOT NULL," +
                    " AUTHOR         TEXT            ," +
                    " FROMFEED       TEXT    NOT NULL)";
            String CREATE_FAVORITE_TABLE_STRING = "CREATE TABLE IF NOT EXISTS FAVORITES " +
                    "(ID             SERIAL          ," +
                    " TITLE          TEXT    UNIQUE  ," +
                    " PUBLISHED    TIMESTAMP NOT NULL," +
                    " SRC            TEXT            ," +
                    " RAWCONTENT     TEXT    NOT NULL," +
                    " AUTHOR         TEXT            ," +
                    " FROMFEED       TEXT    NOT NULL)";
            statement.executeUpdate(CREATE_FEEDS_TABLE_STRING);
            statement.executeUpdate(CREATE_ITEM_TABLE_STRING);
            statement.executeUpdate(CREATE_FAVORITE_TABLE_STRING);
            System.out.println("table link successfully");
        } catch (SQLException e) {
            System.out.println("Failed to create table.");
        }
    }
    public void insertFeed(RSSFeed feed){
        if(connection == null) return;
        try {
            PreparedStatement stm = connection.prepareStatement(
                    "INSERT INTO FEEDS (NAME, SRC)"
                            + "VALUES (?, ?) ON CONFLICT (NAME) DO NOTHING RETURNING ID");
            stm.setString(1,feed.name);
            stm.setString(2,feed.src);
            ResultSet rs = stm.executeQuery();
            System.out.println("Inserting "+ feed.name);
            rs.next();
            for(RSSItem item:feed.rssItems){
                insertItem(item, feed.name, false);
            }
        } catch (SQLException e) {
            System.out.println("Insert failed");
        }
    }
    public void removeFeed(String feed){
        if(connection == null) return;
        try {
            PreparedStatement stmRemoveItems = connection.prepareStatement("DELETE FROM ITEMS WHERE FROMFEED = ?");
            stmRemoveItems.setString(1,feed);
            PreparedStatement stmRemoveFeed = connection.prepareStatement("DELETE FROM FEEDS WHERE NAME = ?");
            stmRemoveFeed.setString(1,feed);
            stmRemoveItems.executeUpdate();
            stmRemoveFeed.executeUpdate();
            System.out.println("Removing " + feed);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    public void insertItem(RSSItem item, String fromFeed, boolean isFavorite){
        if(connection == null) return;
        try {
            PreparedStatement stm = isFavorite ? connection.prepareStatement(
                    "INSERT INTO FAVORITES (TITLE, PUBLISHED, SRC, RAWCONTENT, AUTHOR, FROMFEED)" +
                            "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (TITLE) DO NOTHING "):
                    connection.prepareStatement(
                            "INSERT INTO ITEMS (TITLE, PUBLISHED, SRC, RAWCONTENT, AUTHOR, FROMFEED)" +
                                    "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (TITLE) DO NOTHING ")
                    ;
            stm.setString(1,item.name);
            stm.setObject(2,LocalDateTime.ofInstant(item.publishedDate.toInstant(), ZoneId.systemDefault()));
            stm.setString(3,item.source);
            stm.setString(4,item.rawContent);
            stm.setString(5,item.author);
            stm.setString(6,fromFeed);
            stm.executeUpdate();
            System.out.println("Inserting "+ item.name);
        } catch (SQLException e) {
            System.out.println("Insert failed");
        }
    }
    public void removeDateBefore(int days){
        if (connection == null) return;
        try {
            Statement statement = connection.createStatement();
            String REMOVE_DATE_BEFORE_STRING = "DELETE FROM ITEMS\n" +
                    "WHERE PUBLISHED < CURRENT_TIMESTAMP - INTERVAL '"+ days +" days'";
            statement.executeUpdate(REMOVE_DATE_BEFORE_STRING);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    public ArrayList<RSSFeed> getFeeds(){
        if(connection == null) return null;
        ArrayList<RSSFeed> feeds = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery( "SELECT * FROM FEEDS;" );
            while (resultSet.next()){
                String name = resultSet.getString("name");
                String src = resultSet.getString("src");
                // fulfill items
                PreparedStatement itemStatement = connection.prepareStatement("SELECT * FROM ITEMS " +
                        "WHERE FROMFEED=? " +
                        "ORDER BY PUBLISHED DESC");
                itemStatement.setString(1,name);
                ResultSet itemSet = itemStatement.executeQuery();
                ArrayList<RSSItem> items = new ArrayList<>();
                while(itemSet.next()){
                    String _name = itemSet.getString("title");
                    LocalDateTime localDateTime = itemSet.getObject("published", LocalDateTime.class);
                    Date _date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                    String _src = itemSet.getString("src");
                    String _rawcontent = itemSet.getString("rawcontent");
                    String _author = itemSet.getString("author");
                    RSSItem _item = new RSSItem(_name, _date,_author, _rawcontent, _src);
                    items.add(_item);
                }
                feeds.add(new RSSFeed(name,src,items));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return feeds;
    }
    public ArrayList<RSSItem> getFavorites(){
        if (connection == null) return null;
        ArrayList<RSSItem> items = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet itemSet = statement.executeQuery("SELECT * FROM FAVORITES");
            while (itemSet.next()){
                String _name = itemSet.getString("title");
                LocalDateTime localDateTime = itemSet.getObject("published", LocalDateTime.class);
                Date _date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                String _src = itemSet.getString("src");
                String _rawcontent = itemSet.getString("rawcontent");
                String _author = itemSet.getString("author");
                RSSItem _item = new RSSItem(_name, _date,_author, _rawcontent, _src);
                items.add(_item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}
