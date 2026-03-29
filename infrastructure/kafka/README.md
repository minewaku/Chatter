## Pulling via Docker
```powershell
docker compose -p kafka_chatter -f docker-compose.yml up -d
```

```bash
sed -i 's/\r$//' ../../opt/kafka/scripts/create-topics.sh
```

## Creating topics by script
> [!NOTE]
> Scripts created or edited on Windows use CRLF (`\r\n`) line endings, which will cause `command not found` or syntax errors when executed inside a Linux environment (like a Docker container). You must convert these line endings to the standard Linux LF (`\n`) format before running the script.

```bash
# 1. Convert line endings from CRLF to LF
sed -i 's/\r$//' ../../opt/kafka/scripts/create-topics.sh

# 2. Execute the script
bash ../../opt/kafka/scripts/create-topics.sh