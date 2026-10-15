# Reset the shared PostgreSQL stack

This deletes every service's data in the shared database.

1. Stop the stack: `docker compose -p postgresql_chatter -f docker-compose.yml down`
2. Delete the data folder: `rm -rf data/`
3. Start again: `docker compose -p postgresql_chatter -f docker-compose.yml up -d`

Services recreate their schemas through their Flyway migrations on next start.
