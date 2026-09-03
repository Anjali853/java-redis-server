import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class CommandHandler {

    private final RedisStore store;

    public CommandHandler(RedisStore store) {
        this.store = store;
    }

    public void handle(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments == null || arguments.isEmpty()) {
            writeError(writer, "empty command");
            return;
        }

        String command = arguments.get(0).toUpperCase();

        switch (command) {

            case "PING":
                handlePing(arguments, writer);
                break;

            case "ECHO":
                handleEcho(arguments, writer);
                break;

            case "SET":
                handleSet(arguments, writer);
                break;

            case "GET":
                handleGet(arguments, writer);
                break;

            case "DEL":
                handleDel(arguments, writer);
                break;
            case "INCR":
                handleIncr(arguments, writer);
                break;
            case "DECR":
                handleDecr(arguments, writer);
                break;

            case "INCRBY":
                handleIncrBy(arguments, writer);
                break;

            case "DECRBY":
                handleDecrBy(arguments, writer);
                break;

            case "MGET":
                handleMGet(arguments, writer);
                break;

            case "MSET":
                handleMSet(arguments, writer);
                break;

            case "EXISTS":
                handleExists(arguments, writer);
                break;

            case "KEYS":
                handleKeys(arguments, writer);
                break;

            case "TYPE":
                handleType(arguments, writer);
                break;

            case "TTL":
                handleTtl(arguments, writer);
                break;
                
            case "PERSIST":
                handlePersist(arguments, writer);
                break;    

            default:
                writeError(writer, "unknown command");
        }
    }

    // DEL command implementation
    private void handleDel(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() < 2) {
            writeError(
                    writer,
                    "wrong number of arguments for 'del' command");
            writer.flush();
            return;
        }

        int deleted = 0;

        for (int i = 1; i < arguments.size(); i++) {
            if (store.delete(arguments.get(i))) {
                deleted++;
            }
        }

        writer.write(":" + deleted + "\r\n");
        writer.flush();
    }

    // INCR command implementation
    private void handleIncr(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() != 2) {
            writeError(
                    writer,
                    "wrong number of arguments for 'incr' command");
            writer.flush();
            return;
        }

        String key = arguments.get(1);
        String value = store.get(key);

        if (value == null) {
            store.set(key, "1");
            writer.write(":1\r\n");
            writer.flush();
            return;
        }

        try {
            long number = Long.parseLong(value);
            number++;

            store.set(key, String.valueOf(number));

            writer.write(":" + number + "\r\n");

        } catch (NumberFormatException e) {
            writeError(writer, "value is not an integer or out of range");
        }

        writer.flush();
    }

    // INCRBY command implementation
    private void handleIncrBy(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() != 3) {
            writeError(
                    writer,
                    "wrong number of arguments for 'incrby' command");
            writer.flush();
            return;
        }

        String key = arguments.get(1);
        String value = store.get(key);

        try {
            long amount = Long.parseLong(arguments.get(2));

            if (value == null) {
                long result = amount;
                store.set(key, String.valueOf(result));
                writer.write(":" + result + "\r\n");
            } else {
                long current = Long.parseLong(value);
                long result = current + amount;

                store.set(key, String.valueOf(result));
                writer.write(":" + result + "\r\n");
            }

        } catch (NumberFormatException e) {
            writeError(writer, "value is not an integer or out of range");
        }

        writer.flush();
    }

    private void handlePing(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() == 1) {
            writer.write("+PONG\r\n");

        } else if (arguments.size() == 2) {
            writeBulkString(writer, arguments.get(1));

        } else {
            writeError(
                    writer,
                    "wrong number of arguments for 'ping' command");
        }

        writer.flush();
    }

    private void handleEcho(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() != 2) {
            writeError(
                    writer,
                    "wrong number of arguments for 'echo' command");
            writer.flush();
            return;
        }

        writeBulkString(writer, arguments.get(1));
        writer.flush();
    }

    // SET supports NX, XX, EX and PX

    private void handleSet(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() < 3) {
            writeError(
                    writer,
                    "wrong number of arguments for 'set' command");
            writer.flush();
            return;
        }

        String key = arguments.get(1);
        String value = arguments.get(2);

        boolean nx = false;
        boolean xx = false;
        Long expiryMillis = null;

        int i = 3;

        while (i < arguments.size()) {

            String option = arguments.get(i).toUpperCase();

            if (option.equals("NX")) {

                nx = true;
                i++;

            } else if (option.equals("XX")) {

                xx = true;
                i++;

            } else if (option.equals("EX") || option.equals("PX")) {

                if (i + 1 >= arguments.size()) {
                    writeError(writer, "syntax error");
                    writer.flush();
                    return;
                }

                try {
                    long time = Long.parseLong(arguments.get(i + 1));

                    if (time <= 0) {
                        writeError(writer, "invalid expire time");
                        writer.flush();
                        return;
                    }

                    if (option.equals("EX")) {
                        expiryMillis = time * 1000;
                    } else {
                        expiryMillis = time;
                    }

                } catch (NumberFormatException e) {
                    writeError(writer, "invalid expire time");
                    writer.flush();
                    return;
                }

                i += 2;

            } else {

                writeError(writer, "syntax error");
                writer.flush();
                return;
            }
        }

        // NX and XX cannot be used together
        if (nx && xx) {
            writeError(writer, "syntax error");
            writer.flush();
            return;
        }

        // Check whether key already exists
        String existingValue = store.get(key);

        // NX = set only if key DOES NOT exist
        if (nx && existingValue != null) {
            writer.write("$-1\r\n");
            writer.flush();
            return;
        }

        // XX = set only if key DOES exist
        if (xx && existingValue == null) {
            writer.write("$-1\r\n");
            writer.flush();
            return;
        }

        // Perform SET
        if (expiryMillis != null) {
            store.set(key, value, expiryMillis);
        } else {
            store.set(key, value);
        }

        writer.write("+OK\r\n");
        writer.flush();
    }

    // DECR command implementation
    private void handleDecr(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() != 2) {
            writeError(
                    writer,
                    "wrong number of arguments for 'decr' command");
            writer.flush();
            return;
        }

        String key = arguments.get(1);
        String value = store.get(key);

        if (value == null) {
            store.set(key, "-1");
            writer.write(":-1\r\n");
            writer.flush();
            return;
        }

        try {
            long number = Long.parseLong(value);
            number--;

            store.set(key, String.valueOf(number));

            writer.write(":" + number + "\r\n");

        } catch (NumberFormatException e) {
            writeError(writer, "value is not an integer or out of range");
        }

        writer.flush();
    }

    // DECRBY command implementation
    private void handleDecrBy(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() != 3) {
            writeError(
                    writer,
                    "wrong number of arguments for 'decrby' command");
            writer.flush();
            return;
        }

        String key = arguments.get(1);
        String value = store.get(key);

        try {
            long amount = Long.parseLong(arguments.get(2));

            if (value == null) {
                long result = -amount;
                store.set(key, String.valueOf(result));
                writer.write(":" + result + "\r\n");

            } else {
                long current = Long.parseLong(value);
                long result = current - amount;

                store.set(key, String.valueOf(result));
                writer.write(":" + result + "\r\n");
            }

        } catch (NumberFormatException e) {
            writeError(writer, "value is not an integer or out of range");
        }

        writer.flush();
    }

    private void handleGet(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() != 2) {
            writeError(
                    writer,
                    "wrong number of arguments for 'get' command");
            writer.flush();
            return;
        }

        String value = store.get(arguments.get(1));

        if (value == null) {
            writer.write("$-1\r\n");
        } else {
            writeBulkString(writer, value);
        }

        writer.flush();
    }

    // MGET command implementation
    private void handleMGet(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() < 2) {
            writeError(
                    writer,
                    "wrong number of arguments for 'mget' command");
            writer.flush();
            return;
        }

        int count = arguments.size() - 1;

        writer.write("*" + count + "\r\n");

        for (int i = 1; i < arguments.size(); i++) {

            String value = store.get(arguments.get(i));

            if (value == null) {
                writer.write("$-1\r\n");
            } else {
                writeBulkString(writer, value);
            }
        }

        writer.flush();
    }

    // MSET command implementation
    private void handleMSet(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() < 3 || arguments.size() % 2 == 0) {
            writeError(
                    writer,
                    "wrong number of arguments for 'mset' command");
            writer.flush();
            return;
        }

        for (int i = 1; i < arguments.size(); i += 2) {
            String key = arguments.get(i);
            String value = arguments.get(i + 1);

            store.set(key, value);
        }

        writer.write("+OK\r\n");
        writer.flush();
    }

    // EXISTS command implementation
    private void handleExists(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() < 2) {
            writeError(
                    writer,
                    "wrong number of arguments for 'exists' command");
            writer.flush();
            return;
        }

        int count = 0;

        for (int i = 1; i < arguments.size(); i++) {
            if (store.get(arguments.get(i)) != null) {
                count++;
            }
        }

        writer.write(":" + count + "\r\n");
        writer.flush();
    }

    // KEYS command implementation
    private void handleKeys(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() != 2) {
            writeError(
                    writer,
                    "wrong number of arguments for 'keys' command");
            writer.flush();
            return;
        }

        String pattern = arguments.get(1);

        if (!pattern.equals("*")) {
            writeError(writer, "only '*' pattern is supported");
            writer.flush();
            return;
        }

        java.util.Set<String> keys = store.getKeys();

        writer.write("*" + keys.size() + "\r\n");

        for (String key : keys) {
            writeBulkString(writer, key);
        }

        writer.flush();
    }

    // TYPE command implementation
    private void handleType(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() != 2) {
            writeError(
                    writer,
                    "wrong number of arguments for 'type' command");
            writer.flush();
            return;
        }

        String value = store.get(arguments.get(1));

        if (value == null) {
            writer.write("+none\r\n");
        } else {
            writer.write("+string\r\n");
        }

        writer.flush();
    }


    //
    private void handleTtl(
        List<String> arguments,
        BufferedWriter writer) throws IOException {

    if (arguments.size() != 2) {
        writeError(
                writer,
                "wrong number of arguments for 'ttl' command");
        writer.flush();
        return;
    }

    long ttl = store.getTtl(arguments.get(1));

    writer.write(":" + ttl + "\r\n");
    writer.flush();
}

//
private void handlePersist(
        List<String> arguments,
        BufferedWriter writer) throws IOException {

    if (arguments.size() != 2) {
        writeError(
                writer,
                "wrong number of arguments for 'persist' command");
        writer.flush();
        return;
    }

    boolean removed = store.persist(arguments.get(1));

    writer.write(removed ? ":1\r\n" : ":0\r\n");
    writer.flush();
}

    private void writeBulkString(
            BufferedWriter writer,
            String value) throws IOException {

        writer.write("$" + value.length() + "\r\n");
        writer.write(value);
        writer.write("\r\n");
    }

    private void writeError(
            BufferedWriter writer,
            String message) throws IOException {

        writer.write("-ERR " + message + "\r\n");
    }
}