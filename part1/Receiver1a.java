import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class Receiver1a {
   public static void main(String args[]) throws Exception {
      if (args.length != 2 && args.length != 3) {
        System.out.println("Usage: java Receiver1a <port> <filename> [windowsize]");
        return;
      }
      DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
      byte[] receiveData = new byte[1027];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, 
        receiveData.length);
      FileOutputStream out = new FileOutputStream(args[1]);
      while (receiveData[2] != 1) { //not eof
        serverSocket.receive(receivePacket);
        out.write(receiveData, 3, 1024);
      }
      out.close();
      serverSocket.close();
   }
}