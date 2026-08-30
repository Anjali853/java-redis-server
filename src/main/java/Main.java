import java.io.*;
import java.net.*;

public class Main {

  public static void main(String[] args) {
    int port = 6379;

    try (ServerSocket serverSocket = new ServerSocket(port)) {

      serverSocket.setReuseAddress(true);

      System.out.println("Redis server started on port " + port);

      while (true) {
        Socket clientSocket = serverSocket.accept();

        System.out.println("Client connected: "
            + clientSocket.getRemoteSocketAddress());

        handleClient(clientSocket);
      }

    } catch (IOException e) {
      System.out.println("Server error: " + e.getMessage());
    }
  }

  private static void handleClient(Socket clientSocket) {

    try (
        Socket socket = clientSocket;
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream()))) {

      String line;

      while ((line = reader.readLine()) != null) {

        // RESP Array
        if (line.startsWith("*")) {

          int numberOfArguments = Integer.parseInt(line.substring(1));

          String command = null;

          for (int i = 0; i < numberOfArguments; i++) {

            String lengthLine = reader.readLine();

            if (lengthLine == null) {
              return;
            }

            // Example: $4
            int length = Integer.parseInt(lengthLine.substring(1));

            String argument = reader.readLine();

            if (argument == null) {
              return;
            }

            if (i == 0) {
              command = argument.toUpperCase();
            }
          }

          if ("PING".equals(command)) {
            writer.write("+PONG\r\n");
            writer.flush();
          } else {
            writer.write("-ERR unknown command\r\n");
            writer.flush();
          }
        }
      }

    } catch (IOException | NumberFormatException e) {
      System.out.println("Client error: " + e.getMessage());
    }
  }
}