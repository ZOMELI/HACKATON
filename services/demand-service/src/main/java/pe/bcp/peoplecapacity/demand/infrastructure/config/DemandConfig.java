package pe.bcp.peoplecapacity.demand.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pe.bcp.peoplecapacity.demand.domain.CandidateScorer;

@Configuration
class DemandConfig implements WebMvcConfigurer {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CandidateScorer candidateScorer() {
        return new CandidateScorer();
    }

    /** La web de la demo corre en Vite (puerto 5173). */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
