package pe.bcp.peoplecapacity.cases.infrastructure.config;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pe.bcp.peoplecapacity.cases.application.port.in.ReconcileUseCase;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    public static final ZoneId ZONE = ZoneId.of("America/Lima");

    /** "Hoy" del servicio. cases.reference-date permite fijarlo para que la demo no dependa del día. */
    @Bean
    public Clock clock(@Value("${cases.reference-date:}") String referenceDate) {
        if (referenceDate == null || referenceDate.isBlank()) {
            return Clock.system(ZONE);
        }
        log.warn("Fecha de referencia fijada en {} (cases.reference-date)", referenceDate);
        return Clock.fixed(LocalDate.parse(referenceDate).atTime(9, 0).atZone(ZONE).toInstant(), ZONE);
    }

    public static SimpleClientHttpRequestFactory requestFactory(Duration connect, Duration read) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connect);
        factory.setReadTimeout(read);
        return factory;
    }

    @Bean
    public ApplicationRunner reconcileOnStartup(ReconcileUseCase reconcile,
                                                @Value("${cases.reconcile-on-startup:false}") boolean enabled) {
        return args -> {
            if (!enabled) {
                return;
            }
            try {
                reconcile.reconcile("startup");
            } catch (RuntimeException e) {
                log.warn("No se pudo conciliar al arrancar: {}", e.getMessage());
            }
        };
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("http://localhost:5173").allowedMethods("*").allowedHeaders("*");
    }
}
