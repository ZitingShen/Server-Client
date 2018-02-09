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

class Sender2b {
  private static Thread send, receive;
  private static int timeout;
  private static int windowsize;
  private static InetAddress ipAddress;
  private static int serverPort;
  private static FileInputStream imgStream;
  private static DatagramSocket clientSocket;
  private static Vector<Boolean> ifReceived = new Vector<Boolean>();
  private static Vector<Timer> timers = new Vector<Timer>(); 
  private static Vector<Vector<Byte>> sendPacket = new Vector<Vector<Byte>>();
  private static long startTime = 0;
  private static long endTime = 0;
  private static double dataVolumn = 0;
  private static int retransmission = 0;
  private static Integer base = 1;
  private static Integer sequence = 1;

  // TimerTask used in Timer
  public static class Timeout extends TimerTask{
    private int id;

    public Timeout(int id) {
      this.id = id;
    }

    @Override
    public void run(){
      byte[] sendData = new byte[1027];
      try {
        timers.get(id-1).schedule(new Timeout(id), timeout);
        for (int i = 0; i < sendPacket.get(id-1).size(); i++)
          sendData[i] = sendPacket.get(id-1).get(i);
        if (clientSocket.isClosed()) return;
        clientSocket.send(new DatagramPacket(sendData, 
          sendPacket.get(id-1).size(), ipAddress, serverPort));
        retransmission++;
      } catch(IllegalStateException e) {
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
        ifReceived.add(false);
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
            if (startTime == 0) startTime = System.currentTimeMillis();
            clientSocket.send(new DatagramPacket(sendData, dataLen+3, 
              ipAddress, serverPort));
            timers.add(new Timer());
            timers.get(timers.size()-1).schedule(new Timeout(sequence), timeout);
            dataVolumn += dataLen;

            sequence++;
            dataLen = imgStream.read(sendData, 3, 1024);
            ifReceived.add(false);
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
          int received = ((receiveData[0] & 0xff) << 8 | (receiveData[1] & 0xff));

          timers.get(received-1).cancel();
          if (received >= base && received < base + windowsize) {
            ifReceived.set(received-1, true);
            if (received == base) {
              do {
                base++;
              } while (ifReceived.get(base-1));
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String args[]) throws Exception{
    if (args.length < 5) {
      System.out.println("Usage: java Sender2b localhost <Port> <Filename> <RetryTimeout> <WindowSize>");
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