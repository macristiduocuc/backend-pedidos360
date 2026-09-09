# Pedidos 360 — Backend

Backend de la plataforma Pedidos 360: 2 microservicios Spring Boot + PostgreSQL, corriendo en local con Docker.

```
backend/
├── docker-compose.yml       # PostgreSQL 18 + pgAdmin
├── db/init/                 # Schema y datos de ejemplo (se cargan solos la primera vez)
├── inventario-service/      # Catálogo, categorías, locales, stock (puerto 8081)
└── pedidos-service/         # Pedidos, ítems, auditoría (puerto 8082)
```

`pedidos-service` le habla por HTTP a `inventario-service` (consulta precio/nombre y reserva stock de forma atómica al crear un pedido). Ambos comparten la misma base de datos física, pero cada uno solo toca sus propias tablas — no hay JPA cruzado entre ellos.

## Requisitos

- **Docker Desktop** (para PostgreSQL y pgAdmin)
- **JDK 21** — [Temurin](https://adoptium.net/temurin/releases/?version=21)
- **VS Code** con estas extensiones:
  - Extension Pack for Java (Microsoft)
  - Spring Boot Extension Pack (VMware) — incluye Spring Boot Dashboard
  - REST Client (Huachao Mao) — para probar los endpoints sin Postman

## Cómo levantar todo (orden importa)

### 1. Base de datos

```bash
docker compose up -d
```

Espera a que quede sana:

```bash
docker compose ps
```

`pedidos360-db` debe decir `healthy`. La primera vez que se crea el volumen, Postgres corre automáticamente `db/init/01_schema.sql` y `db/init/02_seed.sql` (6 productos de ejemplo). Si vuelves a correr esto sobre un volumen ya existente, esos scripts **no** se vuelven a ejecutar.

- Postgres: `localhost:5432` — DB `pedidos360_db`, usuario `pedidos360`, clave `pedidos360`
- pgAdmin (opcional, para ver los datos con interfaz gráfica): http://localhost:5050 — `admin@pedidos360.com` / `pedidos360`. Al agregar el servidor en pgAdmin, el **Host** es `db-pedidos` (el nombre del servicio en `docker-compose.yml`), no `localhost`.

> Estas credenciales son solo para desarrollo local, por eso están en el repo sin problema. Nunca se usan así en producción.

### 2. Microservicio de Inventario

Abre la carpeta `inventario-service/` en VS Code (una ventana separada). Espera a que la barra inferior diga **"Java: Ready"** (la primera vez descarga las dependencias de Maven, puede tardar uno o dos minutos).

Corre la app con cualquiera de estas opciones:
- Clic en **Run** sobre el método `main` de `InventarioServiceApplication.java`
- Panel **Spring Boot Dashboard** (ícono en la barra lateral) → botón de play sobre `inventario-service`
- Terminal integrada: `.\mvnw.cmd spring-boot:run` (Windows) o `./mvnw spring-boot:run` (Mac/Linux)

Verifica en `http://localhost:8081/api/productos` — debe devolver un JSON con 6 productos.

### 3. Microservicio de Pedidos

Mismo procedimiento, en otra ventana de VS Code, con la carpeta `pedidos-service/`. Corre `PedidosServiceApplication.java`.

Verifica en `http://localhost:8082/api/pedidos` — debe devolver `[]` si todavía no se ha creado ningún pedido.

> Si `pedidos-service` no logra conectarse a `inventario-service` al crear un pedido, confirma que Inventario esté corriendo **antes** de probar el endpoint de crear pedido.

## Probar los endpoints sin frontend

`pedidos-service/pruebas.http` ya trae ejemplos listos para usar con la extensión REST Client: crear pedido, cambiar estado, marcar entregado, ver auditoría, y el caso de stock insuficiente (409). Ábrelo en VS Code y haz clic en "Send Request" sobre cada bloque.

## Referencia rápida de endpoints

**Inventario (`:8081`)**
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/productos` | Lista productos activos (`?categoria=` opcional) |
| GET | `/api/productos/{id}` | Detalle de un producto |
| POST | `/api/productos` | Crear producto |
| PUT | `/api/productos/{id}` | Editar producto |
| DELETE | `/api/productos/{id}` | Baja lógica (`activo=false`) |
| PATCH | `/api/productos/{id}/reservar-stock` | Descuento atómico de stock (usado por Pedidos) |
| GET | `/api/categorias` | Lista categorías |
| GET | `/api/locales` | Lista locales |

**Pedidos (`:8082`)**
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/pedidos` | Lista pedidos (`?estado=` opcional) |
| GET | `/api/pedidos/cliente/{correo}` | Pedidos de un cliente |
| GET | `/api/pedidos/{id}` | Detalle de un pedido |
| POST | `/api/pedidos` | Crear pedido (llama a Inventario) |
| PATCH | `/api/pedidos/{id}/estado` | Cocina: `En preparación` / `Hecho` |
| PATCH | `/api/pedidos/{id}/entregar` | Despacho: marca `Entregado` |
| GET | `/api/pedidos/resumen` | KPIs para el dashboard de Auditoría |
| GET | `/api/eventos-auditoria` | Log de trazabilidad |

## Solución de problemas conocidos

- **`Restarting (1)` en `pedidos360-db`**: casi siempre es un volumen viejo de una versión distinta de Postgres. Solución: `docker compose down -v` y `docker compose up -d` de nuevo (borra los datos de prueba, se recargan solos).
- **pgAdmin no acepta el email**: dominios como `.local` están bloqueados por su validador; por eso usamos `@pedidos360.com`.
- **CORS**: ambos microservicios ya tienen habilitado `http://localhost:4200` y `https://pedidos360.duckdns.org` en `config/CorsConfig.java`. Si el frontend corre en otro puerto/dominio, hay que agregarlo ahí.
- **`ddl-auto=validate` tira error al arrancar**: significa que una entidad Java no calza con una columna real de la base (tipo de dato, nombre, nulabilidad). Revisa el mensaje de Hibernate, dice exactamente qué columna no coincide.

## Próximos pasos (no incluidos todavía)

- Seguridad JWT con Azure AD (los endpoints hoy están abiertos, sin autenticación).
- RabbitMQ / Kafka para notificaciones y analítica en tiempo real.
- Dockerfile de cada microservicio + despliegue a EC2.
