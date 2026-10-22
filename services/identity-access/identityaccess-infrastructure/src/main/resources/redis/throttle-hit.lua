-- KEYS[1] counter key
-- ARGV[1] window in milliseconds
-- Counts one attempt and returns { count, remaining milliseconds }. The expiry is set by the first attempt of a
-- window and never extended, so repeated attempts cannot prolong a block.
local count = redis.call('INCR', KEYS[1])
if count == 1 then
  redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[1]))
end
local ttl = redis.call('PTTL', KEYS[1])
if ttl < 0 then
  -- a counter without an expiry would block its subject forever
  redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[1]))
  ttl = tonumber(ARGV[1])
end
return { count, ttl }
