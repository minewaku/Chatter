-- KEYS[1] session hash, ARGV[1] owner user id, ARGV[2] now (epoch micros)
-- Revokes the session only when it exists, belongs to the user and is not already revoked.
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
return 1
