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
- implement implicit exceptiop types for message domain models
- implement scheduler for cleaning /temp folder in file storage
- check lai session_id trong init schema
- all delete use cases have to fire an integration event for cleaning up associating data
- implement resilient4j

-readMessage(serverId) (IS_MEMBER)
-addMessage(serverId, message) (IS_MEMBER)
-addMember(serverId, userId) (IS_OWNER)

-


## Access redis CLI
```bash
psql -h localhost -p 5440 -U admin -d chatter
redis-cli -h localhost -p 6387 --user vault -a XIqnIWD0DRb7Axwg
```

```bash
psql -h localhost -p 5441 -U admin -d chatter
redis-cli -h localhost -p 6388 --user vault -a FcDSikLxXFLaf6KN
```

```bash
psql -h localhost -p 5442 -U admin -d chatter
redis-cli -h localhost -p 6389 --user vault -a VDPduQr6lFGx9rrp
AUTH vault VDPduQr6lFGx9rrp
```

## Access PosgresQL CLI
```bash
psql -h localhost -p 5440 -U admin -d chatter
psql -h localhost -p 5441 -U admin -d chatter
psql -h localhost -p 5442 -U admin -d chatter
psql -h localhost -p 5442 -U vault -d chatter

redis-cli -h 127.0.0.1 -p 6387 --user vault -a XIqnIWD0DRb7Axwg
redis-cli -h localhost -p 6380 --user vault --pass coFY4E7Nultq6lxM
redis-cli -h localhost -p 6387 --user vault --pass XIqnIWD0DRb7Axwg
redis-cli -h localhost -p 6388 --user vault --pass FcDSikLxXFLaf6KN
```

## Access ScyllaDB CLI
```bash
cqlsh localhost 9052 -u cassandra -p cassandra
cqlsh localhost 9052 -u cassandra -p cassandra -f /scripts/init-roles.cql
cqlsh 172.18.0.3 9052 -u cassandra -p cassandra -f /scripts/init-roles.cql
cqlsh 172.18.0.2 9052 -u cassandra -p cassandra
docker exec -it message-scylla-chatter nodetool status
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

Server
-createServer
-addChannel(ChannelType(voice, chat))
-addBanner()
-deleteChannel()
-createInviteLink()

Channel

Member
-kickMember()
-joinServer()
-leaveServer()

---
Message


net stop winnat
net start winnat