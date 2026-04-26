# Cloud Service Diploma

Backend для дипломного проекта "Облачное хранилище".

Сервис реализует REST API для авторизации пользователя и работы с файлами: загрузка, получение списка, скачивание, переименование и удаление.

## Стек

- Java 17
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Docker / Docker Compose
- JUnit, Mockito, Testcontainers

## Запуск

```bash
docker compose up -d --build
```

Backend будет доступен по адресу:

```text
http://localhost:8080
```

Проверка:

```bash
curl http://localhost:8080/actuator/health
```

Тестовый пользователь:

```text
login: user@example.com
password: password
```

## Фронтенд

Во фронтенде нужно указать:

```env
VUE_APP_BASE_URL=http://localhost:8080
```

Рекомендуемый запуск фронтенда:

```bash
npm run serve -- --port 8081
```

CORS по умолчанию разрешен для:

- `http://localhost:8080`
- `http://localhost:8081`

## API

| Метод | URL | Описание |
| --- | --- | --- |
| `POST` | `/login` | Авторизация |
| `POST` | `/logout` | Выход |
| `GET` | `/list?limit=10` | Список файлов |
| `POST` | `/file?filename=name.txt` | Загрузка файла |
| `GET` | `/file?filename=name.txt` | Скачивание файла |
| `PUT` | `/file?filename=name.txt` | Переименование файла |
| `DELETE` | `/file?filename=name.txt` | Удаление файла |

Авторизованные запросы используют заголовок:

```http
auth-token: <TOKEN>
```

Формат ответа при успешном логине:

```json
{"auth-token":"..."}
```

Формат ошибок:

```json
{"message":"..."}
```

## Архитектура

- `controller` — REST-контроллеры
- `dto` — request/response модели
- `service` — бизнес-логика
- `repository` — доступ к PostgreSQL
- `entity` — JPA-сущности
- `storage` — хранение файлов на диске
- `security` — проверка `auth-token` и `@CurrentUser`
- `config` — конфигурация приложения
- `exception` — единая обработка ошибок

Файлы хранятся на диске отдельно от БД. В PostgreSQL хранятся пользователи, hash токенов сессий и метаданные файлов.

Авторизация настроена через Spring Security `SecurityFilterChain`. Form login и HTTP Basic отключены, используется stateless-проверка заголовка `auth-token`. JWT не используется: клиент получает raw token, а в PostgreSQL хранится только SHA-256 hash токена. Это позволяет деактивировать сессию при `/logout` и не хранить raw token в БД.

## Конфигурация

Основной конфиг:

```text
src/main/resources/application.yml
```

Основные переменные окружения:

| Переменная | Значение по умолчанию |
| --- | --- |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `cloudservice` |
| `DB_USERNAME` | `cloudservice` |
| `DB_PASSWORD` | `cloudservice` |
| `SERVER_PORT` | `8080` |
| `APP_STORAGE_ROOT` | `./storage` |

## База Данных

При запуске через Docker Compose:

```text
Host: localhost
Port: 5432
Database: cloudservice
Username: cloudservice
Password: cloudservice
```

Схема БД создается через Flyway:

```text
src/main/resources/db/migration/V1__init_schema.sql
```

Истекшие токены периодически удаляются scheduled-задачей. Интервал задается настройкой:

```yaml
app:
  auth:
    expired-token-cleanup-delay-ms: 3600000
```

## Тесты

```bash
./gradlew test
```

В тестах есть unit-тесты сервисов и интеграционный тест полного сценария работы с файлами через `MockMvc + Testcontainers`.
