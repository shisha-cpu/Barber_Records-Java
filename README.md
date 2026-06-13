# Barber Records

Приложение для записи клиентов на стрижку: публичная запись, админ-панель, PostgreSQL, Docker.

## Быстрый старт после клонирования

### 1. Секреты

```powershell
copy .env.example .env
```

Отредактируйте `.env` — задайте пароли БД и админа.

### 2. Docker (рекомендуется)

```powershell
docker compose up --build -d
```

- Сайт: http://localhost:8080  
- Админ: http://localhost:8080/admin/login  

### 3. Локальный запуск (без Docker для app)

```powershell
docker compose up -d db

# PowerShell — подставьте значения из .env
$env:SPRING_DATASOURCE_PASSWORD = "ваш_пароль"
$env:APP_ADMIN_PASSWORD = "ваш_пароль"
.\gradlew.bat bootRun
```

## Что не попадает в git

| Файл | Содержимое |
|------|------------|
| `.env` | Пароли для Docker и локального запуска |
| `backups/` | Резервные копии с данными клиентов |

В репозитории только шаблон: `.env.example`.

## Подробная инструкция

См. [INSTRUCTIONS.md](INSTRUCTIONS.md)

## Стек

- Java 25, Groovy, Spring Boot 3.4
- PostgreSQL 16
- Thymeleaf, Docker
