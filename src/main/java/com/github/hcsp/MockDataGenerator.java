package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {
    public static final String NAME_SPACE = "com.github.hcsp.MockMapper.";

    public static void main(String[] args) {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            mockData(sqlSessionFactory, 2000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void mockData(SqlSessionFactory sqlSessionFactory, int howMany) {
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            List<News> currentNews = session.selectList(NAME_SPACE + "selectNews");
            int count = howMany - currentNews.size();
            Random random = new Random();
            try {
                while (count-- > 0) {
                    int index = random.nextInt(currentNews.size() - 1);
                    News NewsToBeInserted = currentNews.get(index);
                    Instant randomTime = Instant.now().minusSeconds(random.nextInt(3600 * 24 * 365));
                    NewsToBeInserted.setCreatedAt(randomTime);
                    NewsToBeInserted.setModifiedAt(randomTime);
                    if (NewsToBeInserted.getContent().length() > 20) {
                        NewsToBeInserted.setContent(NewsToBeInserted.getContent().substring(0, 20));
                    }
                    session.insert(NAME_SPACE + "insertNews", NewsToBeInserted);
                    Double rate = (1 - ((double) count / howMany)) * 100;
                    System.out.println("Degree of completion:" + String.format("%.2f", rate) + "%");
                    if (count % 2000 == 0) {
                        session.flushStatements();
                    }
                }
                session.commit();
            } catch (Exception e) {
                session.rollback();
                throw new RuntimeException(e);
            }
        }
    }
}