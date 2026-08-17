package system.wgt.orientation.infrastructure.routing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(ValhallaProperties.class)
public class ValhallaConfiguration {

    @Bean
    ValhallaRoutingAdapter valhallaRoutingAdapter(ObjectMapper objectMapper, ValhallaProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        RestClient client = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", properties.getUserAgent())
                .build();
        return new ValhallaRoutingAdapter(client, objectMapper);
    }
}
