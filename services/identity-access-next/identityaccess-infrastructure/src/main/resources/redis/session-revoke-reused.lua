-- KEYS[1] replaced pointer of the presented credential (<prefix>session:replaced:<digest>)
-- ARGV[1] session hash key prefix, ARGV[2] now (epoch micros),
-- ARGV[3] revocation entry key prefix (ends with "access-revoked:"), ARGV[4] access credential lifetime (millis)
-- Revokes the session a replaced credential belonged to, only when the session still exists, is not revoked and has not
-- expired. In the same step it writes the revocation entry <prefix><userId>:<sessionId> that makes the session's
-- access credentials unusable until they expire on their own. Returns "<userId>:<sessionId>" when it revoked a session,
-- otherwise nil; a session that is already revoked, expired or gone is left as it is.
local sessionId = redis.call('GET', KEYS[1])
if not sessionId then
  return false
end

local key = ARGV[1] .. sessionId
if redis.call('EXISTS', key) == 0 then
  return false
end
if redis.call('HEXISTS', key, 'revokedAt') == 1 then
  return false
end

local now = tonumber(ARGV[2])
if now >= tonumber(redis.call('HGET', key, 'expiresAt')) then
  return false
end

local userId = redis.call('HGET', key, 'userId')
redis.call('HSET', key, 'revokedAt', ARGV[2])
redis.call('SET', ARGV[3] .. userId .. ':' .. sessionId, ARGV[2], 'PX', tonumber(ARGV[4]))
return userId .. ':' .. sessionId
