import java.util.HashMap;
import java.util.Map;

public class RedisStore {

    private final Map<String, String> data = new HashMap<>();
    private final Map<String, Long> expiry = new HashMap<>();
    private final Map<String, java.util.List<String>> lists = new HashMap<>();

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

    public synchronized long lpush(String key, String value) {

    java.util.List<String> list =
            lists.computeIfAbsent(key, k -> new java.util.ArrayList<>());

    list.add(0, value);

    return list.size();
}

public synchronized long rpush(String key, String value) {

    java.util.List<String> list =
            lists.computeIfAbsent(key, k -> new java.util.ArrayList<>());

    list.add(value);

    return list.size();
}

public synchronized String lpop(String key) {

    java.util.List<String> list = lists.get(key);

    if (list == null || list.isEmpty()) {
        return null;
    }

    String value = list.remove(0);

    if (list.isEmpty()) {
        lists.remove(key);
    }

    return value;
}

public synchronized String rpop(String key) {

    java.util.List<String> list = lists.get(key);

    if (list == null || list.isEmpty()) {
        return null;
    }

    String value = list.remove(list.size() - 1);

    if (list.isEmpty()) {
        lists.remove(key);
    }

    return value;
}

public synchronized java.util.List<String> lrange(
        String key, int start, int end) {

    java.util.List<String> list = lists.get(key);

    if (list == null || list.isEmpty()) {
        return new java.util.ArrayList<>();
    }

    int size = list.size();

    if (start < 0) {
        start = size + start;
    }

    if (end < 0) {
        end = size + end;
    }

    start = Math.max(start, 0);
    end = Math.min(end, size - 1);

    if (start > end || start >= size) {
        return new java.util.ArrayList<>();
    }

    return new java.util.ArrayList<>(
            list.subList(start, end + 1));
}

public synchronized long llen(String key) {

    java.util.List<String> list = lists.get(key);

    if (list == null) {
        return 0;
    }

    return list.size();
}

public synchronized long lpos(String key, String value) {

    java.util.List<String> list = lists.get(key);

    if (list == null) {
        return -1;
    }

    return list.indexOf(value);
}

public synchronized String lindex(String key, int index) {

    java.util.List<String> list = lists.get(key);

    if (list == null || list.isEmpty()) {
        return null;
    }

    if (index < 0) {
        index = list.size() + index;
    }

    if (index < 0 || index >= list.size()) {
        return null;
    }

    return list.get(index);
}

public synchronized void ltrim(String key, int start, int end) {

    java.util.List<String> list = lists.get(key);

    if (list == null || list.isEmpty()) {
        return;
    }

    int size = list.size();

    if (start < 0) {
        start = size + start;
    }

    if (end < 0) {
        end = size + end;
    }

    start = Math.max(start, 0);
    end = Math.min(end, size - 1);

    if (start > end || start >= size) {
        lists.remove(key);
        return;
    }

    java.util.List<String> trimmed =
            new java.util.ArrayList<>(
                    list.subList(start, end + 1));

    lists.put(key, trimmed);
}
}