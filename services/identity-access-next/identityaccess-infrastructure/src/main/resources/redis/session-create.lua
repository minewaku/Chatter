-- KEYS[1] session hash, KEYS[2] token pointer, KEYS[3] per-user sorted set
-- ARGV[1] session id, ARGV[2] token digest, ARGV[3] issuedAt (epoch micros, the sorted-set score),
-- ARGV[4] key expiry (epoch millis = session expiry + retention), ARGV[5..] alternating hash fields and values
local sessionKey = KEYS[1]
local tokenKey = KEYS[2]
local userKey = KEYS[3]
local expireAtMillis = tonumber(ARGV[4])

for i = 5, #ARGV, 2 do
  redis.call('HSET', sessionKey, ARGV[i], ARGV[i + 1])
end
redis.call('PEXPIREAT', sessionKey, expireAtMillis)

redis.call('SET', tokenKey, ARGV[1])
redis.call('PEXPIREAT', tokenKey, expireAtMillis)

redis.call('ZADD', userKey, tonumber(ARGV[3]), ARGV[1])
-- the per-user index lives as long as its longest-retained session
local current = redis.call('PEXPIRETIME', userKey)
if current < expireAtMillis then
  redis.call('PEXPIREAT', userKey, expireAtMillis)
end
return 1
