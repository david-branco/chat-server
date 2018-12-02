package chat;

import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.db.DataSourceFactory;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ChatConfiguration extends Configuration {

	@NotEmpty
	private String apresentacao;

  public String getApresentacao() { return this.apresentacao; }

  public void setApresentacao(String apresentacao) { this.apresentacao = apresentacao; }


}

