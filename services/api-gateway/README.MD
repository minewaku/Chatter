## Pulling via Docker
```powershell
docker compose -p apigateway_chatter -f docker-compose.yml up -d
```

**Improvement notes:**
- Consider removing the Vault key rotation logic, the Vault database engine doesn't support redis
