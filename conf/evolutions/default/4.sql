-- Email confirmations schema

-- !Ups
CREATE TABLE emailConfirmations (
       id int PRIMARY KEY NOT NULL,
       userId int NOT NULL,
       key varchar UNIQUE NOT NULL,
       sentAt timestamp NOT NULL,
       responseReceivedAt timestamp
);

CREATE UNIQUE INDEX emailConfirmationsId ON emailConfirmations (id);
CREATE UNIQUE INDEX emailConfirmationsKey ON emailConfirmations (key);


-- !Downs
DROP TABLE emailConfirmations;
DROP INDEX IF EXISTS emailConfirmationsId;
DROP INDEX IF EXISTS emailConfirmationsKey;
