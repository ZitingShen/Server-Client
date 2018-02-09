/* Ziting Shen s1679358 */

import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Vector;

class Receiver2b {
   public static void main(String args[]) throws Exception {
      if (args.length < 3) {
        System.out.println("Usage: java Receiver2b <Port> <Filename> <WindowSize>");
        return;
      }
      DatagramSocket serverSocket = new DatagramSocket(Integer.parseInt(args[0]));
      InetAddress ipAddress = InetAddress.getByName("localhost");
      byte[] receiveData = new byte[1027];
      HashMap<Integer, Vector<Byte>> receivePackets = new HashMap<Integer, Vector<Byte>>();
      byte[] sendData = new byte[2];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, 
        receiveData.length);
      FileOutputStream out = new FileOutputStream(args[1]);
      int windowsize = Integer.parseInt(args[2]);
      int sequence = 1;
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length);

      while (receiveData[2] != 1) { //not eof
        serverSocket.receive(receivePacket);
        int clientPort = receivePacket.getPort();
        int received = (receiveData[0] & 0xff) << 8 | (receiveData[1] & 0xff);

        if (received >= sequence && received < sequence + windowsize) {
          sendData[0] = receiveData[0];
          sendData[1] = receiveData[1];
          sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress,
            clientPort);
          serverSocket.send(sendPacket);
          if (received == sequence) {
            out.write(receiveData, 3, receivePacket.getLength()-3);
            sequence++;
            while (receivePackets.containsKey(sequence)) {
              Vector<Byte> newVec = receivePackets.remove(sequence);
              for (Byte b: newVec) out.write(b);
              sequence++;
            }
          } else {
            Vector<Byte> newVec = new Vector<Byte>();
            for (int i = 2; i < receivePacket.getLength(); i++)
              newVec.add(receiveData[i]);
            receivePackets.put(received, newVec);
          }
        } else if (received >= sequence - windowsize && received < sequence) {
          sendData[0] = receiveData[0];
          sendData[1] = receiveData[1];
          sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress,
            clientPort);
          serverSocket.send(sendPacket);
        }
      }
      out.close();
      serverSocket.close();
   }
}
