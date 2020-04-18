package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
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
            mockData(sqlSessionFactory, 100_0000);
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
                    Instant createdAt = NewsToBeInserted.getCreatedAt();
                    Instant randomTime = createdAt.minusSeconds(random.nextInt(3600 * 24 * 365));
                    NewsToBeInserted.setCreatedAt(randomTime);
                    NewsToBeInserted.setModifiedAt(randomTime);
                    session.insert(NAME_SPACE + "insertNews", NewsToBeInserted);
                    Double rate = (double) count / howMany;
                    System.out.println("Degree of completion:" + new DecimalFormat("#.00").format(rate) + "%");
                    if (count % 2000 == 0){
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
