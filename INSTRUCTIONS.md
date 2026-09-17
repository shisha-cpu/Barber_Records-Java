# Barber Records — инструкция

## Для клиента (запись на стрижку)

1. Откройте сайт: `http://localhost:8080`
2. Нажмите **«Записаться»**
3. **Услуга** — найдите через поиск или выберите из списка
4. **Дата** — нажмите на день в календаре
5. **Время** — выберите свободный слот
6. **Контакты** — введите ФИО и телефон
7. **Подтверждение** — проверьте данные и нажмите **«Подтвердить запись»**
8. После успеха нажмите **«На главную»**, чтобы записать ещё раз

### Ограничения (невидимы в интерфейсе)

- На один номер — ограничено число активных записей и записей в день
- С одного IP — ограничено число записей в час и в сутки
- Нельзя дважды записаться на одно и то же время с одного телефона

---

**Вход:** `http://localhost:8080/admin/login`  
Логин и пароль — в файле `.env`. **Не коммитьте `.env` в git.**

## Первый запуск после git clone

```powershell
copy .env.example .env
```

Задайте свои пароли в `.env`, затем:

```powershell
docker compose up --build -d
```

---

## Для администратора

### Вкладка «Календарь»

**Режим «Календарь»**
- Месячный календарь со счётчиком записей на каждый день
- Клик по дню → список всех записей на этот день
- **«Доступные записи»** → выбор услуги → свободные часы
- **«Поделиться PDF»** / **«Скачать PDF»** — отправить клиенту свободные окна

**Режим «За период»**
- Укажите даты «С» и «По» → все записи за период
- **«Доступные записи»** — свободные окна за период по выбранной услуге

### Вкладка «Услуги»

- Добавление, редактирование, отключение и удаление услуг
- У каждой услуги: название, длительность (мин), цена

### Вкладка «Дополнительно»

**Обеденный перерыв** — время, когда запись недоступна (клиентам не показывается)

**Лимиты записи** — защита от спама:
| Параметр | По умолчанию |
|----------|--------------|
| Активных записей на телефон | 2 |
| Записей в день на телефон | 1 |
| Записей в час с IP | 10 |
| Записей в сутки с IP | 20 |

**Резервные копии**
- Автоматически каждый день в **03:00**
- Вручную: **«Создать бэкап сейчас»**
- Скачивание JSON с услугами, записями и настройками
- В Docker хранятся в volume `app_backups`

---

## Запуск

### Локально

```powershell
# PostgreSQL (только БД)
docker compose up -d db

# Переменные из .env (PowerShell)
$env:SPRING_DATASOURCE_PASSWORD = "ваш_пароль"
$env:APP_ADMIN_PASSWORD = "ваш_пароль"
.\gradlew.bat bootRun
```

### Docker (всё)

```powershell
docker compose up --build -d
```

Сайт: http://localhost:8080  
Админ: http://localhost:8080/admin/login

### VPS с 1 GB RAM (важно)

**Не запускайте** `docker compose up --build` на слабом VPS — Gradle внутри Docker съедает всю память.  
На сервере используется `docker-compose.prod.yml` + `Dockerfile.runtime`: JAR собирается **на вашем ПК**, на VPS только пересборка лёгкого образа.

---

## Деплой и обновление на сервере (SSH)

**Ваш сервер:** `root@155.212.139.212`  
**Каталог на VPS:** `~/Barber_Records-Java`  
**Сайт:** http://155.212.139.212:8080  
**Админ:** http://155.212.139.212:8080/admin/login

Ниже — полный цикл: первый выклад и каждое обновление после изменений в коде.

### Что понадобится

| Где | Что |
|-----|-----|
| На ПК | Git, Java/Gradle (или `gradlew`), SSH-клиент (`scp`) |
| На VPS | Docker, Docker Compose, каталог `~/Barber_Records-Java` |
| На VPS | Файл `.env` с паролями (создаётся один раз, **не заливать в git**) |

---

### Первый выклад (один раз)

#### 1. Swap на VPS (рекомендуется при 1 GB RAM)

```bash
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

#### 2. Подготовка каталога на сервере

```bash
ssh root@155.212.139.212
mkdir -p ~/Barber_Records-Java/build/libs
cd ~/Barber_Records-Java
```

Скопируйте на сервер файлы для Docker (с ПК):

```powershell
cd C:\Users\qwe\Desktop\Barber_Records-Java

scp docker-compose.prod.yml Dockerfile.runtime .env.example root@155.212.139.212:~/Barber_Records-Java/
```

На сервере создайте `.env` и задайте пароли:

```bash
cd ~/Barber_Records-Java
cp .env.example .env
nano .env   # POSTGRES_PASSWORD, SPRING_DATASOURCE_PASSWORD, APP_ADMIN_PASSWORD
```

#### 3. Сборка JAR на ПК и загрузка на сервер

```powershell
cd C:\Users\qwe\Desktop\Barber_Records-Java
.\gradlew.bat bootJar --no-daemon
scp build\libs\*.jar root@155.212.139.212:~/Barber_Records-Java/build/libs/
```

#### 4. Запуск на VPS

```bash
ssh root@155.212.139.212
cd ~/Barber_Records-Java
docker compose -f docker-compose.prod.yml up -d --build
```

Проверка:

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f app
```

Сайт: http://155.212.139.212:8080  
Админ: http://155.212.139.212:8080/admin/login

---

### Обновление после изменений в коде (каждый новый build)

Кратко: **собрать JAR на ПК → scp на сервер → пересобрать контейнер app**.

#### Шаг 1. Сборка на ПК

```powershell
cd C:\Users\qwe\Desktop\Barber_Records-Java
git pull                    # если код тянете с git
.\gradlew.bat bootJar --no-daemon
```

Готовый файл: `build\libs\BarberRecods-0.0.1-SNAPSHOT.jar` (имя может немного отличаться).

#### Шаг 2. Копирование JAR на сервер

```powershell
scp build\libs\*.jar root@155.212.139.212:~/Barber_Records-Java/build/libs/
```

Если менялись только Java/шаблоны — достаточно JAR.  
Если менялись `docker-compose.prod.yml` или `Dockerfile.runtime`:

```powershell
scp docker-compose.prod.yml Dockerfile.runtime root@155.212.139.212:~/Barber_Records-Java/
```

#### Шаг 3. Перезапуск на сервере

```bash
ssh root@155.212.139.212
cd ~/Barber_Records-Java
docker compose -f docker-compose.prod.yml up -d --build
```

Команда пересоберёт образ `app` с новым JAR и перезапустит контейнер.  
База PostgreSQL и volume с бэкапами **не удаляются** — данные сохраняются.

#### Шаг 4. Проверка

```bash
docker compose -f docker-compose.prod.yml logs --tail 50 app
```

Успешный старт — строка вида `Started BarberRecodsApplication`.  
Ошибки миграций/БД смотрите в тех же логах.

---

### Полезные команды на VPS

```bash
# Статус контейнеров
docker compose -f docker-compose.prod.yml ps

# Логи приложения
docker compose -f docker-compose.prod.yml logs -f app

# Остановить всё
docker compose -f docker-compose.prod.yml down

# Остановить без удаления данных БД
docker compose -f docker-compose.prod.yml stop
```

**Не выполняйте** `docker compose down -v` без необходимости — флаг `-v` удалит volume с PostgreSQL и бэкапами.

---

### Частые проблемы

| Симптом | Что проверить |
|---------|----------------|
| `No such file build/libs/*.jar` при `docker compose build` | JAR не залили: повторите `bootJar` и `scp` |
| Приложение падает при старте | `docker compose ... logs app`, пароли в `.env` |
| Сайт не открывается снаружи | firewall / security group: порт **8080** |
| Out of memory на VPS | swap (см. выше), не собирайте проект Gradle на сервере |

---

### Локально (для разработки)

## Переменные окружения (Docker)

Задаются в файле `.env` (скопируйте из `.env.example`):

| Переменная | Описание |
|------------|----------|
| `POSTGRES_PASSWORD` | Пароль PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | Пароль БД для приложения |
| `APP_ADMIN_USERNAME` | Логин админа |
| `APP_ADMIN_PASSWORD` | Пароль админа |
