## Pulling via Docker
```powershell
docker compose -p kafka_chatter -f docker-compose.yml up -d

docker compose -p kafka_chatter -f docker-compose.yml down -v
```

```powershell
mkdir ".data/kafka_data", ".data/zookeeper_data"
icacls ".\.data\kafka_data" /grant Everyone:"(OI)(CI)F" /T
icacls ".\.data\zookeeper_data" /grant Everyone:"(OI)(CI)F" /T
```

## Creating topics by script
> [!NOTE]
> Scripts created or edited on Windows use CRLF (`\r\n`) line endings, which will cause `command not found` or syntax errors when executed inside a Linux environment (like a Docker container). You must convert these line endings to the standard Linux LF (`\n`) format before running the script.

```bash
# 1. Convert line endings from CRLF to LF
sed -i 's/\r$//' ../../opt/kafka/scripts/create-topics.sh

# 2. Execute the script
bash ../../opt/kafka/scripts/create-topics.sh

# optional: delete a topic
bash ../../bin/kafka-topics.sh --bootstrap-server localhost:5003 --delete --topic <topic_name>