-- KEYS[1] session hash, ARGV[1] owner user id, ARGV[2] now (epoch micros), ARGV[3] session id,
-- ARGV[4] revocation entry key prefix (ends with "access-revoked:"), ARGV[5] access credential lifetime (millis)
-- Revokes the session only when it exists, belongs to the user and is not already revoked. In the same step it writes
-- the revocation entry <prefix><userId>:<sessionId> that makes the session's access credentials unusable until they
-- expire on their own; a session that is not newly revoked gets no entry and no lifetime reset.
if redis.call('EXISTS', KEYS[1]) == 0 then
  return 0
end
if redis.call('HGET', KEYS[1], 'userId') ~= ARGV[1] then
  return 0
end
if redis.call('HEXISTS', KEYS[1], 'revokedAt') == 1 then
  return 0
end
redis.call('HSET', KEYS[1], 'revokedAt', ARGV[2])
redis.call('SET', ARGV[4] .. ARGV[1] .. ':' .. ARGV[3], ARGV[2], 'PX', tonumber(ARGV[5]))
return 1
