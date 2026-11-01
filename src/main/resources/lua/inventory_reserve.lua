-- KEYS: stock, pending-count, pending-zset, reservation, reconcile-lock
-- ARGV: order-id, quantity, reservation-deadline-ms
if redis.call('exists', KEYS[5]) == 1 then
    return -3
end

local stockValue = redis.call('get', KEYS[1])
if not stockValue then
    return -2
end

local state = redis.call('hget', KEYS[4], 'state')
if state == 'RESERVED' or state == 'CONFIRMED' then
    return tonumber(stockValue)
end
if state then
    return -3
end

local quantity = tonumber(ARGV[2])
local stock = tonumber(stockValue)
if quantity <= 0 or stock < quantity then
    return -1
end

local remaining = redis.call('decrby', KEYS[1], quantity)
redis.call('incr', KEYS[2])
redis.call('zadd', KEYS[3], ARGV[3], ARGV[1])
redis.call('hset', KEYS[4], 'state', 'RESERVED', 'quantity', ARGV[2])
-- A RESERVED record is durable until confirm/compensate. Expiring it first would
-- lose the quantity needed for leak-free recovery. Terminal markers receive TTL.
return remaining
