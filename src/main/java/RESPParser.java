import java.io.*;
import java.util.*;

public class RESPParser {

    private final BufferedReader reader;

    public RESPParser(InputStream inputStream) {
        this.reader = new BufferedReader(
                new InputStreamReader(inputStream));
    }

    public List<String> readCommand() throws IOException {

        String line = reader.readLine();

        if (line == null) {
            return null;
        }

        // RESP Array must start with *
        if (!line.startsWith("*")) {
            throw new IOException("Invalid RESP request");
        }

        int argumentCount = Integer.parseInt(line.substring(1));

        List<String> arguments = new ArrayList<>();

        for (int i = 0; i < argumentCount; i++) {

            String lengthLine = reader.readLine();

            if (lengthLine == null || !lengthLine.startsWith("$")) {
                throw new IOException("Invalid bulk string");
            }

            int length = Integer.parseInt(lengthLine.substring(1));

            String argument = reader.readLine();

            if (argument == null) {
                throw new IOException("Unexpected end of request");
            }

            // Validate length
            if (argument.length() != length) {
                throw new IOException("Invalid bulk string length");
            }

            arguments.add(argument);
        }

        return arguments;
    }
}
