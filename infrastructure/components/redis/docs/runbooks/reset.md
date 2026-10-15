# Reset the shared Redis stack

This deletes every service's keys in the shared store.

1. Stop the stack: `docker compose -p redis_chatter -f docker-compose.yml down`
2. Delete the data folder: `rm -rf data/`
3. Start again: `docker compose -p redis_chatter -f docker-compose.yml up -d`
