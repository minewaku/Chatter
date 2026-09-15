CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';
GRANT approle TO '{{name}}'@'%';
SET DEFAULT ROLE approle TO '{{name}}'@'%';
