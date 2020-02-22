package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.*;

public class JdbcCrawlerDao implements CrawlerDao {
    public static final String JDBC_URL = "jdbc:h2:file:D://Leo/WorkSpace/hcsp/30_Chapter_Crawler/news";
    public static final String USER_NAME = "root";
    public static final String PASSWORD = "root";
    Connection connection = null;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection(JDBC_URL, USER_NAME, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException();
        }
    }

    public String getNextLinkThenDelete() throws SQLException {
        String link = getNextLink("SELECT link FROM NEWS.PUBLIC.LINKS_TO_BE_PROCESSED LIMIT 1");
        if (link != null) {
            deleteOrUpdateLinkFormDatabase("DELETE FROM NEWS.PUBLIC.LINKS_TO_BE_PROCESSED WHERE link = ?", link);
        }
        return link;
    }

    public String getNextLink(String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    public int deleteOrUpdateLinkFormDatabase(String sql, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            return statement.executeUpdate();
        }
    }

    public boolean isLinkProcessed(String link) throws SQLException {
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

    public void insertNew(String title, String content, String url) throws SQLException {
        String sql = "INSERT INTO NEWS.PUBLIC.NEWS (TITLE,CONTENT,URL,CREATED_AT,MODIFIED_AT) values(?,?,?,now(),now())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, content.toString());
            statement.setString(3, url);
            statement.executeUpdate();
        }
    }
}
