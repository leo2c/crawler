package com.github.hcsp;

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
import java.util.stream.Collectors;

public class Crawler {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.106 Safari/537.36";
    private CrawlerDao dao = new JdbcCrawlerDao();


    public void main(String[] args) throws SQLException {
        String link;
        while ((link = dao.getNextLinkThenDelete()) != null) {
            if (!dao.isLinkProcessed(link)) {
                try {
                    System.out.println("link:" + link);
                    Document document = httpGetDocumentParseHtml(link);
                    parseUrlsFromPageAndStoreIntoDatabase(document);
                    storeIntoDatabaseIfItIsNewsPage(document, link);
                } catch (IOException e) {
                    dao.deleteOrUpdateLinkFormDatabase("INSERT INTO NEWS.PUBLIC.LINKS_FAILED_PROCESSED (link) values(?)", link);
                    e.printStackTrace();
                    System.out.println("链接解析失败:" + link);
                } finally {
                    dao.deleteOrUpdateLinkFormDatabase("INSERT INTO NEWS.PUBLIC.LINKS_ALREADY_PROCESSED (link) values(?)", link);
                }
            }
        }
    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document document) throws SQLException {
        for (Element aTag : document.select("a")) {
            String href = aTag.attr("href");
            if (isInterestingLink(href)) {
                dao.deleteOrUpdateLinkFormDatabase("INSERT INTO NEWS.PUBLIC.LINKS_TO_BE_PROCESSED (link) values(?)", href);
            }
        }
    }

    private void storeIntoDatabaseIfItIsNewsPage(Document document, String url) throws SQLException {
        ArrayList<Element> articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.child(0).text();
                ArrayList<Element> sections = articleTag.select("section");
                StringBuilder content = new StringBuilder();
                for (Element sectionTag : sections) {
                    content.append(sectionTag.select("p").stream().map(Element::text).collect(Collectors.joining("\r\n")));
                }
                System.out.println(title);
                dao.insertNew(title, content.toString(), url);
            }
        }
    }

    private static Document httpGetDocumentParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", USER_AGENT);
        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            return Jsoup.parse(html);
        }
    }

    public static boolean isInterestingLink(String link) {
        return isNewsPage(link) && isNotNewsIndex(link);
    }

    public static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotNewsIndex(String link) {
        return !link.equals("https://news.sina.cn/");
    }

}
