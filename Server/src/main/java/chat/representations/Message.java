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

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class Message {

	private String from;
	private String to;
	private String message;
	private Date date;

	@JsonCreator
	public Message() {
		this.from = null;
		this.to = null;
		this.message = null;
		this.date = null;
	}

	@JsonCreator
	public Message(String from, String to, String message) {
		this.from = from;
		this.to = to;
		this.message = message;
		this.date = new Date();
	}

	@JsonProperty
	public String getFrom() { return this.from; }

	@JsonProperty
	public String getTo() { return this.to; }

	@JsonProperty
	public String getMessage() { return this.message; }

	@JsonProperty
	public String getDate() { 
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		return new String(dateFormat.format(this.date)); 
	}

	@JsonProperty
	public void setFrom(String from) { this.from = from; }

	@JsonProperty
	public void setTo(String to) { this.to = to; }

	@JsonProperty
	public void setMessage(String message) { this.message = message; }

	@JsonProperty
	public void updateDate() { this.date = new Date(); }

	@JsonProperty
  public String toString() {
  	StringBuilder s = new StringBuilder();
  	s.append("From: " +this.from);
  	s.append(" at:" +getDate()+"\n");
  	s.append(this.message +"\n\n");
  	return s.toString();
  }
} 