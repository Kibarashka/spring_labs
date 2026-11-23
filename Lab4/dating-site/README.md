# Сайт знайомств - ЛР №4 (RESTful API)

Spring Boot 4.0.8, Java 21, springdoc-openapi 3.0.3, Jakarta JSON-P (Parsson) для JSON Patch / Merge Patch.
HTML-інтерфейс із ЛР №3 збережено, REST API додано в пакет `api`.

## Запуск

```
./mvnw spring-boot:run        # або: mvn spring-boot:run
```

- REST API: http://localhost:8080/api/profiles, http://localhost:8080/api/invitations
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs (JSON), `/v3/api-docs.yaml`; копія - `docs/openapi.yaml`
- Веб-інтерфейс: http://localhost:8080

Операції від імені користувача (PUT/PATCH/DELETE анкети, private-info, відповідь на запрошення)
потребують заголовок `X-User-Id` - імітація автентифікації, безпека не реалізується (п. 2.5).

## Postman

1. Postman → Import → `postman/dating-site.postman_collection.json` та `postman/dating-site.postman_environment.json`.
2. Обрати environment `dating-site local` (`baseUrl = http://localhost:8080`).
3. Запустити застосунок «з нуля» (дані в пам'яті) і виконати колекцію в Collection Runner.

Або з консолі:

```
npm install -g newman
newman run postman/dating-site.postman_collection.json -e postman/dating-site.postman_environment.json
```

Результат останнього прогону: `postman/results/newman-cli.txt` (59 запитів, 310 перевірок, 0 помилок).

## Тести

```
./mvnw test
```

`RestApiTests` (8) + `WebLayerTests` (7) + `DatingSiteApplicationTests` (1).
