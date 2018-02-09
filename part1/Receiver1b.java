/* Ziting Shen s1679358 */

import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class Receiver1b {
   public static void main(String args[]) throws Exception {
      if (args.length < 2) {
        System.out.println("Usage: java Receiver1a <port> <filename> [windowsize]");
        return;
      }
      DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
      InetAddress ipAddress = InetAddress.getByName("localhost");
      byte[] receiveData = new byte[1027];
      byte[] sendData = new byte[1];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, 
        receiveData.length);
      FileOutputStream out = new FileOutputStream(args[1]);
      int sequence = 0;
      int start = 0;
      while (receiveData[2] != 1) { //not eof
        serverSocket.receive(receivePacket);
        sendData[0] = receiveData[0];
        int clientPort = receivePacket.getPort();
        DatagramPacket sendPacket = new DatagramPacket(sendData, 
          sendData.length, ipAddress, clientPort);
        serverSocket.send(sendPacket);
        // extract the data only if the packet is in order
        if (receiveData[0] == sequence) {
          start += 1024;
          out.write(receiveData, 3, 1024);
          sequence = 1 - sequence;
        }
      }
      out.close();
      serverSocket.close();
   }
}