package cl.duoc.duocconecta.prompts.config;

import cl.duoc.duocconecta.comun.seguridad.DocumentacionApi;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación OpenAPI de ms-prompts, visible en Swagger UI. */
@Configuration
public class ConfiguracionOpenApi {

    @Bean
    public OpenAPI definicionDeLaApi() {
        return DocumentacionApi.de("ms-prompts",
                "Biblioteca de prompts: publicar, buscar, bifurcar, valorar y moderar.");
    }
}
