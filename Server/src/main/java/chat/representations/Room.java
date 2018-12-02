package chat.representations;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.InetSocketAddress;
import co.paralleluniverse.actors.*;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.io.*;
import com.fasterxml.jackson.annotation.*;

import java.util.ArrayList;
import java.util.Iterator;

public class Room {
	private String name;
  private ActorRef self; 
  private ArrayList<User> users;

  @JsonCreator
	public Room (String name) {
		this.name = name;
    this.self = null;
    this.users = new ArrayList<User>();
	}

  @JsonCreator
  public Room (String name, ActorRef self) {
    this.name = name;
    this.self = self;
    this.users = new ArrayList<User>();
  }

  @JsonProperty
  public String getName() { return this.name; }

  @JsonProperty
  public ActorRef getRef() { return this.self; }

  @JsonProperty
  public Boolean containsUser(String email) { 
    Iterator<User> iter = users.iterator();
    while(iter.hasNext())
      if(iter.next().getEmail().equals(email))
        return true;
    return false;  
  }

  @JsonProperty
  public void setName(String name) { this.name = name; }

  @JsonProperty
  public void putUser(User user) { users.add(user); }

  @JsonProperty
  public void putUser(String nickname, String email, ActorRef self) { 
    users.add(new User(nickname, email, self)); 
  }

  @JsonProperty
  public void removeUser(User user) { users.remove(user); }

  @JsonProperty
  public User getUser(String email) { 
    Iterator<User> iter = users.iterator();
    while(iter.hasNext()) {
      User aux = iter.next();
      if(aux.getEmail().equals(email))
        return aux;
    }
    return null;
  }

  @JsonProperty
  public void removeUser(String email) {
    Iterator<User> iter = users.iterator();
    while(iter.hasNext()) {
      User u = iter.next();
      if(u.getEmail().equals(email)) {
        removeUser(u); 
        break;
      }
    }
  }

  @JsonProperty
  public int totalUsers() {
    return users.size();
  }

  @JsonProperty
  public ArrayList<User> getUsers() {
    ArrayList<User> aux = new ArrayList<User>();
    for(User u: this.users)
      aux.add(u);

    return aux;
  }

  @JsonProperty
  public String listarUsers() { 
    StringBuilder s = new StringBuilder();
    for(User user: users)
      s.append("\t"+user.toString());
    return s.toString(); 
  }

  @JsonProperty
  public String toString() {
  	StringBuilder s = new StringBuilder();
  	s.append(name+"\n");
    s.append("List of Users:\n");
    s.append(listarUsers());
    s.append("\n");
  	return s.toString();
  }
}