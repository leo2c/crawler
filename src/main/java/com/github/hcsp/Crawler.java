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

public class Crawler extends Thread {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.106 Safari/537.36";
    private CrawlerDao dao;

    public Crawler(CrawlerDao dao) {
        this.dao = dao;
    }

    public void run() {
        String link = null;
        try {
            while ((link = dao.getNextLinkThenDelete()) != null) {
                if (!dao.isLinkProcessed(link) && isInterestingLink(link)) {
                    System.out.println("link:" + link);
                    Document document = httpGetDocumentParseHtml(link);
                    parseUrlsFromPageAndStoreIntoDatabase(document);
                    storeIntoDatabaseIfItIsNewsPage(document, link);
                    dao.insertAlreadyProcessedLink(link);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            try {
                dao.insertFailedLink(link);
            } catch (SQLException ex) {
                throw new RuntimeException(e);
            }
            e.printStackTrace();
            System.out.println("链接解析失败:" + link);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document document) throws SQLException {
        for (Element aTag : document.select("a")) {
            String href = aTag.attr("href");
            dao.insertToBeProcessedLink(href);
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
                dao.insertNewIntoDatabase(title, content.toString(), url);
            }
        }
    }

    private Document httpGetDocumentParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        if (link.contains("\\/")) {
            link = link.replace("\\/", "/");
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
