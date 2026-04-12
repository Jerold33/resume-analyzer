package com.example.resumeanalyzer.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient openAiWebClient(OpenAiProperties openAiProperties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) openAiProperties.connectTimeoutMs())
                .responseTimeout(Duration.ofMillis(openAiProperties.readTimeoutMs()));

        return WebClient.builder()
                .baseUrl(openAiProperties.baseUrl().replaceAll("/$", ""))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
