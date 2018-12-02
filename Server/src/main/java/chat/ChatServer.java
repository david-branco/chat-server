package chat;

import chat.representations.*;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.InetSocketAddress;
import co.paralleluniverse.actors.*;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.io.*;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.nio.*;
import java.nio.charset.*;

import java.util.*;

@SuppressWarnings("null")
public class ChatServer {

  static int MAXLEN = 1024;
  private static String host;
	private static Client client;
	private static WebResource webResource;
	private static ClientResponse response;

  static enum Type { MENUINICIAL, MENUCHAT, MENUGERIRCONTA, MENUMENSAGENS,
  									 OPTION, VALUE, 
  									 NICK, EMAIL, PASS, USEREXIST, DELETEUSERADMIN,
  									 NEW, UPDATE, DELETE, LOGIN, LOGOUT, 
  									 ROOMS, ROOM, ENTERROOM, LEAVEROOM, ENTER, LEAVE,
  									 DATA, EOF, IOE, LINE, MESSAGE, TERMINATE, KICKUSER,
  									 INVALID, INCORRECT, LOGGED, ALREADYINUSE, MESSAGESENDED,
  									 PRIVATEMESSAGE, INBOX, RECEIVERMESSAGE,
  									 REQUEST, RESPONSE }

  static class Msg {
    final Type type;
    final Object message;
    final Object from;  

    Msg(Type type, Object message, Object from) { 
    	this.type = type; 
    	this.message = message;
    	this.from = from; 
    }
  }

  static class SocketReader extends BasicActor<Msg, Void> {
    final ActorRef<Msg> dest;
    final FiberSocketChannel socket;
    private ByteBuffer in;
		private ByteBuffer out; 

    SocketReader(ActorRef<Msg> dest, FiberSocketChannel socket) {
      this.dest = dest;
      this.socket = socket;
      in = ByteBuffer.allocate(MAXLEN);
      out = ByteBuffer.allocate(MAXLEN);
    }

    protected Void doRun() throws InterruptedException, SuspendExecution {  
    	while (receive(msg -> {
        try {
        	boolean eof = false;
		      byte b = 0;		      

		   		if (socket.read(in) <= 0) eof = true;
          in.flip();
          while(in.hasRemaining()) {
            b = in.get();
            out.put(b);
            if (b == '\n') break;
          }

          if (eof || b == '\n') { 
            out.flip();
            if (out.remaining() > 0) {
              byte[] ba = new byte[out.remaining()];
              out.get(ba);
              out.clear();
              dest.send(new Msg(msg.type, ba, (Object) msg.message));
		        }
		      }

      		if (!(eof && !in.hasRemaining())) {
          	in.compact();        
          	return true;
      		}
      		dest.send(new Msg(Type.EOF, null, (Object) msg.message));
        	return null;

	      } catch (IOException e) {
	        dest.send(new Msg(Type.IOE, null, (Object) msg.message));
	        return null;
	      }
      }));
      return null;
    }
  }

  static class UserChat extends BasicActor<Msg, Void> {
    final ActorRef usersManager;
    final ActorRef roomsManager;
    final FiberSocketChannel socket;
    private User user;
    private String room;
    private Message inboxMessage;
    private ActorRef roomManager;

    UserChat(ActorRef usersManager, ActorRef roomsManager, FiberSocketChannel socket) { 
      this.usersManager = usersManager;
      this.roomsManager = roomsManager;
      this.socket = socket;
      this.user = new User(); 
      this.room = null;
      this.inboxMessage = new Message();
    }

  	private String menuInicial() {
	    StringBuilder s = new StringBuilder();
	    s.append("\n\n1. Login\n");
	    s.append("2. Create Account\n");
	    s.append("3. Exit\n");	    
	    return s.toString();
  	}

  	private String menuChat() {
  		StringBuilder s = new StringBuilder();
  		s.append("\n\n1. Private Messages\n");
			s.append("2. Account\n");
			s.append("3. Rooms\n");
			s.append("4. Get in Room\n");
			s.append("5. Logout\n");
			return s.toString();
		}

		private String menuGerirConta() {
			StringBuilder s = new StringBuilder();
			s.append("\n\n1. Information\n");
			s.append("2. Update\n");
			s.append("3. Delete\n");
			s.append("4. Get Back\n");
			return s.toString();
		}

		private String menuMensagens() {
			StringBuilder s = new StringBuilder();
			s.append("\n\n1. Send Private Message\n");
			s.append("2. Inbox\n");;
			s.append("3. Get Back\n");
			return s.toString();
		}

    protected Void doRun() throws InterruptedException, SuspendExecution {
      ActorRef socketReader = new SocketReader(self(), socket).spawn();
      user.setRef((ActorRef) self());   	 	
      while (receive(msg -> {
      	try {
	        switch (msg.type) {
	        	case MENUINICIAL:	      			
	        		socket.write(ByteBuffer.wrap((byte[])menuInicial().getBytes()));
	        		socket.write(ByteBuffer.wrap((byte[])"Option: ".getBytes()));       		
			        socketReader.send(new Msg(Type.OPTION, "MENUINICIAL", self()));
	        		return true;

	          case MENUCHAT:	      			
	        		socket.write(ByteBuffer.wrap((byte[])menuChat().getBytes()));
	        		socket.write(ByteBuffer.wrap((byte[])"Option: ".getBytes()));       		
			        socketReader.send(new Msg(Type.OPTION, "MENUCHAT", self()));
	        		return true;

	        	case MENUMENSAGENS:	      			
	        		socket.write(ByteBuffer.wrap((byte[])menuMensagens().getBytes()));
	        		socket.write(ByteBuffer.wrap((byte[])"Option: ".getBytes()));       		
			        socketReader.send(new Msg(Type.OPTION, "MENUMENSAGENS", self()));
	        		return true;

	        	case ROOMS: 
	        		socket.write(ByteBuffer.wrap((byte[]) ((String) msg.message).getBytes()));
	        		self().send(new Msg(Type.MENUCHAT, null, self()));
	        		return true;

	        	case ENTERROOM:
	        		roomManager = (ActorRef) msg.from;
	        	  String message = "\n\nWelcome to "+((String) msg.message)+" !!\n\t (type ::quit to leave the Room)\n";
	        		socket.write(ByteBuffer.wrap((byte[]) message.getBytes()));
	        		socketReader.send(new Msg(Type.ROOM, "DATA", self()));
	        		return true;

			      case MESSAGE:
			      	message = new String ((byte[]) msg.message, "UTF-8").replace("\n", "").replace("\r", "");
			      	if(((String) msg.from).equals("Admin")) {
			      		socket.write(ByteBuffer.wrap((byte[]) (message+"\n").getBytes()));
			      	}
			      	else {
	            	socket.write(ByteBuffer.wrap((byte[]) ("["+((String) msg.from)+"]: "+message+"\n").getBytes()));
	            }
	            return true; 

	        	case ROOM:
		        	switch ((String) msg.from) {
			          case "DATA":
									message = new String ((byte[]) msg.message, "UTF-8").replace("\n", "").replace("\r", "");
			          	if(message.equals("::quit")) {
			          		roomsManager.send(new Msg(Type.LEAVEROOM, (Object) this.room+"<_>"+user.getEmail()+"<_>"+user.getNickname(), self()));
			          		//this.room = null;
			          	}
			          	else {
				            roomManager.send(new Msg(Type.LINE, msg.message, (Object) this.room+"<_>"+user.getNickname()));
				            socketReader.send(new Msg(Type.ROOM, "DATA", self()));
			          	}
			            return true;
			          /*case "EOF":
			          case "IOE":
			            usersManager.send(new Msg(Type.LOGOUT, (Object) user.getEmail(), self()));
		          		roomsManager.send(new Msg(Type.LEAVEROOM, (Object) this.room+"<_>"+user.getEmail(), self()));
		            	socket.close();
			            return false;	*/		          
			        }

	        	case OPTION:
		        	String opcao = new String ((byte[]) msg.message, "UTF-8").replace("\n", "").replace("\r", "");

		        	switch ((String) msg.from) {
		        		case "MENUINICIAL": 		        		
			    				switch (opcao) {
			        			case "1":
			        				user.reset();
			        			  socket.write(ByteBuffer.wrap((byte[])"Insert Your Email: ".getBytes()));       		
					        		socketReader.send(new Msg(Type.VALUE, "EMAIL", self())); 
			        				return true; 	
			        			case "2":
			        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Nickname: ".getBytes()));       		
					        		socketReader.send(new Msg(Type.VALUE, "NICKNAME", self()));      		
			        				return true; 		        			
			        			case "3": 
				        			socket.write(ByteBuffer.wrap((byte[])"Goodbye !!\n".getBytes()));
				        			socket.close();      		              
			                return false;

			          		default: 
				          		socket.write(ByteBuffer.wrap((byte[])"Invalid Option !!\n".getBytes()));
				          		socket.write(ByteBuffer.wrap((byte[])"Option: ".getBytes())); 
			        				socketReader.send(new Msg(Type.OPTION, "MENUINICIAL", self()));
			        				return true; 
			        	  }

			        	case "MENUCHAT": 		        		
			    				switch (opcao) {
			    					case "1":
			        				self().send(new Msg(Type.MENUMENSAGENS, null, self()));
			        				return true; 

			        			case "2":
			        				socket.write(ByteBuffer.wrap((byte[])menuGerirConta().getBytes()));
			        				socket.write(ByteBuffer.wrap((byte[])"Option: ".getBytes()));
			        				socketReader.send(new Msg(Type.OPTION, "MENUGERIRCONTA", self()));
			        				return true; 

			        			case "3":
			        				roomsManager.send(new Msg(Type.ROOMS, null, self()));
			        				return true;   
		        			
			        			case "4": 
			        				socket.write(ByteBuffer.wrap((byte[])"Insert the Room Name: ".getBytes()));       		
					        		socketReader.send(new Msg(Type.VALUE, "ENTERROOM", self())); 
			        				return true; 	

			        			case "5":
			        				socket.write(ByteBuffer.wrap((byte[])"Goodbye !!\n".getBytes()));
			        				usersManager.send(new Msg(Type.LOGOUT, (Object) user.getEmail(), self())); 		      		
		      						return true;

			          		default: 
				          		socket.write(ByteBuffer.wrap((byte[])"Invalid Option !!\n".getBytes()));
				          		socket.write(ByteBuffer.wrap((byte[])"Option: ".getBytes())); 
			        				socketReader.send(new Msg(Type.OPTION, "MENUCHAT", self()));
			        				return true; 
			        	  }

			        	case "MENUMENSAGENS":
			        		switch (opcao) {
			        			case "1":
			        				socket.write(ByteBuffer.wrap((byte[])"Insert Receiver Email: ".getBytes()));       		
					        		socketReader.send(new Msg(Type.VALUE, "RECEIVEREMAIL", self()));      		
			        				return true;

			        			case "2":
			        				usersManager.send(new Msg(Type.INBOX, (Object) user.getEmail(), self()));
			        				return true;

			        			case "3":
			        				self().send(new Msg(Type.MENUCHAT, null, self()));
			        				return true;  
			        		}

			        	case "MENUGERIRCONTA":
			        		switch (opcao) {
			        			case "1":
			        				socket.write(ByteBuffer.wrap((byte[]) (user.toString()).getBytes()));
			        				self().send(new Msg(Type.MENUCHAT, null, self()));
			        				return true; 

			        			case "2":
			        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Nickname: ".getBytes()));       		
					        		socketReader.send(new Msg(Type.VALUE, "UPDATENICKNAME", self()));      		
			        				return true;

			        			case "3":
			        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Password: ".getBytes()));       		
					        		socketReader.send(new Msg(Type.VALUE, "DELETE", self()));      		
			        				return true; 

			        			case "4":
			        				self().send(new Msg(Type.MENUCHAT, null, self()));
			        				return true;       				

			        			default: 
				          		socket.write(ByteBuffer.wrap((byte[])"Invalid Option !!\n".getBytes()));
				          		socket.write(ByteBuffer.wrap((byte[])"Option: ".getBytes())); 
			        				socketReader.send(new Msg(Type.OPTION, "MENUGERIRCONTA", self()));
			        				return true; 
			        		} 	
	        		}

		        case VALUE:
		        	String value = new String ((byte[]) msg.message, "UTF-8").replace("\n", "").replace("\r", "");

			        switch ((String) msg.from) {
		        		case "NICKNAME":
		        			if(value.equals("")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Empty field !!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Nickname: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "NICKNAME", self()));
					        	return true;  
		        			}

		        			if(value.contains("<_>")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Invalid character inserted (<_>)!!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Nickname: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "NICKNAME", self()));
					        	return true;  
		        			}

		        			user.setNickname(value);
			        		socket.write(ByteBuffer.wrap((byte[])"Insert Your Email: ".getBytes()));       		
					        socketReader.send(new Msg(Type.VALUE, "EMAIL", self()));      		      		
		      				return true;

		      			case "EMAIL":
		      				if(value.equals("")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Empty field !!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Email: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "EMAIL", self()));
					        	return true;  
		        			}

		        			if(value.contains("<_>")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Invalid character inserted (<_>)!!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Email: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "EMAIL", self()));
					        	return true;  
		        			}

		      				user.setEmail(value);
			        		socket.write(ByteBuffer.wrap((byte[])"Insert Your Password: ".getBytes()));       		
					        socketReader.send(new Msg(Type.VALUE, "PASSWORD", self()));      		      		
		      				return true;

		      			case "PASSWORD":
		      				if(value.equals("")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Empty field !!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Password: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "PASSWORD", self()));
					        	return true;  
		        			}

		        			if(value.contains("<_>")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Invalid character inserted (<_>)!!\n".getBytes()));  
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Password: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "PASSWORD", self()));
					        	return true;  
		        			}

		        			user.setPassword(value);

			        		if(user.getNickname() != null) {
			        			usersManager.send(new Msg(Type.NEW, (Object) user.getNickname()+"<_>"+user.getEmail()+"<_>"+user.getPassword(), self())); 
			        			user.changeLogged();
			        		}
			        		else {
			        			if(user.getEmail() != null && user.getPassword() != null) {
			        				usersManager.send(new Msg(Type.LOGIN, (Object) user.getEmail()+"<_>"+user.getPassword(), self()));
			        				user.changeLogged();
			        			}
			        			else {
			        				self().send(new Msg(Type.INCORRECT, null, self()));
			        				self().send(new Msg(Type.MENUINICIAL, null, self()));
			        			}
			        		}
			        		return true;

			        	case "UPDATENICKNAME":
		        			if(value.equals("")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Empty field !!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Nickname: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "UPDATENICKNAME", self()));
					        	return true;  
		        			}

		        			if(value.contains("<_>")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Invalid character inserted (<_>)!!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Nickname: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "UPDATENICKNAME", self()));
					        	return true;  
		        			}

		        			user.setNickname(value);
			        		socket.write(ByteBuffer.wrap((byte[])"Insert Your Password: ".getBytes()));       		
					        socketReader.send(new Msg(Type.VALUE, "UPDATEPASSWORD", self()));      		      		
		      				return true;

		      			case "UPDATEPASSWORD":
		      				if(value.equals("")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Empty field !!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Password: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "UPDATEPASSWORD", self()));
					        	return true;  
		        			}

		        			if(value.contains("<_>")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Invalid character inserted (<_>)!!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Password: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "UPDATEPASSWORD", self()));
					        	return true;  
		        			}

		        			user.setPassword(value);
			        		usersManager.send(new Msg(Type.UPDATE, (Object) user.getNickname()+"<_>"+user.getEmail()+"<_>"+user.getPassword(), self()));
			        		return true;

			        	case "RECEIVEREMAIL":
		      				if(value.equals("")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Empty field !!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Receiver Email: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "RECEIVEREMAIL", self()));  
					        	return true;  
		        			}

		        			if(value.equals("")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Empty field !!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Receiver Email: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "RECEIVEREMAIL", self()));  
					        	return true;  
		        			}
		        			
		        			inboxMessage.setFrom(user.getEmail());
		        			inboxMessage.setTo(value);
		        			usersManager.send(new Msg(Type.USEREXIST, (Object) value, self()));;
			        		return true;

			        	case "RECEIVERMESSAGE":
			        		if(value.contains("<_>")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Invalid character inserted (<_>)!!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Type your Message: ".getBytes()));
		        				socketReader.send(new Msg(Type.VALUE, "RECEIVERMESSAGE", self()));
					        	return true;  
		        			}

		        			inboxMessage.setMessage(value);	        			
		        			usersManager.send(new Msg(Type.PRIVATEMESSAGE, (Object) inboxMessage.getFrom()+"<_>"+inboxMessage.getTo()+"<_>"+inboxMessage.getMessage(), self())); 
			        		return true;

			        	case "DELETE":
			        		if(value.equals("")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Empty field !!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Password: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "DELETE", self()));
					        	return true;  
		        			}

		        			if(value.contains("<_>")) {
		        				socket.write(ByteBuffer.wrap((byte[])"Invalid character inserted (<_>)!!\n".getBytes())); 
		        				socket.write(ByteBuffer.wrap((byte[])"Insert Your Password: ".getBytes()));       		
					        	socketReader.send(new Msg(Type.VALUE, "DELETE", self()));
					        	return true;  
		        			}
		        			
			        		usersManager.send(new Msg(Type.DELETE, (Object) user.getEmail()+"<_>"+value, self()));
			        		return true;

			        	case "ENTERROOM":
			        		this.room = value;
			        		roomsManager.send(new Msg(Type.ENTERROOM, (Object) this.room+"<_>"+user.getNickname()+"<_>"+user.getEmail(), self()));      		      		
		      				return true;

		      			default:
		      				break;
	      			}

	    /*  			case KICKUSER:
	      				socket.write(ByteBuffer.wrap((byte[]) "\n\nYour have been removed from the Chat room by an chat admin !!\n".getBytes()));
	      				self().send(new Msg(Type.MENUCHAT, null, self()));
	      				return true;*/

	      			case DELETEUSERADMIN:
	      				socket.write(ByteBuffer.wrap((byte[]) "\n\nYour account has been removed by an chat admin !!\n".getBytes()));
	      				socket.close();
		            return false;

	      			case RECEIVERMESSAGE:
	      				socket.write(ByteBuffer.wrap((byte[])"Type your Message: ".getBytes()));
		        		socketReader.send(new Msg(Type.VALUE, "RECEIVERMESSAGE", self()));
		        		return true;

	      			case INBOX:
	      				socket.write(ByteBuffer.wrap((byte[]) (msg.message +"\n").getBytes())); 
		        		self().send(new Msg(Type.MENUCHAT, null, self()));
		        		return true;

	      			case MESSAGESENDED: 
	        			socket.write(ByteBuffer.wrap((byte[]) "Message sended with success !!\n".getBytes())); 
	        			return true;

	      			case INCORRECT: 
	        			socket.write(ByteBuffer.wrap((byte[])"Incorrect value(s) inserted !!\n".getBytes())); 
	        			return true;

	        		case LOGGED: 
	        			socket.write(ByteBuffer.wrap((byte[]) "User already logged !!\n".getBytes())); 
	        			return true;

	        		case ALREADYINUSE: 
	        			socket.write(ByteBuffer.wrap((byte[]) "Email already in use !!\n".getBytes())); 
	        			return true;

	        		case NICK: 
	        			user.setNickname((String) msg.message);
	        			return true;

	        		case EOF:
		          case IOE:
		          	if(user.getEmail() == null) { user.setEmail(" "); }
		          	usersManager.send(new Msg(Type.LOGOUT, (Object) user.getEmail(), self()));
		          	user.changeLogged();
		          	if(this.room != null) { 
		          		roomsManager.send(new Msg(Type.LEAVEROOM, (Object) this.room+"<_>"+user.getEmail(), self()));
		          	}
		            socket.close();
		            return false;

			      	default:
			      		break;
	          }
	      } catch (IOException e) { System.out.println("erro UserChat"); }
        return false;  // stops the actor if some unexpected message is received
      }));
      return null;
    }
  }

	static class AdminChat extends BasicActor<Msg, Void> {
    final ActorRef usersManager;
    final ActorRef roomsManager;
    final FiberSocketChannel socket;

    AdminChat(ActorRef usersManager, ActorRef roomsManager, FiberSocketChannel socket) { 
    	this.usersManager = usersManager;
      this.roomsManager = roomsManager;
      this.socket = socket;
    }

  	protected Void doRun() throws InterruptedException, SuspendExecution {
  		ActorRef socketReader = new SocketReader(self(), socket).spawn();
  		socketReader.send(new Msg(Type.REQUEST, null, self()));
	    while (receive(msg -> {
  			try {
  				String response= new String ((byte[]) msg.message, "UTF-8").replace("\n", "").replace("\r", "");
	        
	        switch (msg.type) {
	          case REQUEST:
	            String[] parts = response.split("<_>");

	            switch (parts[0]) {
		        		case "NEW":
			            roomsManager.send(new Msg(Type.NEW, (Object) parts[1] , self()));
			            break;

			          case "DELETEROOM":
			          	roomsManager.send(new Msg(Type.DELETE, (Object) parts[1] , self()));
			            break;

			          case "DELETEUSER":
			          	roomsManager.send(new Msg(Type.DELETEUSERADMIN, (Object) parts[1] , self()));
			          	usersManager.send(new Msg(Type.DELETEUSERADMIN, (Object) parts[1] , self()));
			            break;

			          case "KICKUSER":
			          	roomsManager.send(new Msg(Type.KICKUSER, (Object) parts[1]+"<_>"+parts[2] , self()));
			          	break;

		            case "ERROR":
			            socket.write(ByteBuffer.wrap((byte[]) ("Failed : HTTP error code : "+parts[1]+"\n").getBytes()));
			            break;


			          case "EOF":
	          		case "IOE":
			            socket.close();
			            return false;
		          }
		          socketReader.send(new Msg(Type.REQUEST, null, self()));
		          return true;

		        case EOF:
	          case IOE:
	            socket.close();
	            return false;
	        }
	    	} catch(Exception e) { }
	    	return false;
	    }));
      return null;
    }
  }

  static class RoomManager extends BasicActor<Msg, Void> {
  	private Room room;

  	RoomManager(String name) { 
      this.room = new Room(name);
    }

    protected Void doRun() throws InterruptedException, SuspendExecution { 
      while (receive(msg -> {
        switch (msg.type) {
        	case ENTER:
        		String parts[] = ((String) msg.message).split("<_>");
        		room.putUser(parts[0], parts[1], (ActorRef) msg.from);
        		self().send(new Msg(Type.LINE, (Object) ((byte[]) ("[User "+parts[0]+ " entered the room]").getBytes()) , (Object) room.getName()+"<_>"+"Admin"));
          	((ActorRef) msg.from).send(new Msg(Type.ENTERROOM, (Object) room.getName(), self()));
        		return true;

        	case LEAVE:
        		parts = ((String) msg.message).split("<_>");	
        		room.removeUser(parts[0]);
          	((ActorRef) msg.from).send(new Msg(Type.MENUCHAT, null, self()));          	
          	self().send(new Msg(Type.LINE, (Object) ((byte[]) ("[User "+parts[1]+ " left the room]").getBytes()) , (Object) room.getName()+"<_>"+"Admin"));
          	return true;

/*          case KICKUSER:
        		String email = (String) msg.message;
        		User userAux = room.getUser(email);
        		room.removeUser(email);
        		((ActorRef) userAux.getRef()).send(new Msg(Type.KICKUSER, null, self()));
          	self().send(new Msg(Type.LINE, (Object) ((byte[]) ("[User "+userAux.getNickname()+ " has been kicked from the room]").getBytes()) , (Object) room.getName()+"<_>"+"Admin"));
          	return true;*/

          case LINE:;
         	  parts = ((String) msg.from).split("<_>");	  
        	  for (User u: room.getUsers()) {     	  	
         	  	if (!u.getNickname().equals(parts[1])) {
         	  		u.getRef().send(new Msg(Type.MESSAGE, msg.message, parts[1]));
         	  	}
         	  }
            return true;

          case DELETEUSERADMIN:
          	String email = (String) msg.message;
          	room.removeUser(email);
          	return true;

          case TERMINATE:
          	return false;
        }
	      return false;
      }));
      return null;
    }
  }

  static class RoomsManager extends BasicActor<Msg, Void> {
  	private Rooms rooms = new Rooms();

  	private void putUserInRoom(String name, String nickname, String email, ActorRef from) {
	    Room room = rooms.getRoom(name);
	    int index = rooms.indexOf(room);
	    User user = new User(nickname, email, "****", from);
	    room.putUser(user);
	    rooms.set(index, room);
	    webResource = client.resource("http://"+host+":8080/chat/room/"+name+"/user?email="+email+"&nickname="+nickname);
			webResource.accept("application/json").put(ClientResponse.class); 
	  }

	  private void removeUserFromRoom(String name, String email) {
	  	if(rooms.containsRoom(name)) {
	    	Room room = rooms.getRoom(name);
		    if(room.containsUser(email)) {
			    int index = rooms.indexOf(room);
			    room.removeUser(email);
			    rooms.set(index, room);
			    webResource = client.resource("http://"+host+":8080/chat/room/"+name+"/user?email="+email);
					webResource.accept("application/json").delete(ClientResponse.class);  
			  }
	  	}
	  } 

  	protected Void doRun() throws InterruptedException, SuspendExecution { 
      while (receive(msg -> {
        switch (msg.type) {
          case ROOMS:
          	((ActorRef) msg.from).send(new Msg(Type.ROOMS, (Object) rooms.listarRooms(), self()));
          	return true;

          case ENTERROOM:
          	String[] parts = ((String) msg.message).split("<_>");
          	if(rooms.containsRoom(parts[0])) {
          		putUserInRoom(parts[0], parts[1], parts[2], (ActorRef) msg.from);
          		System.out.println(rooms.listarRooms());
          		ActorRef roomRef = rooms.getRoom(parts[0]).getRef();
          		roomRef.send(new Msg(Type.ENTER, (Object) parts[1]+"<_>"+parts[2], msg.from));      		
          	}
          	else {
          		((ActorRef) msg.from).send(new Msg(Type.INCORRECT, null, self()));
          		((ActorRef) msg.from).send(new Msg(Type.MENUCHAT, null, self()));
          	}
          	return true;   

          case LEAVEROOM:
          	parts = ((String) msg.message).split("<_>");
          	removeUserFromRoom(parts[0], parts[1]);
          	System.out.println(rooms.listarRooms());
          	if(parts[1].equals(" ")) return true;
          	ActorRef roomRef = rooms.getRoom(parts[0]).getRef();
          	roomRef.send(new Msg(Type.LEAVE, (Object) parts[1]+"<_>"+parts[2], msg.from));
          	return true;

          case NEW:
          	String name = (String) msg.message; 
          	roomRef = new RoomManager(name).spawn();
          	rooms.addRoom(new Room(name, roomRef));
          	System.out.println(rooms.listarRooms());
          	return true;

          case DELETE:
          	name = (String) msg.message;
          	roomRef = rooms.getRoom(name).getRef(); 
          	roomRef.send(new Msg(Type.TERMINATE, null, msg.from));
          	rooms.deleteRoom(name);
          	System.out.println(rooms.listarRooms());
          	return true;

          case DELETEUSERADMIN:
          	String email = (String) msg.message;
          	name = rooms.whichRoomUser(email);
          	if(name != null) {
          		roomRef = rooms.getRoom(name).getRef();
          		roomRef.send(new Msg(Type.DELETEUSERADMIN, (Object) email, msg.from));
          		rooms.removeUserFromRoom(name, email);
          	}
          	return true;

/*          	case KICKUSER:
	          	parts = ((String) msg.message).split("<_>");
	          	User userAux = rooms.getRoom(parts[0]).getUser(parts[1]);
	          	((ActorRef) userAux.getRef()).send(new Msg(Type.ROOM, ((byte[]) "::quit".getBytes()), (Object) "DATA"));
	          	return true;*/

/*          case KICKUSER:
          	parts = ((String) msg.message).split("<_>");
          	rooms.removeUserFromRoom(parts[0], parts[1]);
          	System.out.println(rooms.listarRooms());
          	roomRef = rooms.getRoom(parts[0]).getRef();
          	roomRef.send(new Msg(Type.KICKUSER, (Object) parts[1], msg.from));
          	return true;*/
        }
	      return false;
      }));
      return null;
    }
  }

  static class UsersManager extends BasicActor<Msg, Void> {
  	private Users users = new Users();

  	protected Void doRun() throws InterruptedException, SuspendExecution {
      while (receive(msg -> {
        switch (msg.type) {
          case MENUINICIAL:;
            ((ActorRef) msg.from).send(msg);
            return true;

          case NEW:
          	String[] parts = ((String) msg.message).split("<_>");
          	User usarAux = users.getUser(parts[1]);
          	if(users.getUser(parts[1]) == null) {
          		users.addUser(new User(parts[0], parts[1], parts[2], (ActorRef) msg.from));
          		webResource = client.resource("http://"+host+":8080/chat/user?email="+parts[1]+"&nickname="+parts[2]);
							webResource.accept("application/json").post(ClientResponse.class);
          		System.out.println(users.listarUsers());
          		((ActorRef) msg.from).send(new Msg(Type.MENUCHAT, null, self()));
          	}
          	else {
          		((ActorRef) msg.from).send(new Msg(Type.ALREADYINUSE, null, self()));
							((ActorRef) msg.from).send(new Msg(Type.MENUINICIAL, null, self()));
          	}
          	return true;

          case UPDATE:
          	parts = ((String) msg.message).split("<_>");
          	users.updateUser(parts[1], parts[0], parts[2]);
          	System.out.println(users.listarUsers());
          	((ActorRef) msg.from).send(new Msg(Type.MENUCHAT, null, self()));
          	return true;

          case DELETE:
          	parts = ((String) msg.message).split("<_>");
          	if(users.deleteUser(parts[0], parts[1])) {
          		webResource = client.resource("http://"+host+":8080/chat/user/"+parts[0]);
							webResource.accept("application/json").delete(ClientResponse.class);							
          		((ActorRef) msg.from).send(new Msg(Type.OPTION, ((byte[]) "3".getBytes()), "MENUINICIAL"));
          	}
          	else {
          		((ActorRef) msg.from).send(new Msg(Type.INCORRECT, null, self()));
							((ActorRef) msg.from).send(new Msg(Type.MENUGERIRCONTA, null, self()));
          	}
          	return true;

          case DELETEUSERADMIN:
          	String email = (String) msg.message;
          	User userAux = users.getUser(email);
          	users.deleteUser(email);
          	if(userAux.getLogged()) 							
          		((ActorRef) userAux.getRef()).send(new Msg(Type.DELETEUSERADMIN, ((byte[]) "3".getBytes()), "MENUINICIAL"));

          	return true;

          case LOGIN:;
          	parts = ((String) msg.message).split("<_>");
          	userAux = users.getUser(parts[0]);
          	if(userAux != null && !userAux.getLogged() && userAux.getPassword().equals(parts[1])) {
          		users.loggUser(parts[0]);
          		webResource = client.resource("http://"+host+":8080/chat/user?email="+parts[1]);
							webResource.accept("application/json").put(ClientResponse.class);
          		System.out.println(users.listarUsers());
          		((ActorRef) msg.from).send(new Msg(Type.NICK, (Object) userAux.getNickname(), self()));
          		((ActorRef) msg.from).send(new Msg(Type.MENUCHAT, null, self()));
          	}
          	else {
          		if(userAux == null || !userAux.getPassword().equals(parts[1])) {
	          		((ActorRef) msg.from).send(new Msg(Type.INCORRECT, null, self()));
          		}
          		else {
          			((ActorRef) msg.from).send(new Msg(Type.LOGGED, null, self()));
	          	}
	          	((ActorRef) msg.from).send(new Msg(Type.MENUINICIAL, null, self()));
          	}

          	return true;	

          case LOGOUT:
          	email = (String) msg.message;
          	if(email.equals(" ")) return true;
          	users.loggUser(email);
          	webResource = client.resource("http://"+host+":8080/chat/user?email="+email);
						webResource.accept("application/json").put(ClientResponse.class);
          	System.out.println(users.listarUsers());
          	((ActorRef) msg.from).send(new Msg(Type.MENUINICIAL, null, self()));
          	return true;

          case USEREXIST:
          	email = (String) msg.message;
          	if(users.containsUser(email) == true) {
          		((ActorRef) msg.from).send(new Msg(Type.RECEIVERMESSAGE, null, "MENUMENSAGENS"));
          	}
          	else { 
          		((ActorRef) msg.from).send(new Msg(Type.INCORRECT, null, self()));
          		((ActorRef) msg.from).send(new Msg(Type.MENUMENSAGENS, null, self()));
          	}
          	return true;

          case PRIVATEMESSAGE:
          	parts = ((String) msg.message).split("<_>");
          	userAux = users.getUser(parts[1]);
          	if(userAux == null) {
          		((ActorRef) msg.from).send(new Msg(Type.INCORRECT, null, self()));
          	}
          	else {
	          	userAux.sendMessage(new Message(parts[0], parts[1], parts[2]));
	          	users.addUser(userAux);

	          	((ActorRef) msg.from).send(new Msg(Type.MESSAGESENDED, null, self()));
	          }
          	((ActorRef) msg.from).send(new Msg(Type.MENUMENSAGENS, null, self()));
          	return true;

          case INBOX:
          	email = (String) msg.message;
          	String inbox = (users.getUser(email)).getMessages(); 
          	((ActorRef) msg.from).send(new Msg(Type.INBOX, (Object) inbox, self()));
          	return true;
        }
	      return false;
      }));
      return null;
    }
  }

  static class AcceptorUser extends BasicActor {
    final int port;
    final ActorRef usersManager;
    final ActorRef roomsManager;

    AcceptorUser(int port, ActorRef usersManager, ActorRef roomsManager) { 
      this.port = port; 
      this.usersManager = usersManager;
      this.roomsManager = roomsManager;
    }

    protected Void doRun() throws InterruptedException, SuspendExecution {
      try {
	      FiberServerSocketChannel ss = FiberServerSocketChannel.open();
	      ss.bind(new InetSocketAddress(port));
	      while (true) {
	        FiberSocketChannel socket = ss.accept();
	        ActorRef user = new UserChat(usersManager, roomsManager, socket).spawn();
	        socket.write(ByteBuffer.wrap((byte[]) "WELCOME !!\n".getBytes()));
	        usersManager.send(new Msg(Type.MENUINICIAL, null, user));
      	}
      } catch (IOException e) { }
      return null;
    }
  }

  static class AcceptorAdmin extends BasicActor {
    final int port;
    final ActorRef usersManager;
    final ActorRef roomsManager;

    AcceptorAdmin(int port, ActorRef usersManager, ActorRef roomsManager)  { 
      this.port = port; 
      this.usersManager = usersManager;
      this.roomsManager = roomsManager;
    }

    protected Void doRun() throws InterruptedException, SuspendExecution {
      try {
	      FiberServerSocketChannel ss = FiberServerSocketChannel.open();
	      ss.bind(new InetSocketAddress(port));
	      while (true) {
	        FiberSocketChannel socket = ss.accept();
	       	new AdminChat(usersManager, roomsManager, socket).spawn();
      	}
      } catch (IOException e) { }
      return null;
    }
  }

  public static void main() throws Exception {
    host = "localhost"; //Integer.parseInt(args[2]);
    int portUser = 12345; //Integer.parseInt(args[1]);
    int portAdmin = 54321; //Integer.parseInt(args[2]);
    //host= "172.19.101.75"; //Integer.parseInt(args[2]);

    client = Client.create();
    ActorRef usersManager = new UsersManager().spawn();
    ActorRef roomsManager = new RoomsManager().spawn();

    AcceptorUser acceptorUser = new AcceptorUser(portUser, usersManager, roomsManager);
    AcceptorAdmin acceptorAdmin = new AcceptorAdmin(portAdmin, usersManager, roomsManager);

    acceptorUser.spawn();
    acceptorAdmin.spawn();
    System.out.println("\nServer ready !!");
    acceptorUser.join();
    acceptorAdmin.join();
  }
}