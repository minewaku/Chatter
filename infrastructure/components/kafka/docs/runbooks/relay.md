# Identity-access CDC relay

Publishes the account-deleted announcement of `identity-access` to Kafka. The service writes a row to
`identity_access.outbox` and deletes it in the same transaction; this relay (Kafka Connect with the Debezium
PostgreSQL connector) reads the insert from the Postgres change log and publishes one message to
`identityaccess.user.v1`. Spec: `specs/013-account-deleted-outbox/`.

> Run from the repository root.

## Start order

1. **Credentials.** Put real values for `IDENTITYACCESS_DEBEZIUM_USERNAME` and `IDENTITYACCESS_DEBEZIUM_PASSWORD` in
   `infrastructure/components/postgresql/.env` (names are in `.env.example`; Vault is not involved).
2. **Postgres with logical decoding.** The compose file sets `wal_level=logical`; a change needs a restart (data is kept).
   See [../../../postgresql/README.md](../../../postgresql/README.md).
3. **Start `identity-access` once** so its migrations create `identity_access.outbox`.
4. **Relay role and publication.** `bash infrastructure/components/postgresql/scripts/create-relay-role.sh`
   (safe to repeat; see [relay-role.md](../../../postgresql/docs/runbooks/relay-role.md)).
5. **Kafka and Connect.** The compose file reads the two credentials from your shell:

   ```bash
   source infrastructure/shared/scripts/relay-env.sh
   docker compose -p kafka_chatter \
     --project-directory infrastructure/components/kafka \
     -f infrastructure/components/kafka/docker-compose.yml up -d
   ```

6. **Topics.** `bash infrastructure/components/kafka/scripts/create-topics.sh` (creates `identityaccess.user.v1`,
   `identityaccess.user.v1.profile-next.dlt` and `__debezium-heartbeat.identityaccess`). The topics must exist before
   the connector publishes.
7. **Connector.** `bash infrastructure/components/kafka/scripts/register-connectors.sh` (creates or updates; safe to repeat).

## Verify

```bash
curl -s localhost:5004/connectors/identity-access-outbox/status      # connector and task: RUNNING
curl -s localhost:5004/connectors/identity-access-outbox/config      # shows ${env:...} placeholders, no password
docker exec kafka-chatter kafka-console-consumer --bootstrap-server kafka-chatter:5003 \
  --topic identityaccess.user.v1 --from-beginning --max-messages 1 --timeout-ms 30000 \
  --property print.key=true --property print.headers=true
```

After closing an account you should see within seconds: key = user id, headers `id` and `eventType:UserSoftDeleted`,
value `{"eventId":"…","userId":"…","occurredAt":"…Z"}` (a JSON object, not an escaped string), and no empty record.

`bash infrastructure/components/kafka/scripts/soak-relay.sh 1000` stress-tests the relay with random broker and Connect
outages on an isolated topic; it must report `missing=0 leaked=0`.

## Watch the retained change log

While the relay is stopped, Postgres keeps WAL for its replication slot. Check how much:

```bash
docker exec postgresql-chatter psql -U admin -d chatter -c \
  "SELECT slot_name, active, pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn)) AS lag
   FROM pg_replication_slots WHERE slot_name = 'identity_access_outbox';"
```

If it keeps growing, start the relay again. If the relay is gone for good, drop the slot (below); announcements written
while no relay was reading are then lost.

## Reset

```bash
curl -s -X DELETE localhost:5004/connectors/identity-access-outbox
docker exec postgresql-chatter psql -U admin -d chatter -c "SELECT pg_drop_replication_slot('identity_access_outbox')"
docker exec postgresql-chatter psql -U admin -d chatter -c "DROP PUBLICATION IF EXISTS identity_access_outbox"
```

Then delete Connect's internal topics if you want a clean worker (`connect-configs`, `connect-offsets`, `connect-status`,
`__debezium-heartbeat.identityaccess`) with `kafka-topics --delete`, and start again from step 4.

## Old topics

`create-topics.sh` no longer creates the legacy identity-access and Debezium topics. Existing ones are not deleted
automatically; remove them by hand if you want:

```bash
for t in dev.shared.event.identityaccess.user dev.internal.event.identityaccess.outbox dev.internal.event.identityaccess.dlq \
         debezium-offsets.identityaccess debezium-offsets.profile debezium-offsets.message \
         debezium-schema-history.identityaccess debezium-schema-history.profile debezium-schema-history.message; do
  docker exec kafka-chatter kafka-topics --bootstrap-server kafka-chatter:5003 --delete --topic "$t" || true
done
```
