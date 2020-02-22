package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkThenDelete() throws SQLException;

    String getNextLink(String sql) throws SQLException;

    int deleteOrUpdateLinkFormDatabase(String sql, String link) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertNew(String title, String content, String url) throws SQLException;
}
