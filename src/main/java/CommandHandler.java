import java.io.*;
import java.util.*;

public class CommandHandler {

    public static void handle(
            List<String> arguments,
            BufferedWriter writer) throws IOException {

        if (arguments == null || arguments.isEmpty()) {
            writer.write("-ERR empty command\r\n");
            writer.flush();
            return;
        }

        String command = arguments.get(0).toUpperCase();

        switch (command) {

            case "PING":

                if (arguments.size() == 1) {
                    writer.write("+PONG\r\n");
                } else if (arguments.size() == 2) {
                    // Redis supports PING with a message
                    String message = arguments.get(1);
                    writer.write("$" + message.length() + "\r\n");
                    writer.write(message + "\r\n");
                } else {
                    writer.write(
                            "-ERR wrong number of arguments for 'ping' command\r\n");
                }

                writer.flush();
                break;

            case "ECHO":

                if (arguments.size() != 2) {
                    writer.write(
                            "-ERR wrong number of arguments for 'echo' command\r\n");
                } else {
                    String message = arguments.get(1);

                    writer.write("$" + message.length() + "\r\n");
                    writer.write(message + "\r\n");
                }

                writer.flush();
                break;

            default:

                writer.write("-ERR unknown command\r\n");
                writer.flush();
        }
    }
}