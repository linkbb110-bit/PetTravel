-- KEYS: stock, pending-count, reconcile-lock
-- ARGV: lock-token, database-stock
if redis.call('get', KEYS[3]) ~= ARGV[1] then
    return -1
end
if tonumber(redis.call('get', KEYS[2]) or '0') ~= 0 then
    return -2
end
redis.call('set', KEYS[1], ARGV[2])
return tonumber(ARGV[2])
