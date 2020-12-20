-- Auth token schema

-- !Ups
CREATE TABLE authTokens (
       id int PRIMARY KEY NOT NULL,
       userId int NOT NULL,
       expiresAt timestamp NOT NULL,
       tokenValue varchar(256) NOT NULL UNIQUE
);

CREATE UNIQUE INDEX authTokensIdIndex ON authTokens (id);
CREATE UNIQUE INDEX tokenValueIndex ON authTokens (tokenValue);

-- !Downs
DROP TABLE IF EXISTS authTokens;
DROP INDEX IF EXISTS authTokensIdIndex;
DROP INDEX IF EXISTS tokenValueIndex;
