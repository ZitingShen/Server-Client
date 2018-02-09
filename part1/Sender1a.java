import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.imageio.ImageIO;

class Sender1a {
  public static void main(String args[]) throws Exception{
    if (args.length != 3) {
      System.out.println("Usage: java Sender1a localhost <port> <filename>");
      return;
    }
    DatagramSocket clientSocket = new DatagramSocket();
    BufferedImage img = ImageIO.read(new File(args[2]));
    ByteArrayOutputStream imgstream = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", imgstream );
    byte[] imgdata = imgstream.toByteArray();
    byte[] sendData;
    int sendDataLen;
    int serverPort = Integer.parseInt(args[1]);
    int start = 0;
    while(start < imgdata.length) {
      if (imgdata.length - 1024 > start)
        sendDataLen  = 1024;
      else
        sendDataLen = imgdata.length - start;
      sendData = new byte[sendDataLen+3];
      sendData[0] = 0; // sequence number
      sendData[1] = 0; // sequence number
      if (start + 1024 >= imgdata.length)
        sendData[2] = 1; // eof flag
      else
        sendData[2] = 0;
      System.arraycopy(imgdata, start, sendData, 3, sendDataLen);
      start += 1024;

      InetAddress ipAddress = InetAddress.getByName(args[0]);
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
        ipAddress, serverPort);
      clientSocket.send(sendPacket);
      Thread.sleep(10);
    }
    clientSocket.close();
  }
}