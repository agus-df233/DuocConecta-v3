package cl.duoc.duocconecta.analitica.config;

import cl.duoc.duocconecta.comun.seguridad.DocumentacionApi;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación OpenAPI de ms-analitica, visible en Swagger UI. */
@Configuration
public class ConfiguracionOpenApi {

    @Bean
    public OpenAPI definicionDeLaApi() {
        return DocumentacionApi.de("ms-analitica",
                "Consumidores de Kafka: tendencia de proyectos y prompts, alertas de actividad y métricas por hora.");
    }
}
