import java.util.HashMap;
import java.util.Map;

public class RedisStore {

    private final Map<String, String> data = new HashMap<>();
    private final Map<String, Long> expiry = new HashMap<>();

    public synchronized void set(String key, String value) {
        data.put(key, value);
        expiry.remove(key);
    }

    public synchronized void set(String key, String value, long expiryMillis) {
        data.put(key, value);
        expiry.put(key, System.currentTimeMillis() + expiryMillis);
    }

    public synchronized String get(String key) {
        if (!data.containsKey(key)) {
            return null;
        }

        Long expireAt = expiry.get(key);

        if (expireAt != null && System.currentTimeMillis() >= expireAt) {
            data.remove(key);
            expiry.remove(key);
            return null;
        }

        return data.get(key);
    }

    public synchronized long getTtl(String key) {

    if (!data.containsKey(key)) {
        return -2;
    }

    Long expireAt = expiry.get(key);

    if (expireAt == null) {
        return -1;
    }

    long remaining = expireAt - System.currentTimeMillis();

    if (remaining <= 0) {
        data.remove(key);
        expiry.remove(key);
        return -2;
    }

    return remaining / 1000;
}

//
public synchronized boolean persist(String key) {

    if (!data.containsKey(key)) {
        return false;
    }

    if (!expiry.containsKey(key)) {
        return false;
    }

    Long expireAt = expiry.get(key);

    if (System.currentTimeMillis() >= expireAt) {
        data.remove(key);
        expiry.remove(key);
        return false;
    }

    expiry.remove(key);
    return true;
}

    public synchronized boolean delete(String key) {
        if (!data.containsKey(key)) {
            return false;
        }

        data.remove(key);
        expiry.remove(key);

        return true;
    }

    public synchronized java.util.Set<String> getKeys() {
        return new java.util.HashSet<>(data.keySet());
    }
}