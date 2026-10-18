-- Login role with superuser rights so the ORM can create keyspaces and tables automatically
CREATE ROLE '{{name}}' WITH PASSWORD = '{{password}}' AND LOGIN = true AND SUPERUSER = true;