package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class Main {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.106 Safari/537.36";
    public static final String JDBC_URL = "jdbc:h2:file:D://Leo/WorkSpace/hcsp/30_Chapter_Crawler/news";
    public static final String USER_NAME = "root";
    public static final String PASSWORD = "root";

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws SQLException {
        Connection connection = DriverManager.getConnection(JDBC_URL, USER_NAME, PASSWORD);
        while (true) {
            List<String> linkPool = loadUrlsFromDatabase(connection, "SELECT link FROM NEWS.PUBLIC.LINKS_TO_BE_PROCESSED");
            if (linkPool.isEmpty()) {
                break;
            }
            String link = linkPool.get(linkPool.size() - 1);
            deleteOrUpdateLinkFormDatabase(connection, "DELETE FROM NEWS.PUBLIC.LINKS_TO_BE_PROCESSED WHERE link = ?", link);
            if (isInterestingLink(link) && (!isLinkProcessed(connection, link))) {
                try {
                    Document document = httpGetDocumentParseHtml(link);
                    parseUrlsFromPageAndStoreIntoDatabase(connection, document);
                    storeIntoDatabaseIfItIsNewsPage(connection, "INSERT INTO NEWS.PUBLIC.NEWS (title) values(?)", document);
                } catch (Exception e) {
                    deleteOrUpdateLinkFormDatabase(connection, "INSERT INTO NEWS.PUBLIC.LINKS_FAILED_PROCESSED (link) values(?)", link);
                    System.out.println("链接解析失败:" + link);
                } finally {
                    deleteOrUpdateLinkFormDatabase(connection, "INSERT INTO NEWS.PUBLIC.LINKS_ALREADY_PROCESSED (link) values(?)", link);
                }
            }
        }
    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document document) throws SQLException {
        for (Element aTag : document.select("a")) {
            String href = aTag.attr("href");
            deleteOrUpdateLinkFormDatabase(connection, "INSERT INTO NEWS.PUBLIC.LINKS_TO_BE_PROCESSED (link) values(?)", href);
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM NEWS.PUBLIC.LINKS_ALREADY_PROCESSED WHERE LINK = ?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    private static ArrayList<String> loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        ArrayList<String> linkPool = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                linkPool.add(resultSet.getString(1));
            }
        }
        return linkPool;
    }

    private static int deleteOrUpdateLinkFormDatabase(Connection connection, String sql, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            return statement.executeUpdate();
        }
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Connection connection, String sql, Document document) throws SQLException {
        ArrayList<Element> articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String text = articleTag.child(0).text();
                System.out.println(text);
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, text);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static Document httpGetDocumentParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        System.out.println("link:" + link);
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", USER_AGENT);
        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            return Jsoup.parse(html);
        }
    }

    public static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link);
    }

    public static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    public static boolean isIndexPage(String link) {
        return link.equals("https://sina.cn");
    }

    public static boolean isNotLoginPage(String link) {
        return !(link.contains("passport.sina.cn") || link.contains("passport.weibo.com"));
    }
}
