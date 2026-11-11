package cl.duoc.duocconecta.analitica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** ms-analitica: consume Kafka y arma las vistas de tendencia, alertas y métricas. */
@SpringBootApplication
@EnableScheduling
public class MsAnaliticaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAnaliticaApplication.class, args);
    }
}
