# HTTP Notification Management Server

Навчальний сервер на чистому Java, який демонструє власну реалізацію HTTP, Basic‑автентифікації та CRUD-операцій над користувачами/сповіщеннями в SQLite.

## Як працює
1. `HttpServer` відкриває `ServerSocket` на порту `1488` і для кожного вхідного з’єднання створює окремий `Thread`.
2. `HttpParser.parseHTTP` читає рядок запиту, заголовки, Basic Auth та тіло (враховуючи `Content-Length` і chunked-encoding) і повертає масив `[method, path, version, username, password, body]`.
3. `HttpServer.handleClient` верифікує користувача через `Authenticator.isAuthenticated`, визначає HTTP-метод і делегує роботу відповідному *handler*-у (`GetHandler`, `PutHandler`, `PostHandler`, `DeleteHandler`).
4. Обробник виконує операцію через `DataBaseWrapper`, логікує дію й формує HTTP-відповідь (JSON або plain text).

## Збирання / запуск
```bash
mvn clean package          # стандартний Maven-проєкт
java -cp target/classes org.example.server.HttpServer

# або напряму з вихідників
javac -cp . src/main/java/org/example/**/*.java
java -cp src/main/java org.example.server.HttpServer
```

## Структура репозиторію
```
src/main/java/org/example/
├── Main.java
├── README.md              ← цей файл
├── db/
│   └── DataBaseWrapper.java
├── logger/
│   └── Logger.java
├── parsers/
│   ├── HttpParser.java
│   └── JsonParser.java
├── server/
│   ├── Authenticator.java
│   ├── DeleteHandler.java
│   ├── GetHandler.java
│   ├── HttpServer.java
│   ├── PostHandler.java
│   └── PutHandler.java
└── structures/
    └── NotificationInfo.java
```

## Довідник по класах і функціях

### `org.example.Main`
- `IS_DEBUG` – прапорець для умовних логів (поки не використовується).
- `main(String[] args)` – точка входу для експериментів; залишена порожньою.

### `org.example.logger.Logger`
- `setOutput(OutputType type)` – вмикає логування в консоль або файл.
- `setLogFilePath(String path)` – змінює цільовий файл (дефолт: `application.log`).
- `info | warn | error(String)` – базові рівні логування.
- `error(String, Throwable)` – записує повідомлення плюс стек винятку.
- `log(String level, String message, Throwable throwable)` – спільна гілка, що визначає викликаючий клас і обирає цільовий стрім.
- `getCallerClassName()` – бере ім'я класу з стеку, щоб у логах було видно джерело.
- `writeToFile(String logEntry, Throwable t)` – аппендить рядок у файл (try-with-resources).

### `org.example.db.DataBaseWrapper`
- **Конструктори** `DataBaseWrapper()` та `DataBaseWrapper(String url)` – відкривають SQLite-з’єднання (за потреби з альтернативним шляхом).
- `demo()` / `demo(DataBaseWrapper db)` – швидке створення таблиць і тестових користувачів; корисно при запуску.
- `connect()` / `closeDbConnection()` – життєвий цикл JDBC-з’єднання.
- `setAuthenticationDB()` / `setNotificationsDB()` – створюють таблиці `authentications` та `notifications` (DDL знаходиться в `String`-блоках).
- `ensureAuthUsernameColumn()` / `hasColumn()` – перевіряють наявність поля `username` і додають його, якщо відсутнє.
- `sha256(String input)` – допоміжний метод для хешування логіна/пароля.
- `queryUserByUsernameHash(String usernameHash)` – приватний хелпер, що повертає `ResultSet` (використовується під час розширень, зараз не викликається напряму).
- `addClient(String username, String password, int isAdmin)` – додає користувача, попередньо перевіряючи, що його не існує (`findClientStatus`).
- `removeClient(String username, String password)` – видаляє користувача за ID (порожній пароль у виклику означає «просто за ім'ям»).
- `findClientStatus(String username, String password)` – повертає 0 (немає), -1 (неправильний пароль), 1 (звичайний), 2 (адмін).
- `getClientID(String username, String password)` – шукає ID користувача у таблиці `authentications`.
- `addNotifications(List<NotificationInfo>, int clientID)` – масово зберігає сповіщення і повертає список `id` із SQLite.
- `getDbForClient(int clientID)` – читає всі сповіщення користувача (`SELECT id, title, payload, fire_at`).
- `removeNotification(int clientID, int notificationID)` – видаляє запис; якщо `clientID == -1`, діє як суперюзер.
- `printAuthTable()` / `printNotificationsTable()` – діагностичні дампи в stdout.

### `org.example.parsers.HttpParser`
- `parseHTTP(BufferedReader in, Socket socket)` – розбирає request line, основні заголовки (Authorization, Content-Length, Transfer-Encoding) та тіло; повертає масив із 6 елементів.
- `readFixedLengthBody(BufferedReader in, int length)` – дочитує рівно `Content-Length` символів (підходить для JSON/ASCII).
- `readChunkedBody(BufferedReader in)` – реалізація chunked transfer encoding (size → дані → CRLF).

### `org.example.parsers.JsonParser`
- `parseJson2Auth(String jsonBody)` – дістає `username`, `password`, `isAdmin`; логіка використовується у POST `/users/add/...`.
- `parseJsons2Usernames(String jsonBody)` – перетворює масив об’єктів на `ArrayList<String>` для DELETE `/users/delete/superuser`.
- `parseJson2NotificationsId(String jsonBody)` – витягує `notificationId` зі списку (використовується в DELETE `/notifications/delete/...`).

### `org.example.structures.NotificationInfo`
- Поля `clientID`, `notificationID`, `title`, `payload`, `fireAt`.
- Геттери `getClientID()`, `getNotificationID()`, `getTitle()`, `getPayload()`, `getFireAt()`.
- Сеттери для кожного поля й метод `toString()` для зручного логування.

### `org.example.server.HttpServer`
- `main(String[] args)` – створює `ServerSocket`, піднімає `DataBaseWrapper`, викликає `DataBaseWrapper.demo(db)` та у циклі приймає клієнтів.
- `handleClient(Socket socket, DataBaseWrapper db)` – основний цикл обробки: читає запит, визначає статус користувача (`produceAuthAndGetId`), роутить за HTTP-методом і відсилає відповіді.
- `produceAuthAndGetId(String username, String password, DataBaseWrapper db)` – повертає масив `[status, clientId]`, де `status` узятий з `Authenticator`.
- `sendHttpNotFound(Socket socket, String message)` / `sendHttpNotFound(Socket)` – повертають 404 (plain text).
- `sendHttpOk(Socket socket, String message)` – загальна обгортка для 200 OK з текстом.
- `sendHttpAuthError(Socket socket, String message)` / без тексту – відсилають 401.
- `sendHttpAccepted(Socket socket, String message)` / без тексту – відсилають 202 (використовується при дублюванні користувача).

### `org.example.server.Authenticator`
- `isAuthenticated(String username, String password, DataBaseWrapper db)` – повертає 0/1/2 залежно від `findClientStatus`; усі handler-и покладаються на ці статуси.

### `org.example.server.GetHandler`
- `handleGet(Socket socket, DataBaseWrapper db, String[] parsedHTTP)` – маршрутизує GET-запити (`/notifications/get`, `/users/status`).
- `getNotificationsForClient(Socket socket, int clientID, DataBaseWrapper db)` – читає сповіщення, конвертує у JSON-масив і повертає його клієнту.
- `sendStatus(Socket socket, boolean status)` – відповідає JSON `{"isAdmin":true|false}`.
- `sendHttpJson(Socket socket, String json)` – універсальний відправник JSON-відповідей.

### `org.example.server.PutHandler`
- `fromByte2Array(String jsonBody, int clientID)` – десеріалізує JSON-масив у список `NotificationInfo`.
- `handlePut(Socket socket, DataBaseWrapper db, int clientID, String[] parsedHTTP)` – обробляє шлях `/notifications/put`.
- `putNotificationFromClient(Socket socket, DataBaseWrapper db, String jsonBody, int clientID)` – валідує тіло, зберігає сповіщення і повертає `{"status":"ok","clientId":...,"webIds":[...]}`.

### `org.example.server.PostHandler`
- `handlePost(Socket socket, DataBaseWrapper db, String[] parsedHTTP)` та перевантажена версія з `userStatus` – відповідають за `/users/add/manually` і `/users/add/superuser`.
- Логіка: розбирає JSON (`parseJson2Auth`), перевіряє, чи існує користувач; для `superuser` додатково перевіряє `userStatus`.

### `org.example.server.DeleteHandler`
- `handleDelete(Socket socket, DataBaseWrapper db, String[] parsedHTTP)` та перевантаження з `userStatus` – маршрути для `/users/delete/superuser`, `/notifications/delete/{manually|superuser}`.
- Для користувачів потрібен статус суперюзера; для видалення власних сповіщень – достатньо звичайного акаунта.

## HTTP API (curl-friendly)
Сервер за замовчуванням слухає `http://localhost:1488`. Усі ендпоїнти, окрім `POST /users/add/manually`, вимагають Basic Auth.

### PUT `/notifications/put`
```json
[
  {
    "id": 1,
    "title": "Wake up",
    "payload": "Morning alarm",
    "fireAt": 1730000000
  }
]
```

```shell
curl -v -u myuser:mypass -X PUT \
  -H "Content-Type: application/json" \
  -d '[{"id":1,"title":"Wake up","payload":"Morning alarm","fireAt":1730000000}]' \
  http://localhost:1488/notifications/put
```

### GET `/notifications/get`
```shell
curl -v -u myuser:mypass -X GET http://localhost:1488/notifications/get
```

### GET `/users/status`
```shell
curl -v -u admin:admin -X GET http://localhost:1488/users/status
```

### POST `/users/add/manually`
Аутентифікація не потрібна.
```shell
curl -X POST "http://localhost:1488/users/add/manually" \
  -H "Content-Type: application/json" \
  -d '{"username":"volodymyr","password":"secret123","isAdmin":0}'
```

### POST `/users/add/superuser`
```shell
curl -v -u admin:admin -X POST "http://localhost:1488/users/add/superuser" \
  -H "Content-Type: application/json" \
  -d '{"username":"pipiskanegra","password":"secret123","isAdmin":1}'
```
Без авторизації сервер поверне `You are not superuser`.

### DELETE `/users/delete/superuser`
```shell
curl -u admin:admin -X DELETE http://localhost:1488/users/delete/superuser \
  -H "Content-Type: application/json" \
  -d '[{"username":"myuser"}]'
```

### DELETE `/notifications/delete/manually`
```shell
curl -u myuser:mypass -X DELETE http://localhost:1488/notifications/delete/manually \
  -H "Content-Type: application/json" \
  -d '[{"notificationId": 1}]'
```

### DELETE `/notifications/delete/superuser`
```shell
curl -u admin:admin -X DELETE http://localhost:1488/notifications/delete/superuser \
  -H "Content-Type: application/json" \
  -d '[{"notificationId": 1}]'
```

