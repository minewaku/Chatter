-- Chỉ cần xóa role, các bảng/dữ liệu do role này tạo ra trong keyspace vẫn an toàn
DROP ROLE IF EXISTS '{{name}}';