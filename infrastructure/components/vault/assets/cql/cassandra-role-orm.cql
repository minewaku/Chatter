-- Tạo role với quyền đăng nhập và Superuser (phục vụ cho ORM tự động tạo bảng/keyspace)
CREATE ROLE '{{name}}' WITH PASSWORD = '{{password}}' AND LOGIN = true AND SUPERUSER = true;