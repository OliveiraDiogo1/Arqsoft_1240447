package pt.psoft.g1.psoftg1.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HttpClientConfiguration {

    @Bean
    public RestTemplate externalRestTemplate(
            RestTemplateBuilder builder,
            @Value("${bibliographic.http.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${bibliographic.http.read-timeout-ms:5000}") int readTimeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
