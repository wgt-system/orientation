package system.wgt.orientation.host.discovery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import system.wgt.orientation.application.discovery.DiscoveryImportService;
import system.wgt.orientation.application.discovery.DiscoveryRepository;
import system.wgt.orientation.infrastructure.discovery.SQLiteDiscoveryRepository;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;

@Configuration
public class DiscoveryConfiguration {
    @Bean
    DiscoveryRepository discoveryRepository(@Value("${orientation.discovery.database-path}") String databasePath) {
        return new SQLiteDiscoveryRepository(Path.of(databasePath));
    }

    @Bean
    DiscoveryImportService discoveryImportService(ObjectMapper objectMapper, DiscoveryRepository repository) {
        return new DiscoveryImportService(objectMapper, repository);
    }
}
