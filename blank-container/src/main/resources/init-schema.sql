DROP SCHEMA IF EXISTS blank CASCADE;

CREATE SCHEMA blank;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE blank.blank
(
    id uuid NOT NULL,
    name character varying COLLATE pg_catalog."default" NULL,
    CONSTRAINT blank_pkey PRIMARY KEY (id)
);
