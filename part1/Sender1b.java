/* Ziting Shen s1679358 */

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;

class Sender1b {
  public static Timer timer;
  public static int timeout;
  public static int retransmission = 0;

  // TimerTask used in Timer
  public static class ResendTimerTask extends TimerTask{
    DatagramPacket sendPacket;
    DatagramSocket clientSocket;

    public ResendTimerTask(DatagramSocket clientSocket, 
      DatagramPacket sendPacket) {
      this.clientSocket = clientSocket;
      this.sendPacket = sendPacket;
    }

    @Override
    public void run(){
      try {
        clientSocket.send(sendPacket);
        retransmission++;
        timer.schedule(new ResendTimerTask(clientSocket, sendPacket), timeout);
      } catch(IllegalStateException e) {
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String args[]) throws Exception{
    if (args.length < 3) {
      System.out.println("Usage: java Sender1a localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
      return;
    }
    if (args.length > 3)
      timeout = Integer.parseInt(args[3]);
    DatagramSocket clientSocket = new DatagramSocket();
    BufferedImage img = ImageIO.read(new File(args[2]));
    ByteArrayOutputStream imgstream = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", imgstream );
    byte[] imgdata = imgstream.toByteArray();
    byte[] sendData;
    byte[] receiveData = new byte[1];
    DatagramPacket receivePacket = new DatagramPacket(receiveData, 
      receiveData.length);
    int sendDataLen;
    int serverPort = Integer.parseInt(args[1]);
    int start = 0;
    int sequence = 0;
    long startTime = 0;
    long endTime = 0;
    while(start < imgdata.length) {
      if (imgdata.length - 1024 > start)
        sendDataLen  = 1024;
      else
        sendDataLen = imgdata.length - start;
      sendData = new byte[sendDataLen+3];
      sendData[0] = (byte) sequence; // sequence number
      sendData[1] = 0; // sequence number
      if (start + 1024 >= imgdata.length)
        sendData[2] = 1; // eof flag
      else
        sendData[2] = 0;
      System.arraycopy(imgdata, start, sendData, 3, sendDataLen);
      InetAddress ipAddress = InetAddress.getByName(args[0]);
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
        ipAddress, serverPort);
      if (startTime == 0) startTime = System.currentTimeMillis();
      clientSocket.send(sendPacket);

      timer = new Timer();
      timer.schedule(new ResendTimerTask(clientSocket, sendPacket), timeout);

      do {
        clientSocket.receive(receivePacket);
        if (sendData[2] == 1 && receiveData[0] != sequence) { //the last packet
          // If the last ACK is lost, wait for 150ms for retransmission,
          // then exit the sender.
          Thread.sleep(150); 
          receiveData[0] = (byte) sequence;
        }
      } while (receiveData[0] != sequence);
      
      timer.cancel();
      start += 1024;
      sequence = 1 - sequence;
    }
    endTime = System.currentTimeMillis();
    System.out.println("# of retransmission: " + retransmission);
    System.out.println("average throughput: " + 
      (imgdata.length/1024.0 + retransmission) / (endTime - startTime) * 1000);
    clientSocket.close();
  }
}