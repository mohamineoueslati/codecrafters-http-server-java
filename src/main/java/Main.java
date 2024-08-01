import java.io.IOException;

public class Main {

  public static void main(String[] args) {

    Properties.DIR_PATH = args.length >= 2 && args[0].equals("--directory") ? args[1] : "";

    System.out.println("Logs from your program will appear here!");

    try (Server server = new Server(4221)) {
      server.start();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
