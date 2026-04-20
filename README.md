## HOW TO BUILD?


# Build the entire project (include all child services)
```bash
.\mvnw clean install
```

# Build the specific service
```bash
.\mvnw clean package -pl services/service-registry
```

## DEV NOTE
- Since soft-delete is treated as a domain concept, we need to implement user anonymization (e.g., clearing all fields and masking the username to something like DeletedUser#123).
    + Add an anonymize() method to the User domain model.
    + Create an AnonymizeExpiredUsersUseCase (Application Service).
    + Implement a scheduler in the Infrastructure layer to periodically trigger the anonymization use case.
    + Introduce a state indicator (e.g., a Status enum or isDeleted boolean) in both the User domain model and JPA entity. When a user's account recovery grace period expires, update this state to mark them as fully deleted/anonymized. Introduce specific repository methods (like findByEmailAndDeletedFalse()) and refactor existing Use Cases to strictly filter out deleted users where necessary.
    + Consider the affect of anonymization status for all other services (like may a anonymize user dont own a profile)
- Remove the Cerbos, each service now have to manage it own policies (implement policies as bussiness logics)
- standardize all serialization

-readMessage(serverId) (IS_MEMBER)
-addMessage(serverId, message) (IS_MEMBER)
-addMember(serverId, userId) (IS_OWNER)
-


## Access PosgresQL CLI
```bash
psql -h localhost -p 5440 -U admin -d chatter
redis-cli -h localhost -p 6387 --user vault -a XIqnIWD0DRb7Axwg
```

## Access PosgresQL CLI
```bash
psql -h localhost -p 5440 -U admin -d chatter
psql -h localhost -p 5441 -U admin -d chatter

redis-cli -h 127.0.0.1 -p 6387 --user vault -a XIqnIWD0DRb7Axwg
```

INSERT INTO role (id, name, code, description, is_deleted, created_at) 
VALUES (1445089180648505345, 'admin', 'ADMIN', 'Admin role', false, NOW());

INSERT INTO user_role (user_id, role_id) VALUES (1462087179677237248, 1445089180648505345);



DO $$ 
DECLARE 
    r RECORD;
BEGIN 
    -- Tìm tất cả các user do Vault tự động tạo ra (bắt đầu bằng v-root-)
    FOR r IN SELECT rolname FROM pg_roles WHERE rolname LIKE 'v-root-%' 
    LOOP 
        -- 1. Chuyển quyền sở hữu các bảng/object từ user Vault sang user admin
        EXECUTE 'REASSIGN OWNED BY "' || r.rolname || '" TO admin;';
        
        -- 2. Gỡ bỏ mọi quyền hạn (privileges) còn sót lại của user Vault
        EXECUTE 'DROP OWNED BY "' || r.rolname || '";';
        
        -- 3. Tiêu diệt user Vault
        EXECUTE 'DROP ROLE "' || r.rolname || '";';
        
        RAISE NOTICE 'Đã dọn dẹp và xóa role: %', r.rolname;
    END LOOP; 
END $$; 