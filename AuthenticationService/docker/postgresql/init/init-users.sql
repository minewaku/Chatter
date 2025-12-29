-- PostgreSQL init script (idempotent)

-------------------------------
-- Create vault user (superuser)
-------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'vault') THEN
    CREATE ROLE vault WITH LOGIN PASSWORD 'FT6Qwf4NqRRT9BhA' SUPERUSER;
  ELSE
    ALTER ROLE vault WITH PASSWORD 'FT6Qwf4NqRRT9BhA';
  END IF;
END
$$;

-------------------------------
-- Create dummy user
-------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'dummy') THEN
    CREATE ROLE dummy WITH LOGIN PASSWORD '123456';
  ELSE
    ALTER ROLE dummy WITH PASSWORD '123456';
  END IF;
END
$$;

-------------------------------
-- Create debezium user
-------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'debezium') THEN
    CREATE ROLE debezium WITH LOGIN REPLICATION PASSWORD 'Yu28OtptFv20rEkG';
  ELSE
    ALTER ROLE debezium WITH REPLICATION PASSWORD 'Yu28OtptFv20rEkG';
  END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO debezium;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO debezium;

-------------------------------
-- System variables table
-------------------------------
CREATE TABLE IF NOT EXISTS system_variables (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

INSERT INTO system_variables (key, value)
VALUES ('server_id', 3)
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;
