package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static final String ROOT_HTML = "https://sina.cn";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.106 Safari/537.36";

    public static void main(String[] args) throws IOException {
        List<String> linkPool = new ArrayList<>();
        Set<String> processedLinks = new HashSet<>();
        linkPool.add(ROOT_HTML);
        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }
            // ArrayList从尾部删除更有效率
            String link = linkPool.remove(linkPool.size() - 1);
            //是否处理过了
            if (processedLinks.contains(link)) {
                continue;
            }
            if (isInterestingLink(link)) {
                try {
                    Document document = httpGetDocumentParseHtml(link);
                    document.select("a").stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);
                    storeIntoDatabaseIfItIsNewsPage(document);
                } catch (Exception e) {
                    System.out.println("链接解析失败:" + link);
                } finally {
                    processedLinks.add(link);
                }
            }
        }
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Document document) {
        ArrayList<Element> articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTag.child(0).text();
                System.out.println(title);
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
