import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    int port = 6379;

    try (ServerSocket serverSocket = new ServerSocket(port)) {

      serverSocket.setReuseAddress(true);

      Socket clientSocket = serverSocket.accept();

      OutputStream output = clientSocket.getOutputStream();

      output.write("+PONG\r\n".getBytes());
      output.flush();

      clientSocket.close();

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}