import java.io.*;
import java.net.*;

public class Main {

  public static void main(String[] args) {

    int port = 6379;

    RedisStore store = new RedisStore();

    try (ServerSocket serverSocket = new ServerSocket(port)) {

      serverSocket.setReuseAddress(true);

      System.out.println("Redis server started on port " + port);

      while (true) {

        Socket clientSocket = serverSocket.accept();

        System.out.println(
            "Client connected: "
                + clientSocket.getRemoteSocketAddress());

        // Each client gets its own thread
        Thread clientThread = new Thread(
            () -> handleClient(clientSocket, store));

        clientThread.start();
      }

    } catch (IOException e) {

      System.out.println(
          "Server error: " + e.getMessage());
    }
  }

  private static void handleClient(
      Socket clientSocket,
      RedisStore store) {

    try (Socket socket = clientSocket) {

      RESPParser parser = new RESPParser(
          socket.getInputStream());

      BufferedWriter writer = new BufferedWriter(
          new OutputStreamWriter(
              socket.getOutputStream()));

      CommandHandler handler = new CommandHandler(store);

      while (true) {

        var arguments = parser.readCommand();

        if (arguments == null) {
          break;
        }

        handler.handle(
            arguments,
            writer);
      }

    } catch (IOException e) {

      System.out.println(
          "Client error: " + e.getMessage());
    }
  }
}