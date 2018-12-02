package chat;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import chat.resources.ChatResource;
import chat.health.ChatHealthCheck;

import co.paralleluniverse.fibers.dropwizard.FiberApplication;
import co.paralleluniverse.fibers.dropwizard.FiberDBIFactory;
import co.paralleluniverse.fibers.dropwizard.FiberHttpClientBuilder;

public class ChatApplication extends FiberApplication<ChatConfiguration> {
	public static void main(String[] args) throws Exception {
	    new ChatApplication().run(args);
	    ChatServer server = new ChatServer();
	    server.main();
	}

	@Override
	public String getName() { return "Chat SD !!"; }

	@Override
	public void initialize(Bootstrap<ChatConfiguration> bootstrap) { }

	@Override
	public void fiberRun(ChatConfiguration configuration, Environment environment) throws Exception {
    environment.jersey().register(
                    new ChatResource(configuration.getApresentacao()));
    // environment.jersey().register(new ChatServer());
    environment.healthChecks().register("Chat Health Check",new ChatHealthCheck());
	}
}