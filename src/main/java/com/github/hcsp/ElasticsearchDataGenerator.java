package com.github.hcsp;

import org.apache.http.HttpHost;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchDataGenerator {
    public static final String NAME_SPACE = "com.github.hcsp.MockMapper.";

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<News> newsFromMySQL = getNewsFromMySQL(sqlSessionFactory);
        for (int i = 0; i < 20; i++) {
            new Thread(() -> writeSingleThread(newsFromMySQL)).start();
        }
    }

    private static void writeSingleThread(List<News> newsFromMySQL) {
        try (RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http"))
        )) {
            //single thread write 2000*1000 = 200_0000 data
            for (int i = 0; i < 1000; i++) {
                BulkRequest bulkRequest = new BulkRequest();
                for (News news : newsFromMySQL
                ) {
                    IndexRequest request = new IndexRequest("news");

                    Map<String, Object> data = new HashMap<>();
                    data.put("content", news.getContent().length() > 10 ? news.getContent().substring(0, 10) : news.getContent());
                    data.put("url", news.getUrl());
                    data.put("title", news.getTitle());
                    data.put("createdAt", news.getCreatedAt());
                    data.put("modifiedAt", news.getModifiedAt());

                    request.source(data, XContentType.JSON);

                    bulkRequest.add(request);
                    //IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                    //System.out.println(response.status().getStatus());
                }
                BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                System.out.println("Current thread:" + Thread.currentThread().getName() + " finishes " + i + ":" + bulkResponse.status().getStatus());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<News> getNewsFromMySQL(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList(NAME_SPACE + "selectNews");
        }
    }
}
