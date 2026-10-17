-- KEYS[1] per-user sorted set of session ids
-- ARGV[1] session hash key prefix, ARGV[2] session id to keep ("" keeps none), ARGV[3] now (epoch micros),
-- ARGV[4] user id, ARGV[5] revocation entry key prefix (ends with "access-revoked:"),
-- ARGV[6] access credential lifetime (millis)
-- Revokes every retained, not yet revoked session of the user except the kept one and, in the same step, writes a
-- revocation entry <prefix><userId>:<sessionId> for each session newly revoked. Returns how many were revoked.
local ids = redis.call('ZRANGE', KEYS[1], 0, -1)
local revoked = 0
for _, id in ipairs(ids) do
  if id ~= ARGV[2] then
    local key = ARGV[1] .. id
    if redis.call('EXISTS', key) == 1 and redis.call('HEXISTS', key, 'revokedAt') == 0 then
      redis.call('HSET', key, 'revokedAt', ARGV[3])
      redis.call('SET', ARGV[5] .. ARGV[4] .. ':' .. id, ARGV[3], 'PX', tonumber(ARGV[6]))
      revoked = revoked + 1
    end
  end
end
return revoked
