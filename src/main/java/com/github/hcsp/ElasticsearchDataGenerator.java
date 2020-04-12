package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

public class ElasticsearchDataGenerator {
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

    private static void mockData(SqlSessionFactory sqlSessionFactory, int i) {
    }
}
