package chat.representations;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.InetSocketAddress;
import co.paralleluniverse.actors.*;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.io.*;
import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;

public class Users {
	private HashMap<String,User> users; 

  @JsonCreator
  public Users() {
    this.users = new HashMap<String,User>();
  }

  @JsonProperty
  public void addUser(User user) { users.put(user.getEmail(), user); }

  @JsonProperty
  public User getUser(String email) { return users.get(email); }

  @JsonProperty
  public Boolean containsUser(String email) { return users.containsKey(email); }

  @JsonProperty
  public int totalUsers() {
    return users.size();
  }

  @JsonProperty
  public String showUser(String email) { 
    User u = users.get(email);
    return (u != null)? u.toString() : "User not found\n";
  }

  @JsonProperty
  public boolean deleteUser(String email, String password) {
    User u = users.get(email);
    if(u.getPassword().equals(password)) { 
     users.remove(email); 
     return true;
   }
   return false;
  }

  @JsonProperty
  public void deleteUser(String email) { users.remove(email); }

  @JsonProperty
  public void updateUser(String email, String nickname, String password) { 
    User user = users.get(email);
    user.updateUser(nickname, password);
    users.put(email, user);
  }

  @JsonProperty
  public void loggUser(String email) { 
    User user = users.get(email);
    if(user != null) {
      user.changeLogged();
      users.put(email, user);
    }
  }

  @JsonProperty
  public Users getUsers() {
    Users aux = new Users();
    for(User u: users.values())
      aux.addUser(u);

    return aux;
  }

  @JsonProperty
  public String listarUsers() {
    if(users.isEmpty())
      return "Without Users\n";

    StringBuilder s = new StringBuilder();
    for (User u: users.values()) {
      s.append(u.toString());
      //s.append(u.getLogged());
    }
    return s.toString();
  }   

  @JsonProperty
  public void sendMessage(String from, String to, String message) {
    Message m = new Message(from, to, message);
    getUser(to).sendMessage(m);
  }
}

