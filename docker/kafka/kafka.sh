#!/usr/bin/env bash
# Administración de Kafka desde la línea de comandos, con las herramientas que trae la imagen.
# Corre donde está el docker-compose: en local o en la EC2 (por SSM).
#
#   ./docker/kafka/kafka.sh topicos                    lista los tópicos
#   ./docker/kafka/kafka.sh describir [tópico]         particiones, líder, réplicas e ISR
#   ./docker/kafka/kafka.sh configuracion <tópico>     retención, limpieza y demás
#   ./docker/kafka/kafka.sh retencion <tópico> <ms>    cambia la retención
#   ./docker/kafka/kafka.sh grupos                     lista los grupos de consumidores
#   ./docker/kafka/kafka.sh lag [grupo]                offsets, lag y quién atiende cada partición
#   ./docker/kafka/kafka.sh mensajes <tópico> [n]      los primeros n mensajes, con clave y partición
#   ./docker/kafka/kafka.sh caida <1|2|3>              detiene un broker (demo de tolerancia a fallos)
#   ./docker/kafka/kafka.sh levantar <1|2|3>           lo vuelve a encender
set -euo pipefail

INTERNO="kafka-1:29092,kafka-2:29092,kafka-3:29092"

# Las herramientas corren dentro de un broker encendido: si uno está caído, se usa otro.
broker_vivo() {
  for n in 1 2 3; do
    if [ "$(docker inspect -f '{{.State.Running}}' "duocconecta-kafka-$n" 2>/dev/null)" = "true" ]; then
      echo "duocconecta-kafka-$n"; return
    fi
  done
  echo "No hay ningún broker encendido." >&2; exit 1
}

kafka() { local herramienta=$1; shift; docker exec "$(broker_vivo)" "$herramienta" --bootstrap-server "$INTERNO" "$@"; }

exigir() { [ -n "${1:-}" ] || { echo "Falta $2. Uso: $0 ${3}" >&2; exit 2; }; }

case "${1:-}" in
  topicos)        kafka kafka-topics --list --exclude-internal ;;
  describir)      if [ -n "${2:-}" ]; then kafka kafka-topics --describe --topic "$2"
                  else kafka kafka-topics --describe --exclude-internal; fi ;;
  configuracion)  exigir "${2:-}" "el tópico" "configuracion <tópico>"
                  kafka kafka-configs --describe --entity-type topics --entity-name "$2" ;;
  retencion)      exigir "${2:-}" "el tópico" "retencion <tópico> <ms>"; exigir "${3:-}" "los ms" "retencion <tópico> <ms>"
                  kafka kafka-configs --alter --entity-type topics --entity-name "$2" --add-config "retention.ms=$3"
                  kafka kafka-configs --describe --entity-type topics --entity-name "$2" ;;
  grupos)         kafka kafka-consumer-groups --list ;;
  lag)            if [ -n "${2:-}" ]; then kafka kafka-consumer-groups --describe --group "$2"
                  else kafka kafka-consumer-groups --describe --all-groups; fi ;;
  mensajes)       exigir "${2:-}" "el tópico" "mensajes <tópico> [n]"
                  kafka kafka-console-consumer --topic "$2" --from-beginning --max-messages "${3:-10}" \
                    --timeout-ms 10000 --property print.key=true --property print.partition=true \
                    --property print.offset=true --property print.headers=true || true ;;
  caida)          exigir "${2:-}" "el número de broker" "caida <1|2|3>"
                  docker stop "duocconecta-kafka-$2" >/dev/null && echo "Broker $2 detenido." ;;
  levantar)       exigir "${2:-}" "el número de broker" "levantar <1|2|3>"
                  docker start "duocconecta-kafka-$2" >/dev/null && echo "Broker $2 encendido: vuelve al ISR cuando se ponga al día." ;;
  *)              sed -n '2,16p' "$0"; exit 2 ;;
esac
