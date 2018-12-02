import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.Console;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;

import java.util.*;

public class ChatClient {

  private static int MAXLEN = 1024;

  public static void main(String[] args) throws Exception {
    //String host = args[0];
    //String host = "172.19.101.75";
    String host = "localhost";
    //int port = Integer.parseInt(args[1]);
    int port = 12345;

    SocketChannel socket;
  	while(true) {
    	try {
	    	socket = SocketChannel.open(new InetSocketAddress(host, port));
	    	if (socket != null) 
	    		break;
    	} catch(Exception e) { Thread.sleep(1000); }  
  	}

    SocketReader reader = new SocketReader(socket);    
    SocketWriter writer = new SocketWriter(socket);
    reader.start();
    writer.start();
    reader.join();
    writer.join();
  }


    private static class SocketReader extends Thread {
    private final SocketChannel socket;
    private ByteBuffer in;
    private ByteBuffer out; 

    SocketReader(SocketChannel socket) {
      this.socket = socket;
      this.in = ByteBuffer.allocate(MAXLEN);
      this.out = ByteBuffer.allocate(MAXLEN);
    }

    public void run()  {
      boolean eof = false;
      byte b = 0;
      try {
        for(;;) {
          if (socket.read(in) <= 0) eof = true;
          in.flip();
          while(in.hasRemaining()) {
            b = in.get();
            out.put(b);
          }

          out.flip();
          if (out.remaining() > 0) {
            byte[] ba = new byte[out.remaining()];
            out.get(ba);
            out.clear();
            System.out.print(new String(ba));
          }

          if (eof && !in.hasRemaining()) break;
          in.compact();
        }

        socket.close(); 
      } catch (IOException ie) {
        try {
          socket.close();
        } catch(Exception e) { }
      }
      try {
        socket.close();
      } catch(Exception e) { }
      System.exit(0);
    }
  }

  private static class SocketWriter extends Thread {
    private final SocketChannel socket;

    SocketWriter(SocketChannel socket) {
      this.socket = socket;
    }

    public void run() {
      try {
        while (true) {
          String message = System.console().readLine();
          if (message == null) break;
          socket.write(ByteBuffer.wrap((byte[]) (message+"\n").getBytes()));
        }
      } catch(IOException ie) { 
        try {
          socket.close();
        } catch(Exception e) { }
        System.exit(0);
      }
    }
  }
}