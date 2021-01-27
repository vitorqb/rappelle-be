-- Reminders confirmations schema

-- !Ups
CREATE TABLE reminders (
       id int PRIMARY KEY NOT NULL,
       userId int NOT NULL,
       title varchar UNIQUE NOT NULL,
       datetime timestamp NOT NULL
);

CREATE UNIQUE INDEX remindersId ON reminders (id);
CREATE UNIQUE INDEX reminders_userId ON reminders (userId);


-- !Downs
DROP TABLE reminders;
DROP INDEX IF EXISTS remindersId;
