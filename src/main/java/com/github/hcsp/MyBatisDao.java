package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MyBatisDao implements CrawlerDao {
    public static final String NAME_SPACE = "com.github.hcsp.MyMapper.";
    private SqlSessionFactory sqlSessionFactory;

    public MyBatisDao() {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized String getNextLinkThenDelete() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = session.selectOne(NAME_SPACE + "selectNextAvailableLink");
            if (link != null) {
                session.delete(NAME_SPACE + "deleteProcessedLink", link);
            }
            return link;
        }
    }

    @Override
    public void insertNewIntoDatabase(String title, String content, String url) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert(NAME_SPACE + "insertNews", new News(title, content, url));
        }
    }

    @Override
    public boolean isLinkProcessed(String link) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int count = session.selectOne(NAME_SPACE + "countLink", link);
            return count > 0;
        }
    }

    private void insertLinkIntoDatabase(String tableName, String link) {
        Map<String, String> param = new HashMap<>();
        param.put("tableName", tableName);
        param.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert(NAME_SPACE + "insertLink", param);
        }
    }

    public void insertFailedLink(String link) throws SQLException {
        insertLinkIntoDatabase("links_failed_processed", link);
    }

    public void insertAlreadyProcessedLink(String link) throws SQLException {
        insertLinkIntoDatabase("links_already_processed", link);
    }

    public void insertToBeProcessedLink(String href) throws SQLException {
        insertLinkIntoDatabase("links_to_be_processed", href);
    }
}
