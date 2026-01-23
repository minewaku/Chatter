-- PostgreSQL init script (idempotent)

-------------------------------
-- 1. Create vault user (superuser)
-------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'vault') THEN
    CREATE ROLE vault WITH LOGIN PASSWORD 'QIUbomaKaq463g25' SUPERUSER;
  ELSE
    ALTER ROLE vault WITH PASSWORD 'QIUbomaKaq463g25';
  END IF;
END
$$;

-------------------------------
-- 2. Create dummy user
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
-- 3. Create debezium user & Config Permissions (QUAN TRỌNG)
-------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'debezium') THEN
    CREATE ROLE debezium WITH LOGIN REPLICATION PASSWORD '5gtsjWos5y1NutTw';
  ELSE
    ALTER ROLE debezium WITH REPLICATION PASSWORD '5gtsjWos5y1NutTw';
  END IF;


  EXECUTE 'GRANT pg_read_all_data TO debezium';
  EXECUTE 'GRANT USAGE ON SCHEMA public TO debezium';

  IF NOT EXISTS (SELECT FROM pg_publication WHERE pubname = 'dbz_publication') THEN
    EXECUTE 'CREATE PUBLICATION dbz_publication FOR ALL TABLES';
    EXECUTE 'ALTER PUBLICATION dbz_publication OWNER TO debezium';
  END IF;
END
$$;

-------------------------------
-- 4. System variables table
-------------------------------
CREATE TABLE IF NOT EXISTS system_variables (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

-- Lưu ý: Kiểm tra server_id
INSERT INTO system_variables (key, value)
VALUES ('server_id', 9)
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;