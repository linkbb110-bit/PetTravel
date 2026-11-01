-- KEYS: stock, released-order-marker, reconcile-lock
-- ARGV: quantity. The idempotency marker is intentionally persistent.
if redis.call('exists', KEYS[2]) == 1 then
    return 0
end
if redis.call('exists', KEYS[3]) == 1 then
    return -2
end
if redis.call('exists', KEYS[1]) == 0 then
    return -1
end

redis.call('incrby', KEYS[1], tonumber(ARGV[1]))
redis.call('set', KEYS[2], '1')
return 1
