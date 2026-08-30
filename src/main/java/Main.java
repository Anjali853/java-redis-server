import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    int port = 6379;

    try {
      ServerSocket serverSocket = new ServerSocket(port);
      serverSocket.setReuseAddress(true);

      Socket clientSocket = serverSocket.accept();

      InputStream inputStream = clientSocket.getInputStream();

      byte[] buffer = new byte[1024];
      int bytesRead;

      while ((bytesRead = inputStream.read(buffer)) != -1) {
        clientSocket.getOutputStream().write("+PONG\r\n".getBytes());
        clientSocket.getOutputStream().flush();
      }

      clientSocket.close();
      serverSocket.close();

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}