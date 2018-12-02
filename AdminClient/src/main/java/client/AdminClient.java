package client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.util.Scanner;
import java.util.StringTokenizer; 
import java.util.Scanner;
import java.util.InputMismatchException;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.Console;
import java.net.InetSocketAddress;
import java.nio.*;
import java.nio.channels.*;

public class AdminClient {

	private static Client client;
	private static WebResource webResource;
	private static ClientResponse response;
	private static SocketChannel socket;
	private static String host;
	private static int port;

	private static int MAXLEN = 1024;

	public static void main(String[] args) {
		host = "localhost";
    //host= "172.19.101.75";
    //host= "localhost";
    //int port = Integer.parseInt(args[1]);
    port = 54321;
		client  = Client.create();

		try {
			while(true) {
				try {
	      	socket = SocketChannel.open(new InetSocketAddress(host, port));
	      	if (socket != null) 
	      		break;
	      } catch(Exception e) { Thread.sleep(1000); } 
    	}
	    
	    SocketReader reader = new SocketReader(socket);
			reader.start();

			System.out.println("WELCOME !!");
			while(true) {
				switch(menuInicial()) {
					case 1: 
						switch(menuRooms()) {
							case 1:
								cleanScreen();	
								infoRoom();
								break;
							case 2:
								cleanScreen();
								criarRoom();
								break;
							case 3:
								cleanScreen();
								removerRoom();
								break;
							case 4:
								cleanScreen();
								listarRooms();
								break;
							case 5:
								break;
							default: System.out.println("Option not valid");
							menuInicial();
						}
						break;

				  case 2:
					  switch(menuUsers()) {
							case 1:
								cleanScreen();
								infoUser();
								break;
							case 2:
								cleanScreen();
								removerUser();
								break;
							/*case 3:
								removerUserFromRoom();
								break;*/
							case 3:
								cleanScreen();
								listarUsers();
								break;
							case 4:
								break;
							default: System.out.println("Option not valid");
							menuInicial();
						}
						break;

					case 3:
						System.out.println("Goodbye !!");
			      socket.close();
			      return;

					default: System.out.println("Option not valid");
					menuInicial();
				}
			}

			//reader.join();
		} catch(Exception e) { } 
	}

	private static void cleanScreen() {
		char ESC = 27;
		System.console().writer().print(ESC + "[2J");
		System.console().flush();
		System.console().writer().print(ESC + "[1;1H");
		System.console().flush();
	}

	private static int menuInicial() {
		System.out.println("\n1. Rooms");
		System.out.println("2. Users");
		System.out.println("3. Exit");
		return lerInt(3);
	}

	private static int menuRooms() {
		cleanScreen();
		System.out.println("1. Information");
		System.out.println("2. Create");
		System.out.println("3. Remove");
		System.out.println("4. List");
		System.out.println("5. Get Back");
		return lerInt(5);
	}

	private static int menuUsers() {
		cleanScreen();
		System.out.println("1. Information");
		System.out.println("2. Remove Account");
		// System.out.println("3. Remove From Room");
		System.out.println("3. List");
		System.out.println("4. Get Back");
		return lerInt(4);
	}

	private static void infoUser() throws IOException {
		String user = lerString("Insert the User Email: ");

		webResource = client.resource("http://"+host+":8080/chat/user/"+user);
		response = webResource.accept("application/json").get(ClientResponse.class);

		System.out.println(response.getEntity(String.class)+"\n");
	}

	private static void listarUsers() throws IOException {
		webResource = client.resource("http://"+host+":8080/chat/users");
		response = webResource.accept("application/json").get(ClientResponse.class);

		if (response.getStatus() != 200) {
			System.out.println("Failed : HTTP error code : "+ response.getStatus()+"\n"); 
		}
		else {
			System.out.println(response.getEntity(String.class)+"\n");
		}
	}

	private static void removerUser() throws IOException {
		String email = lerString("Insert the User Email: ");

		webResource = client.resource("http://"+host+":8080/chat/user/"+email);
		response = webResource.accept("application/json").delete(ClientResponse.class);

		if (response.getStatus() == 200) {
			System.out.println("User Removed with success !!\n");
			socket.write(ByteBuffer.wrap((byte[]) ("DELETEUSER<_>"+email+"\n").getBytes())); 
		}
		else { System.out.println("Failed : HTTP error code : "+ response.getStatus()+"\n"); }
	}

	private static void removerUserFromRoom() throws IOException {
		String room = lerString("Insert the Room Name: ");
		String email = lerString("Insert the User Email: ");

		webResource = client.resource("http://"+host+":8080/chat/room/"+room+"/user?email="+email);
		response = webResource.accept("application/json").delete(ClientResponse.class);

		if (response.getStatus() == 200) {
			System.out.println("User Removed from "+room+ " with success !!\n");
			socket.write(ByteBuffer.wrap((byte[]) ("KICKUSER<_>"+room+"<_>"+email+"\n").getBytes())); 
		}
		else { System.out.println("Failed : HTTP error code : "+ response.getStatus()+"\n"); }
	}

	private static void infoRoom() throws IOException {
		String room = lerString("Insert the Room Name: ");

		webResource = client.resource("http://"+host+":8080/chat/room/"+room);
		response = webResource.accept("application/json").get(ClientResponse.class);

		System.out.println(response.getEntity(String.class)+"\n");
	}

	private static void removerRoom() throws IOException {
		String room = lerString("Insert the Room Name: ");

		webResource = client.resource("http://"+host+":8080/chat/room/"+room);
		response = webResource.accept("application/json").delete(ClientResponse.class);

		if (response.getStatus() == 200) {
			socket.write(ByteBuffer.wrap((byte[]) ("DELETEROOM<_>"+room+"\n").getBytes()));
			System.out.println("Room Removed with success !!\n");
		}
		else { 
			System.out.println("Failed : HTTP error code : "+ response.getStatus()+"\n"); 
		}
	}

	private static void criarRoom() throws IOException {
		String room = lerString("Insert the Room Name: ");
		webResource = client.resource("http://"+host+":8080/chat/room/"+room);
		response = webResource.accept("application/json").post(ClientResponse.class);

		if (response.getStatus() == 200) {
			socket.write(ByteBuffer.wrap((byte[]) ("NEW<_>"+room+"\n").getBytes()));
			System.out.println("Room "+room+" added with success !!\n");
		}
		else { 
			System.out.println("Failed : HTTP error code : "+ response.getStatus()+"\n"); 
		}
	}

	private static void listarRooms() throws IOException {
		webResource = client.resource("http://"+host+":8080/chat/rooms");
		response = webResource.accept("application/json").get(ClientResponse.class);

		if (response.getStatus() != 200) {
			System.out.println("Failed : HTTP error code : "+ response.getStatus()+"\n"); 
		}
		else {
			System.out.println(response.getEntity(String.class)+"\n");
		}
	}

	private static int lerInt(int max) {
		Scanner input = new Scanner(System.in);
		boolean ok = false; 
		int i = -1;
		System.out.print("Option: ");  
		while(!ok) {
			try {
				i = input.nextInt();
				if(i > 0 && i <= max) {
					ok = true;
				}
			}
			catch (InputMismatchException e) { 
				System.out.println("Invalid Option !!");
				System.out.print("Option: ");
				input.nextLine(); 
			}
		}
		//input.close();		
		return i;
	} 

	private static String lerString(String request) {
		Scanner input = new Scanner(System.in);
		boolean ok = false; 
		String txt = "";

		System.out.print(request);
		while(!ok) {
			try {
				txt = input.nextLine();
				ok = true;
			}
			catch(InputMismatchException e) { 
				System.out.println("Invalid Value !!"); 
				System.out.print(request); 
				input.nextLine(); 
			}
		}
		//input.close();
		return txt;
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
}
