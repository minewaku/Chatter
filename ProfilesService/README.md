## Pulling via Docker
```powershell
docker compose -p profiles_service_chatter -f docker-compose-dev.yml up -d
```

## Access PosgresQL CLI
```bash
psql -h localhost -p 5441 -U admin -d chatter
```