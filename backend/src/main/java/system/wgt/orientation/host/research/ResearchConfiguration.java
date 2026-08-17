package system.wgt.orientation.host.research;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import system.wgt.orientation.application.research.SpatialResearchPromptService;

@Configuration
public class ResearchConfiguration {
    @Bean
    SpatialResearchPromptService spatialResearchPromptService() {
        return new SpatialResearchPromptService();
    }
}
