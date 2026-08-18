package system.wgt.orientation.infrastructure.journey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(MotisProperties.class)
public class MotisConfiguration {

    @Bean
    MotisJourneyAdapter motisJourneyAdapter(ObjectMapper objectMapper, MotisProperties properties) {
        return new MotisJourneyAdapter(client(properties), objectMapper);
    }

    @Bean
    MotisPlaceAdapter motisPlaceAdapter(ObjectMapper objectMapper, MotisProperties properties) {
        return new MotisPlaceAdapter(client(properties), objectMapper);
    }

    private RestClient client(MotisProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", properties.getUserAgent())
                .build();
    }
}
