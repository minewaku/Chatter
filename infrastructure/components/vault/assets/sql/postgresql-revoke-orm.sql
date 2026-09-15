-- Sang tên đổi chủ toàn bộ bảng/sequence do role này tạo ra cho tài khoản admin cố định ('vault' hoặc 'postgres')
REASSIGN OWNED BY "{{name}}" TO vault;

-- Xóa mọi quyền phụ thuộc còn lại
DROP OWNED BY "{{name}}";

-- Xóa role an toàn
DROP ROLE IF EXISTS "{{name}}";