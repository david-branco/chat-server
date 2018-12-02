package chat.representations;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.InetSocketAddress;
import co.paralleluniverse.actors.*;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.io.*;
import com.fasterxml.jackson.annotation.*;

import java.nio.*;
import java.nio.charset.*;

import java.util.*;

public class User {
	private String nickname;
  private String email;
  private String password;
  private ActorRef self; 
  private boolean logged;
  private LinkedList<Message> inbox;

  @JsonCreator
  public User() {
    this.nickname = null;
    this.email = null;
    this.password = null;
    this.self = null;
    this.logged = false;
    this.inbox = new LinkedList<Message>();
  }

  @JsonCreator
  public User(String nickname, String email) {
    this.nickname = nickname;
    this.email = email;
    this.password = null;
    this.self = null;
    this.logged = true;
    this.inbox = new LinkedList<Message>();
  }

  @JsonCreator
  public User(String nickname, String email, ActorRef self) {
    this.nickname = nickname;
    this.email = email;
    this.password = null;
    this.self = self;
    this.logged = true;
    this.inbox = new LinkedList<Message>();
  }

  @JsonCreator
  public User(String nickname, String email, String password, ActorRef self) {
    this.nickname = nickname;
    this.email = email;
    this.password = password;
    this.self = self;
    this.logged = true;
    this.inbox = new LinkedList<Message>();
  }


  @JsonProperty
  public String getNickname() { return this.nickname; }

  @JsonProperty
  public String getEmail() { return this.email; }

  @JsonProperty
  public String getPassword() { return this.password; }

  @JsonProperty
  public ActorRef getRef() { return this.self; }

  @JsonProperty
  public boolean getLogged() { return this.logged; }

  @JsonProperty
  public void setNickname(String nickname) { this.nickname = nickname; }

  @JsonProperty
  public void setEmail(String email) { this.email = email; }

  @JsonProperty
  public void setPassword(String password) { this.password = password; }

  @JsonProperty
  public void setRef(ActorRef self) { this.self = self; }

  @JsonProperty
  public void changeLogged() { 
    this.logged = !this.logged;
  }

  @JsonProperty
  public void reset() {
    this.nickname = null;
    this.email = null;
    this.password = null;
    this.logged = false;
  }

  @JsonProperty
  public void updateUser(String nickname, String password) { 
    this.nickname = nickname;
    this.password = password;
  }

  @JsonProperty
  public String toString() {
  	StringBuilder s = new StringBuilder();
  	s.append(nickname+" - "); 
  	s.append(email+" - ");
    if(logged) s.append("Online\n");
    else { s.append("Offline\n"); }
    //s.append(password+"\n"); 
  	return s.toString();
  }

  @JsonProperty
  public void sendMessage(Message message) {
    inbox.add(message);
    if(inbox.size() > 3) {
      inbox.removeFirst();
    }
  }

  @JsonProperty
  public String getMessages() {
    
    if(inbox.isEmpty()) 
      return "Inbox is Empty !!\n";

    StringBuilder s = new StringBuilder();
    s.append("\n\tINBOX \n");
    for (Message m : inbox)
      s.append(m.toString());

    return s.toString();
  }
}

