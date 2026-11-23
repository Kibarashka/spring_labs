# Сайт знайомств - ЛР №3 (Thymeleaf)

Spring Boot 4.0.8, Java 21, Thymeleaf, Bean Validation. Дані зберігаються в пам'яті (Fake-репозиторії).

## Запуск

```
./mvnw spring-boot:run        # або: mvn spring-boot:run
```

Відкрити http://localhost:8080

## Сторінки

| URL | Що там |
|-----|--------|
| `/` | список анкет, пошук `?keyword=` |
| `/profiles/{id}` | анкета: закрита інформація (th:if/unless), дії (th:switch) |
| `/profiles/new` | створення анкети (валідація) |
| `/profiles/me/edit` | редагування власної анкети |
| `/invitations` | отримані та надіслані запрошення |

Поточний користувач обирається у шапці (бін `CurrentUserService` з областю видимості session),
аутентифікації немає (п. 2.5 завдання).

## Тести

```
./mvnw test
```

`WebLayerTests` - Elvis-оператор, пошук, екранування XSS, валідація форми, сценарій запрошень, 403, 404.
