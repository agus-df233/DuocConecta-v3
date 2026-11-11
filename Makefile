# Puerta de entrada única a las tareas del proyecto.
# Envuelve infra/aws.sh para que nadie tenga que recordar los subcomandos.
#
#   make verificar                    Qué habilita el lab de AWS. Correr primero.
#   make crear                        Crea la infraestructura (ECR, ECS, RDS, ALB, API Gateway).
#   make mensajeria                   Tópico SNS, colas SQS y DLQ de la EP2. Idempotente.
#   make rabbitmq                     RabbitMQ en una EC2: un nodo. Idempotente.
#   make desplegar                    Construye las imágenes, las sube y actualiza el servicio.
#   make desplegar SERVICIO=bff-web   Igual, pero sube solo ese servicio.
#   make iniciar / make apagar        Prende y apaga las tareas. APAGAR al terminar la jornada.
#   make publicar                     Publica el frontend en S3 y actualiza el API Manager.
#   make local                        Levanta Postgres y compila todo en la máquina.
#   make servicio S=ms-analitica      Corre un servicio en segundo plano, con poca memoria.
#   make parar                        Detiene los servicios que levantó "make servicio".
#   make dev                          Sirve el frontend en localhost:5173.
#   make test                         Corre la suite completa.

.PHONY: verificar crear mensajeria rabbitmq desplegar publicar iniciar apagar urls local servicio parar test dev limpiar

# Si no se indica SERVICIO, se construyen los que existan en el repo.
SERVICIO ?=

verificar:
	./infra/aws.sh verificar

crear:
	./infra/aws.sh crear

# Tópico SNS, colas SQS y DLQ de la EP2. Idempotente: se puede correr sobre lo que ya existe.
mensajeria:
	./infra/aws.sh mensajeria

# El segundo broker: EC2 con el nodo de docker/rabbitmq. Idempotente.
rabbitmq:
	./infra/aws.sh rabbitmq

desplegar:
ifeq ($(SERVICIO),)
	./infra/aws.sh build ms-usuarios
	./infra/aws.sh build bff-web
	./infra/aws.sh build ms-proyectos
	./infra/aws.sh build ms-contacto
	./infra/aws.sh build ms-notificaciones
	./infra/aws.sh build ms-mensajeria
else
	./infra/aws.sh build $(SERVICIO)
endif
	./infra/aws.sh desplegar

iniciar:
	./infra/aws.sh iniciar

apagar:
	./infra/aws.sh apagar

urls:
	./infra/aws.sh urls

# --- Desarrollo local ---
local:
	docker compose up -d   # Postgres, RabbitMQ y Kafka (3 ZooKeeper, 3 brokers y Kafka UI)
	mvn -q clean install -DskipTests
	@echo "Listo. Levanta cada servicio con, por ejemplo:"
	@echo "  make servicio S=ms-usuarios"
	@echo "  make servicio S=bff-web"

# Un servicio desde su jar, en segundo plano y con heap chico: en un Mac de 8 GB caben seis a
# la vez junto al clúster de Kafka. Lee las variables de .env. Log: <servicio>/target/local.log
PERFIL ?= dev
servicio:
	@test -n "$(S)" || (echo "Uso: make servicio S=ms-analitica" && exit 1)
	@test -f $(S)/target/$(S)-0.0.1-SNAPSHOT.jar || (echo "Falta compilar: make local" && exit 1)
	@set -a; . ./.env; set +a; \
	  JAVA_TOOL_OPTIONS="-Xmx256m -XX:+UseSerialGC -XX:TieredStopAtLevel=1" \
	  nohup java -jar $(S)/target/$(S)-0.0.1-SNAPSHOT.jar --spring.profiles.active=$(PERFIL) \
	  > $(S)/target/local.log 2>&1 &
	@echo "$(S) arrancando. Log: $(S)/target/local.log"

# Detiene todo lo que levantó "make servicio" (los reconoce por el nombre del jar).
parar:
	@pkill -f -- '-0.0.1-SNAPSHOT.jar --spring.profiles.active' || true
	@echo "Servicios detenidos."

test:
	mvn test

# Sirve el frontend en la máquina, contra el backend local.
dev:
	cd frontend-web && npm install && npm run dev

# Compila el frontend y lo publica en S3, y deja las rutas del API Manager al día.
publicar:
	./infra/aws.sh front

limpiar:
	mvn -q clean
	docker compose down
