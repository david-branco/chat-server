package chat.health;

import com.codahale.metrics.health.HealthCheck;

public class ChatHealthCheck extends HealthCheck {

	//public ChatHealthCheck() { }

	@Override
	protected Result check() throws Exception {
		return Result.healthy();
	}
}