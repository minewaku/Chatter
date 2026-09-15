CREATE ROLE "{{name}}"
  WITH LOGIN
       PASSWORD '{{password}}'
       REPLICATION
       SUPERUSER
       VALID UNTIL '{{expiration}}';
