# Kafka de Pedidos 360

Infraestructura de Kafka, separada del backend.

| Archivo | Qué levanta |
|---|---|
| `docker-compose.kafka.yml` | Zookeeper y 2 brokers (`kafka-1` y `kafka-2`) |
| `docker-compose.kafka-ui.yml` | Kafka UI, para administrar brokers y tópicos |
| `docker-compose.kafka-admin.yml` | `kafka-admin-service`: administra tópicos y consumer groups por REST |

## Cómo levantarlo

Una sola vez por computador (crea la red que comparten Kafka y el backend):

```
docker network create kafka_net
```

Después, en este orden:

```
docker compose -f kafka/docker-compose.kafka.yml up -d
docker compose -f kafka/docker-compose.kafka-ui.yml up -d
docker compose -f kafka/docker-compose.kafka-admin.yml up -d --build
```

- Kafka UI: http://localhost:8080
- Administrador (API REST, requiere JWT con rol AUDITORIA): http://localhost:8084/api/topics

Al arrancar, el administrador crea los tópicos del sistema (`pedidos.eventos` e `inventario.stock`) si no existen.

## Cómo apagarlo

```
docker compose -f kafka/docker-compose.kafka-admin.yml down
docker compose -f kafka/docker-compose.kafka-ui.yml down
docker compose -f kafka/docker-compose.kafka.yml down
```

Los eventos quedan guardados en volúmenes; no se pierden al apagar. Para borrarlos, agregar `-v` al `down` del clúster.

## Notas

- El backend (`docker-compose.yml` de la raíz) también usa la red `kafka_net`. Si no vas a usar Kafka, igual debe existir (`docker network create kafka_net`).
- Los tópicos no se crean automáticamente: los crea el administrador o Kafka UI.
- Desde otros contenedores, los brokers están en `kafka-1:29092` y `kafka-2:29093` (red `kafka_net`).
- Desde el computador, en `localhost:9092` y `localhost:9093`.