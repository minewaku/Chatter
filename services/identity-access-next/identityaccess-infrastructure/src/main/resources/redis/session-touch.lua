-- KEYS[1] session hash, ARGV[1] now (epoch micros)
-- Sets lastUsedAt = max(lastUsedAt, now) only when the session exists, is not revoked and has not expired.
if redis.call('EXISTS', KEYS[1]) == 0 then
  return 0
end
if redis.call('HEXISTS', KEYS[1], 'revokedAt') == 1 then
  return 0
end

local now = tonumber(ARGV[1])
local expiresAt = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
if now >= expiresAt then
  return 0
end

local lastUsedAt = redis.call('HGET', KEYS[1], 'lastUsedAt')
if (not lastUsedAt) or tonumber(lastUsedAt) < now then
  redis.call('HSET', KEYS[1], 'lastUsedAt', ARGV[1])
end
return 1
