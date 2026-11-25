# Dead-letter topic: inspect and replay

`profile` reads account-deleted notices from `identityaccess.user.v1` (group `profile`). A notice it cannot process is not dropped and does not block the ones behind it: it is published to **`identityaccess.user.v1.profile.dlt`**, on the same partition, with the key and headers of the original plus the failure headers Spring Kafka adds (`kafka_dlt-exception-fqcn`, `kafka_dlt-exception-message`, `kafka_dlt-original-topic`, `kafka_dlt-original-offset`).

Two things land there:

| Cause | Retried first? | Typical fix |
|---|---|---|
| The value is not readable (not JSON, a field missing, `userId` not a positive whole number) | No, straight to the dead-letter topic | Usually nothing to replay: the producer sent something wrong. Fix the producer; replay only a corrected copy |
| Processing kept failing (for example the database was down) for about 2 minutes (1 s backoff, doubling, 30 s cap) | Yes | Fix the cause, then replay |

The notice processing is idempotent (deleting a profile that is already gone is success), so **a replay is safe**, also if the notice was in fact applied before.

> Run from the repository root. The broker is the `kafka-chatter` container (see [relay.md](relay.md) for how it is started).

## Inspect

```bash
docker exec kafka-chatter kafka-console-consumer --bootstrap-server kafka-chatter:5003 \
  --topic identityaccess.user.v1.profile.dlt --from-beginning --timeout-ms 10000 \
  --property print.key=true --property print.headers=true --property print.partition=true
```

The key is the user id; the headers include the original `eventType` and `id`, and why it failed. To count what is waiting:

```bash
docker exec kafka-chatter kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list kafka-chatter:5003 --topic identityaccess.user.v1.profile.dlt
```

(The topic is never trimmed by the service; the offsets are the total ever dead-lettered.)

## Replay one notice

Publish the record back to `identityaccess.user.v1` with **the same key and headers**. The key keeps it on the same partition, so it stays in order with that user's other notices; `eventType` must be `UserSoftDeleted` or the listener skips it.

The console producer reads one line per record as `headers<TAB>key<TAB>value` when `parse.headers` and `parse.key` are on (headers are `name:value` pairs joined by commas):

```bash
printf 'eventType:UserSoftDeleted,id:0197...\t123456789\t{"eventId":"0197...","userId":"123456789","occurredAt":"2026-10-29T08:15:30Z"}\n' | \
docker exec -i kafka-chatter kafka-console-producer --bootstrap-server kafka-chatter:5003 \
  --topic identityaccess.user.v1 \
  --property parse.headers=true --property parse.key=true
```

Take the value and `eventId` from the dead-lettered record, not from memory.

## Replay everything since a point in time

Resetting the consumer group re-reads the main topic, which re-applies every notice since that point. Because processing is idempotent this is safe, but stop `profile` first (a group with active members cannot be reset):

```bash
docker exec kafka-chatter kafka-consumer-groups --bootstrap-server kafka-chatter:5003 \
  --group profile --topic identityaccess.user.v1 --reset-offsets --to-datetime 2026-10-29T00:00:00.000 --dry-run
# review the plan, then repeat with --execute instead of --dry-run
```

Use `--to-earliest` for everything the topic still holds, or `--shift-by -N` for the last N records per partition.

## Verify the outcome

```bash
docker exec kafka-chatter kafka-consumer-groups --bootstrap-server kafka-chatter:5003 --group profile --describe
```

`LAG` should fall to 0. Then read the user's profile (`GET /api/v1/profiles/{id}` returns defaults once the row is gone) and, for the images, check `profile.pending_image_removal` is draining.

## If the dead-letter topic is missing

The service cannot dead-letter without it, and the failing record would be retried in a loop. Create both topics with `bash infrastructure/components/kafka/scripts/create-topics.sh` (3 partitions each: the dead letter goes to the same partition number as the original).
