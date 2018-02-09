/* Ziting Shen s1679358 */

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Vector;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;

class Sender2a {
  private static Thread send, receive;
  private static Timer timer = new Timer();
  private static int timeout;
  private static int windowsize;
  private static InetAddress ipAddress;
  private static int serverPort;
  private static FileInputStream imgStream;
  private static DatagramSocket clientSocket;
  private static Vector<Vector<Byte>> sendPacket = new Vector<Vector<Byte>>();
  private static Vector<Long> sendTime = new Vector<Long>();
  private static long startTime = 0;
  private static long endTime = 0;
  private static double dataVolumn = 0;
  private static int retransmission = 0;
  private static Integer base = 1;
  private static Integer sequence = 1;

  // TimerTask used in Timer
  public static class Timeout extends TimerTask{
    @Override
    public void run(){
      byte[] sendData = new byte[1027];
      try {
        timer.schedule(new Timeout(), timeout);
        for (int i = base; i < sequence; i++) {
          sendTime.set(i-1, System.currentTimeMillis());
          for (int j = 0; j < sendPacket.get(i-1).size(); j++)
            sendData[j] = sendPacket.get(i-1).get(j);
          if (clientSocket.isClosed()) System.exit(0);
          clientSocket.send(new DatagramPacket(sendData, 
            sendPacket.get(i-1).size(), ipAddress, serverPort));
          retransmission++;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static class SendThread implements Runnable {
    public void run() {
      byte[] sendData = new byte[1027];
      try {
        int dataLen = imgStream.read(sendData, 3, 1024);
        while (dataLen != -1) {
          if (sequence < base + windowsize) {
            sendData[1] = (byte) (sequence & 0xff); // sequence number
            sendData[0] = (byte) ((sequence >> 8) & 0xff); // sequence number
            if (dataLen < 1024) sendData[2] = 1; // eof flag
            else sendData[2] = 0;

            Vector<Byte> newVec = new Vector<Byte>();
            for (int i = 0; i < dataLen+3; i++)
              newVec.add(sendData[i]);
            sendPacket.add(newVec);
            sendTime.add(System.currentTimeMillis());
            if (startTime == 0) startTime = System.currentTimeMillis();
            clientSocket.send(new DatagramPacket(sendData, dataLen+3, 
              ipAddress, serverPort));
            dataVolumn += dataLen;

            if (base == sequence) {
              timer.cancel();
              timer = new Timer();
              timer.schedule(new Timeout(), timeout);
            }
            sequence++;
            dataLen = imgStream.read(sendData, 3, 1024);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static class ReceiveThread implements Runnable {
    public void run() {
      byte[] receiveData = new byte[2];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, 
      receiveData.length);

      try {
        while (sendPacket.size() == 0 ||
          sendPacket.get(sendPacket.size()-1).get(2) != 1) {
          clientSocket.receive(receivePacket);
          base = ((receiveData[0] & 0xff) << 8 | (receiveData[1] & 0xff)) + 1;
            
          // dynamically adjust the timeout value
          if (base == sequence) {
            timer.cancel();
            timer = new Timer();
          } else {
            if (base-1 > sequence) {
              System.out.println(sendTime.get(base-1));
              long newTime = sendTime.get(base-1) + timeout - System.currentTimeMillis();
              if (newTime < 0) newTime = 0;
              timer.schedule(new Timeout(), newTime);
            }
          }
        }
        timer.cancel();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String args[]) throws Exception{
    if (args.length < 5) {
      System.out.println("Usage: java Sender2a localhost <Port> <Filename> <RetryTimeout> <WindowSize>");
      return;
    }
    ipAddress = InetAddress.getByName(args[0]);
    serverPort = Integer.parseInt(args[1]);
    imgStream = new FileInputStream(args[2]);
    timeout = Integer.parseInt(args[3]);
    windowsize = Integer.parseInt(args[4]);
    clientSocket = new DatagramSocket();
    
    SendThread sendThread = new SendThread();
    ReceiveThread receiveThread = new ReceiveThread();
    Thread send = new Thread (sendThread);
    Thread receive = new Thread(receiveThread);
    send.start();
    receive.start();

    send.join();
    receive.join();

    endTime = System.currentTimeMillis();
    System.out.println("# of retransmission: " + retransmission);
    System.out.println("average throughput: " + 
      dataVolumn / (endTime - startTime) /1024 * 1000);
    clientSocket.close();
    System.exit(0);
  }
}