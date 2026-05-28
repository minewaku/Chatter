
CREATE KEYSPACE IF NOT EXISTS chatter 
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

-- ---------------------------------------------------------
-- 1. Create vault user (superuser)
-- ---------------------------------------------------------
CREATE ROLE IF NOT EXISTS vault WITH PASSWORD = 'SelJiVviktHEgiYO' AND LOGIN = true AND SUPERUSER = true;

-- Chạy thêm lệnh ALTER để đảm bảo password được cập nhật nếu role đã tồn tại trước đó
ALTER ROLE vault WITH PASSWORD = 'SelJiVviktHEgiYO';


-- ---------------------------------------------------------
-- 2. Create dummy user
-- ---------------------------------------------------------
CREATE ROLE IF NOT EXISTS dummy WITH PASSWORD = '123456' AND LOGIN = true;

ALTER ROLE dummy WITH PASSWORD = '123456';


-- ---------------------------------------------------------
-- 3. Create debezium user & Config Permissions (QUAN TRỌNG)
-- ---------------------------------------------------------
CREATE ROLE IF NOT EXISTS debezium WITH PASSWORD = 'eo6wd9bZIbX6F2kr' AND LOGIN = true;

ALTER ROLE debezium WITH PASSWORD = 'eo6wd9bZIbX6F2kr';

-- Tương đương với pg_read_all_data: Cho phép Debezium quét tất cả keyspaces (bao gồm schema và cdc log)
GRANT SELECT ON ALL KEYSPACES TO debezium;
