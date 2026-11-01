-- KEYS: pending-count, pending-zset, reservation
-- ARGV: order-id, retention-seconds
local state = redis.call('hget', KEYS[3], 'state')
if state == 'CONFIRMED' then
    return 0
end
if state ~= 'RESERVED' then
    return -1
end

local pending = tonumber(redis.call('get', KEYS[1]) or '0')
if pending > 0 then
    redis.call('decr', KEYS[1])
end
redis.call('zrem', KEYS[2], ARGV[1])
redis.call('hset', KEYS[3], 'state', 'CONFIRMED')
redis.call('expire', KEYS[3], ARGV[2])
return 1
