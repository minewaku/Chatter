## Pulling via Docker
```powershell
docker compose -p kafka_service_chatter -f docker-compose.yml up -d
```

```bash
sed -i 's/\r$//' ../../opt/kafka/scripts/create-topics.sh
```

## Creating topics by script
```bash
bash ../../opt/kafka/scripts/create-topics.sh
```

```do not run, just test
kafka-console-consumer \
  --bootstrap-server kafka:5003 \
  --topic dev.shared.cdc.authorization.user-role.id \
  --group dev.com.minewaku.chatter.authorization.authorization-service.user.role
```
