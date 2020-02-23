package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkThenDelete() throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void insertNewIntoDatabase(String title, String content, String url) throws SQLException;

    void insertFailedLink(String link) throws SQLException;

    void insertAlreadyProcessedLink(String link) throws SQLException;

    void insertToBeProcessedLink(String href) throws SQLException;

}
