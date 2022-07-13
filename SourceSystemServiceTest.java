package com.fmr.authapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fmr.authapp.config.SourceSystemProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URISyntaxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@Import({SourceSystemServiceTest.Config.class, JacksonAutoConfiguration.class})
@ExtendWith(SpringExtension.class)
public class SourceSystemServiceTest {


  @TestConfiguration
  static class Config {
    @Bean
    public WireMockServer webServer() {
      WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());
      wireMockServer.start();
      return wireMockServer;
    }

    @Bean
    public WebClient webClient(WireMockServer server) {
      return WebClient.builder().baseUrl(server.baseUrl()).build();
    }

    @Bean
    public SourceSystemProperties SourceSystemProperties() {
      SourceSystemProperties sourceSystemProperties =  new SourceSystemProperties();
      return sourceSystemProperties;
    }

    @Bean
    public SourceSystemService client(WebClient webClient, SourceSystemProperties sourceSystemProperties) {
      sourceSystemProperties.setLoginUrl("/api/todos");
      sourceSystemProperties.setUrl("/api/todos");
      return new SourceSystemService(webClient, sourceSystemProperties, new ObjectMapper());
    }
  }

  @Autowired
  private WireMockServer wireMockServer;

  @Autowired
  SourceSystemService webClientApiService;

  @Autowired
  SourceSystemProperties sourceSystemProperties;


  @AfterEach
  public void afterEach() {
    this.wireMockServer.resetAll();
  }


  @Test
  void testGetAllTodosShouldReturnDataFromClientWithSuccessResponse() throws URISyntaxException, JsonProcessingException {
    this.wireMockServer.stubFor(
        WireMock.get(sourceSystemProperties.getUrl())
            .willReturn(aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("response-200.json"))
    );

    this.wireMockServer.stubFor(
        WireMock.post(sourceSystemProperties.getLoginUrl())
            .willReturn(aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("auth-response.json"))
    );
    webClientApiService.callPostApi();
  }

  @Test
  void testGetAllTodosShouldReturnDataFromClientWith404Resposne() throws URISyntaxException, JsonProcessingException {
    this.wireMockServer.stubFor(
        WireMock.get(sourceSystemProperties.getUrl())
            .willReturn(aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("response-200.json"))
    );

    this.wireMockServer.stubFor(
        WireMock.post(sourceSystemProperties.getLoginUrl())
            .willReturn(aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withBodyFile("auth-response.json"))
    );

    ResponseStatusException thrown = assertThrows(
        ResponseStatusException.class,
        () -> webClientApiService.callPostApi()
    );
    assertNotNull(thrown);
    assertEquals(thrown.getStatus(),HttpStatus.NOT_FOUND);
  }

  @Test
  void testGetAllTodosShouldReturnDataFromClientWith500Resposne() throws URISyntaxException, JsonProcessingException {
    this.wireMockServer.stubFor(
        WireMock.get(sourceSystemProperties.getUrl())
            .willReturn(aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBodyFile("response-200.json"))
    );

    this.wireMockServer.stubFor(
        WireMock.post(sourceSystemProperties.getLoginUrl())
            .willReturn(aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())
                .withBodyFile("auth-response.json"))
    );

    ResponseStatusException thrown = assertThrows(
        ResponseStatusException.class,
        () -> webClientApiService.callPostApi()
    );
    assertNotNull(thrown);
    assertEquals(thrown.getStatus(),HttpStatus.SERVICE_UNAVAILABLE);
  }

}
