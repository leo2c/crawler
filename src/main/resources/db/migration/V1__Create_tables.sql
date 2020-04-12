ALTER DATABASE NEWS CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
create table LINKS_TO_BE_PROCESSED
(
    link varchar(2000) null
) default charset = utf8mb4;
create table LINKS_ALREADY_PROCESSED
(
    link varchar(2000) null
) default charset = utf8mb4;
create table LINKS_FAILED_PROCESSED
(
    link varchar(2000) null
) default charset = utf8mb4;
create table NEWS
(
    id          bigint auto_increment,
    title       text          null,
    content     text          null,
    url         varchar(1000) null,
    created_at  timestamp     null,
    modified_at timestamp     null,
    constraint NEWS_pk
        primary key (id)
) default charset = utf8mb4;