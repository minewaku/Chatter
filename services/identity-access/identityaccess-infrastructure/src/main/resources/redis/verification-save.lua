-- KEYS[1] token hash key, KEYS[2] per-user pointer key
-- ARGV[1] expiry (epoch millis), ARGV[2] token digest, ARGV[3] "1" to (re)point the user key at this token,
-- ARGV[4..] alternating hash field names and values
local tokenKey = KEYS[1]
local userKey = KEYS[2]
local expireAtMillis = tonumber(ARGV[1])

for i = 4, #ARGV, 2 do
  redis.call('HSET', tokenKey, ARGV[i], ARGV[i + 1])
end
redis.call('PEXPIREAT', tokenKey, expireAtMillis)

if ARGV[3] == '1' then
  redis.call('SET', userKey, ARGV[2])
  redis.call('PEXPIREAT', userKey, expireAtMillis)
end
return 1
