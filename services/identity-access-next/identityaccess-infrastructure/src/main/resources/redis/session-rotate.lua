-- KEYS[1] session hash, KEYS[2] presented token pointer, KEYS[3] new token pointer
-- ARGV[1] session id, ARGV[2] new token digest, ARGV[3] now (epoch micros),
-- ARGV[4] new expiresAt (epoch micros), ARGV[5] key expiry (epoch millis = new session expiry + retention),
-- ARGV[6] per-user sorted set key prefix (the owner id is read from the session hash)
-- Renews the session only when it exists, is not revoked, has not expired, and the presented credential belongs to it:
-- the credential pointer moves to the new digest and the session gets the new expiry, so of two concurrent calls with
-- the same credential at most one succeeds. lastUsedAt never moves backwards. Returns 1 when renewed, otherwise 0.
if redis.call('EXISTS', KEYS[1]) == 0 then
  return 0
end
if redis.call('HEXISTS', KEYS[1], 'revokedAt') == 1 then
  return 0
end

local now = tonumber(ARGV[3])
local expiresAt = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
if now >= expiresAt then
  return 0
end
if redis.call('GET', KEYS[2]) ~= ARGV[1] then
  return 0
end

local expireAtMillis = tonumber(ARGV[5])
redis.call('HSET', KEYS[1], 'expiresAt', ARGV[4], 'tokenDigest', ARGV[2])
local lastUsedAt = redis.call('HGET', KEYS[1], 'lastUsedAt')
if (not lastUsedAt) or tonumber(lastUsedAt) < now then
  redis.call('HSET', KEYS[1], 'lastUsedAt', ARGV[3])
end
redis.call('PEXPIREAT', KEYS[1], expireAtMillis)

redis.call('DEL', KEYS[2])
redis.call('SET', KEYS[3], ARGV[1])
redis.call('PEXPIREAT', KEYS[3], expireAtMillis)

local userKey = ARGV[6] .. redis.call('HGET', KEYS[1], 'userId')
local current = redis.call('PEXPIRETIME', userKey)
if current < expireAtMillis then
  redis.call('PEXPIREAT', userKey, expireAtMillis)
end
return 1
