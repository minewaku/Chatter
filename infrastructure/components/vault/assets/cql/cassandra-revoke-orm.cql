-- Dropping the role is enough; tables and data it created in the keyspace stay intact
DROP ROLE IF EXISTS '{{name}}';