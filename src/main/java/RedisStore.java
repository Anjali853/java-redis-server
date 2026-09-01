import java.util.HashMap;
import java.util.Map;

public class RedisStore {

    private final Map<String, String> data = new HashMap<>();
    private final Map<String, Long> expiry = new HashMap<>();

    public void set(String key, String value) {
        data.put(key, value);
        expiry.remove(key);
    }

    public void set(String key, String value, long expiryMillis) {
        data.put(key, value);
        expiry.put(key, System.currentTimeMillis() + expiryMillis);
    }

    public String get(String key) {
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
}