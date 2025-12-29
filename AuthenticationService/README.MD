## Pulling via Docker
```powershell
docker compose -p authentication_service_chatter -f docker-compose-dev.yml up -d
```

## Access PosgresQL CLI
```bash
psql -h localhost -p 5440 -U admin -d chatter
```
INSERT INTO role (id, name, code, description, is_deleted, created_at) 
VALUES (1445089180648505345, 'admin', 'ADMIN', 'Admin role', false, NOW());

INSERT INTO user_role (user_id, role_id) VALUES (1454904067537010688, 1445089180648505345);



