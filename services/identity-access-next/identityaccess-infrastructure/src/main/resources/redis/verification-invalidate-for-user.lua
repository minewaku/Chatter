-- KEYS[1] per-user pointer key, ARGV[1] token key prefix (including the service prefix and "verification:token:")
-- Deletes the user's outstanding token unless it was already consumed, then removes the pointer.
-- Returns 1 when a token was deleted, 0 otherwise.
local digest = redis.call('GET', KEYS[1])
if not digest then
  return 0
end

local tokenKey = ARGV[1] .. digest
local deleted = 0
if redis.call('EXISTS', tokenKey) == 1 and redis.call('HEXISTS', tokenKey, 'verifiedAt') == 0 then
  redis.call('DEL', tokenKey)
  deleted = 1
end
redis.call('DEL', KEYS[1])
return deleted
