# Kafka de Pedidos 360

Infraestructura de Kafka, separada del backend.

| Archivo | Qué levanta |
|---|---|
| `docker-compose.kafka.yml` | Zookeeper y 2 brokers (`kafka-1` y `kafka-2`) |
| `docker-compose.kafka-ui.yml` | Kafka UI, para administrar brokers y tópicos |

## Cómo levantarlo

```
docker compose -f kafka/docker-compose.kafka.yml up -d
docker compose -f kafka/docker-compose.kafka-ui.yml up -d
```

Kafka UI: http://localhost:8080

## Cómo apagarlo

```
docker compose -f kafka/docker-compose.kafka-ui.yml down
docker compose -f kafka/docker-compose.kafka.yml down
```

Los eventos quedan guardados en volúmenes; no se pierden al apagar. Para borrarlos, agregar `-v` al `down` del clúster.

## Notas

- Los tópicos no se crean automáticamente: se crean con el administrador de Kafka o con Kafka UI.
- Desde otros contenedores, los brokers están en `kafka-1:29092` y `kafka-2:29093` (red `kafka_net`).
- Desde el computador, en `localhost:9092` y `localhost:9093`.