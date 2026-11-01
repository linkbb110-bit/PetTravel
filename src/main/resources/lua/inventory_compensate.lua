-- KEYS: stock, pending-count, pending-zset, reservation
-- ARGV: order-id, retention-seconds
local state = redis.call('hget', KEYS[4], 'state')
if state == 'COMPENSATED' then
    return 0
end
if state ~= 'RESERVED' then
    return -1
end

local quantity = tonumber(redis.call('hget', KEYS[4], 'quantity') or '0')
if quantity <= 0 then
    return -1
end

redis.call('incrby', KEYS[1], quantity)
local pending = tonumber(redis.call('get', KEYS[2]) or '0')
if pending > 0 then
    redis.call('decr', KEYS[2])
end
redis.call('zrem', KEYS[3], ARGV[1])
redis.call('hset', KEYS[4], 'state', 'COMPENSATED')
redis.call('expire', KEYS[4], ARGV[2])
return 1
