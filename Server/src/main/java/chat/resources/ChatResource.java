package chat.resources;

import chat.representations.*;
import com.google.common.base.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.ArrayList;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;


@Path("/chat")
@Produces(MediaType.APPLICATION_JSON)
public class ChatResource {

  private String apresentacao;
  private Rooms rooms;
  private Users users;

  public ChatResource(String apresentacao) {
    this.apresentacao = apresentacao;
    this.rooms = new Rooms();
    this.users = new Users();
  }

  /** CHAT **/
  @GET
  public String apresentacao() {
    return apresentacao;
  }

  /** USERS **/
  @GET
  @Path("/users")
  public String listarUsers() {
    return users.listarUsers();
  }

  /** USER **/
  @GET  
  @Path("/user/{email}")
  public String showUser(@PathParam("email") String email) {
    return users.showUser(email);
  }

  @POST 
  @Path("/user")
  public Response criarUser(@QueryParam("email") String email, @QueryParam("nickname") String nickname) {
    if(users.containsUser(email)) 
      return Response.status(409).build();

    users.addUser(new User(nickname, email));
    return Response.ok().build();
  }

  @DELETE
  @Path("/user/{email}")
  public Response deleteUser(@PathParam("email") String email) {
    User user = users.getUser(email);
    if(user == null) 
      return Response.status(405).build();

    users.deleteUser(email);
    return Response.ok().build();
  }

  @PUT 
  @Path("/user")
  public Response loggarUser(@QueryParam("email") String email) {
    users.loggUser(email);
    return Response.ok().build();
  }

  /** ROOMS **/
  @GET
  @Path("/rooms")
  public String listarRooms() {
    return rooms.listarRooms();
  }

  /** ROOM **/
  @GET  
  @Path("/room/{name}")
  public String infoRoom(@PathParam("name") String name) {
    return rooms.showRoom(name);
  }

  @POST
  @Path("/room/{name}")
  public Response criarRoom(@PathParam("name") String name) {
    if(rooms.containsRoom(name)) 
      return Response.status(409).build();

    rooms.addRoom(new Room(name));
    return Response.ok().build();
  }

  @DELETE
  @Path("/room/{name}")
  public Response deleteRoom(@PathParam("name") String name) {
    if(!rooms.containsRoom(name)) 
      return Response.status(405).build();

    if(rooms.getRoom(name).totalUsers() != 0) 
      return Response.status(405).build();
    
    rooms.deleteRoom(name);
    return Response.ok().build();
  }

  /** ROOMS/USERS **/
  @PUT
  @Path("/room/{name}/user")
  public Response putUserInRoom(@PathParam("name") String name, @QueryParam("email") String email, @QueryParam("nickname") String nickname) {
    if(!rooms.containsRoom(name))
      return Response.status(405).build();

    if(rooms.containsUser(name, email)) 
      return Response.status(405).build();

    rooms.putUserInRoom(name, nickname, email);
    return Response.ok().build();
  }

  @DELETE
  @Path("/room/{name}/user")
  public Response removeUserFromRoom(@PathParam("name") String name, @QueryParam("email") String email) {
    if(!rooms.containsRoom(name)) 
      return Response.status(405).build();

    if(!rooms.containsUser(name, email)) 
      return Response.status(405).build();

    rooms.removeUserFromRoom(name, email);
    return Response.ok().build();
  }
}