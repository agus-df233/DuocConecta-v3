#!/usr/bin/env bash
# Crea los tópicos de DuocConecta y les reaplica la configuración. Se puede correr las veces
# que haga falta: lo que existe no se recrea, solo se vuelve a dejar con estos valores.
#
#   BOOTSTRAP=localhost:9092 ./docker/kafka/topicos.sh
#
# Estos valores son los mismos de duocconecta.kafka.topicos en ms-mensajeria/application.yml.
# Si cambias uno, cambia el otro.
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP:-kafka-1:29092,kafka-2:29092,kafka-3:29092}"
PARTICIONES=3
REPLICAS=3

DIA=86400000
HORA=3600000

# nombre | retención (ms) | tamaño de segmento (ms) | por qué
TOPICOS=(
  # Interacciones con proyectos y prompts (clave: id del recurso). 7 días = la ventana de
  # tendencia, y permite releer para reconstruir la vista. No lleva quién actuó.
  "duocconecta.contenido.interacciones|$((7 * DIA))|$DIA"
  # Actividad por persona (clave: oid). Lleva identificadores de persona y las ventanas de
  # detección son de minutos: se guarda lo mínimo. El segmento corto hace que las 24 h se
  # cumplan, porque la retención borra segmentos enteros, nunca mensajes sueltos.
  "duocconecta.usuarios.actividad|$DIA|$HORA"
  # Lo que los consumidores no pudieron procesar: 14 días para revisarlo, como las DLQ de SQS.
  "duocconecta.contenido.interacciones.dlt|$((14 * DIA))|$DIA"
  "duocconecta.usuarios.actividad.dlt|$((14 * DIA))|$DIA"
)

for linea in "${TOPICOS[@]}"; do
  IFS='|' read -r nombre retencion segmento <<<"$linea"
  config="cleanup.policy=delete,retention.ms=$retencion,segment.ms=$segmento,min.insync.replicas=2"
  opciones=()
  IFS=',' read -ra pares <<<"$config"
  for par in "${pares[@]}"; do opciones+=(--config "$par"); done

  kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$nombre" --partitions "$PARTICIONES" --replication-factor "$REPLICAS" \
    "${opciones[@]}" >/dev/null
  kafka-configs --bootstrap-server "$BOOTSTRAP" --alter --entity-type topics \
    --entity-name "$nombre" --add-config "$config" >/dev/null
  echo "listo: $nombre ($config)"
done

kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic 'duocconecta\..*' | grep -v '^\s*Topic:.*Partition:' || true
