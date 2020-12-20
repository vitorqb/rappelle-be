-- User schema

-- !Ups
CREATE TABLE users (
       id int PRIMARY KEY NOT NULL,
       email varchar UNIQUE NOT NULL,
       passwordHash varchar NOT NULL
);

-- !Downs
DROP TABLE users;
