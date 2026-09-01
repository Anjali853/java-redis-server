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

            default:
                writeError(writer, "unknown command");
        }
    }

    private void handlePing(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments.size() == 1) {
            writer.write("+PONG\r\n");

        } else if (arguments.size() == 2) {

            writeBulkString(
                    writer,
                    arguments.get(1));

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

        writeBulkString(
                writer,
                arguments.get(1));

        writer.flush();
    }

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

        if (arguments.size() == 3) {

            store.set(key, value);

        } else if (arguments.size() == 5) {

            String option = arguments.get(3).toUpperCase();
            long time;

            try {
                time = Long.parseLong(arguments.get(4));
            } catch (NumberFormatException e) {
                writeError(writer, "invalid expire time");
                writer.flush();
                return;
            }

            if (time < 0) {
                writeError(writer, "invalid expire time");
                writer.flush();
                return;
            }

            if (option.equals("EX")) {
                store.set(key, value, time * 1000);

            } else if (option.equals("PX")) {
                store.set(key, value, time);

            } else {
                writeError(writer, "syntax error");
                writer.flush();
                return;
            }

        } else {

            writeError(writer, "syntax error");
            writer.flush();
            return;
        }

        writer.write("+OK\r\n");
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

        String key = arguments.get(1);

        String value = store.get(key);

        if (value == null) {

            // RESP Null Bulk String
            writer.write("$-1\r\n");

        } else {

            writeBulkString(
                    writer,
                    value);
        }

        writer.flush();
    }

    private void writeBulkString(
            BufferedWriter writer,
            String value) throws IOException {

        writer.write("$" + value.length() + "\r\n");
        writer.write(value + "\r\n");
    }

    private void writeError(
            BufferedWriter writer,
            String message) throws IOException {

        writer.write("-ERR " + message + "\r\n");
    }
}