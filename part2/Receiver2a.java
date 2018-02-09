/* Ziting Shen s1679358 */

import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class Receiver2a {
   public static void main(String args[]) throws Exception {
      if (args.length < 2) {
        System.out.println("Usage: java Receiver2a <Port> <Filename>");
        return;
      }
      DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
      InetAddress ipAddress = InetAddress.getByName("localhost");
      byte[] receiveData = new byte[1027];
      byte[] sendData = new byte[2];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, 
        receiveData.length);
      FileOutputStream out = new FileOutputStream(args[1]);
      int sequence = 1;
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length);

      while (receiveData[2] != 1) { //not eof
        serverSocket.receive(receivePacket);
        int clientPort = receivePacket.getPort();
        // extract the data only if the packet is in order
        if (((receiveData[0] & 0xff) << 8 | (receiveData[1] & 0xff)) == sequence) {
          out.write(receiveData, 3, receivePacket.getLength()-3);
          sendData[0] = receiveData[0];
          sendData[1] = receiveData[1];
          sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress,
            clientPort);
          serverSocket.send(sendPacket);
          sequence++;
        } else {
          sendPacket.setAddress(ipAddress);
          sendPacket.setPort(clientPort);
          serverSocket.send(sendPacket);
        }
      }
      out.close();
      serverSocket.close();
   }
}
