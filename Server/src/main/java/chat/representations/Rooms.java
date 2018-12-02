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

public class Rooms {
	private ArrayList<Room> rooms;

  @JsonCreator
  public Rooms() {
    this.rooms = new ArrayList<Room>();
  }
  
  @JsonProperty
  public void addRoom(Room room) { rooms.add(room); }

  @JsonProperty
  public int indexOf(Room room) { return rooms.indexOf(room); }

  @JsonProperty
  public void set(Integer index, Room room) { rooms.set(index, room); }

  @JsonProperty
  public Room getRoom(String name) { 
    Iterator<Room> iter = rooms.iterator();
    while(iter.hasNext()) {
      Room aux = iter.next();
      if(aux.getName().equals(name))
        return aux;
    }
    return null;
  }

  @JsonProperty
  public void updateRoom(String name, String newName) { 
    Room room = getRoom(name);
    int index = rooms.indexOf(room);
    room.setName(newName);
    rooms.set(index, room);
  }

  @JsonProperty
  public void deleteRoom(String name) { rooms.remove(getRoom(name)); }

  @JsonProperty
  public Boolean containsUser(String name, String email) {
    return getRoom(name).containsUser(email);
  }

  @JsonProperty
  public Boolean containsRoom(String name) { 
    Iterator<Room> iter = rooms.iterator();
    while(iter.hasNext())
      if(iter.next().getName().equals(name))
        return true;
    return false;  
  }

  @JsonProperty
  public String showRoom(String name) { 
    Room room = getRoom(name);
    return (room != null)? room.toString() : "Room not found\n";
  }

  @JsonProperty
  public Rooms getRooms() {
    Rooms aux = new Rooms();
    for(Room room: rooms)
      aux.addRoom(room);

    return aux;
  }

  @JsonProperty
  public String listarRooms() {
    if(rooms.isEmpty())
      return "Without Rooms\n";

    StringBuilder s = new StringBuilder();
    for (Room room: rooms) 
      s.append(room.toString());
    
    return s.toString();
  }

  @JsonProperty
  public void putUserInRoom(String name, String nickname, String email) {
    Room room = getRoom(name);
    int index = rooms.indexOf(room);
    User user = new User(nickname, email);
    room.putUser(user);
    rooms.set(index, room); 
  }

  public void removeUserFromRoom(String name, String email) {
    if(containsRoom(name)) {
      Room room = getRoom(name);
      if(room.containsUser(email)) {
        int index = rooms.indexOf(room);
        room.removeUser(email);
        rooms.set(index, room); 
      }
    }
  }

  public String whichRoomUser(String email) {
    Iterator<Room> iter = rooms.iterator();
    while(iter.hasNext()) {
      String nomeSala = iter.next().getName();
      if(containsUser(nomeSala, email)) 
        return nomeSala;
    }
    return null;
  }

  public void removeUserFromRooms(String email) {
    Iterator<Room> iter = rooms.iterator();
    while(iter.hasNext()) {
      String nomeSala = iter.next().getName();
      if(containsUser(nomeSala, email)) {
        removeUserFromRoom(nomeSala, email);
        break;
      }
    }
  }
}