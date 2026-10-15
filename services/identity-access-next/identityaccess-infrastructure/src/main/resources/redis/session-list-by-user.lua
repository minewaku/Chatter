-- KEYS[1] per-user sorted set of session ids, ARGV[1] session hash key prefix
-- Returns one flat field/value array per retained session (with its id appended as "sessionId") in a single round
-- trip, and removes ids whose session hash has already expired from the index.
local ids = redis.call('ZRANGE', KEYS[1], 0, -1)
local result = {}
for _, id in ipairs(ids) do
  local fields = redis.call('HGETALL', ARGV[1] .. id)
  if #fields == 0 then
    redis.call('ZREM', KEYS[1], id)
  else
    table.insert(fields, 'sessionId')
    table.insert(fields, id)
    table.insert(result, fields)
  end
end
return result
