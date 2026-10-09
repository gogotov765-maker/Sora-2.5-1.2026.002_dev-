# Sora 2.5 — версия 1.2026.002_dev

Нативное Android-приложение (Kotlin + Compose) и сервер генерации (Python).

## Что внутри
- `app/` — вход и регистрация (почта + пароль или номер телефона по SMS, поверх обоев),
  лента, создание (текст / персонаж / картинка), профиль с черновиками и публикацией.
- `server/` — сервер: генерирует видео, вшивает вотермарку (хромакей, 60%, угол меняется
  каждые 5 с), раздаёт файл. Без `FAL_KEY` работает демо-режим.
- `firestore.rules` — правила базы. `.github/workflows/build.yml` — сборка APK в облаке.

## Что нужно сделать
1. **`google-services.json`** для пакета `com.sora25new.app` положи в `app/`
   (скачай в Firebase: Project settings → Your apps; имя файла ровно `google-services.json`).
2. **Firebase → Authentication → Sign-in method**: включи **Email/Password** и **Phone**.
   - Для телефона добавленные SHA-1 и SHA-256 обязательны. Для тестов без SMS: Phone →
     «Phone numbers for testing» (тестовый номер + код). Лимиты на SMS проверь в консоли.
3. **Firestore**: Build → Firestore Database → Create, затем Rules → вставь `firestore.rules` → Publish.
   Аккаунты сохраняются в коллекции `users` (почта и/или телефон), видео — в `videos`.
4. **Обои**: замени `app/src/main/res/drawable/login_bg.webp` на свои (другой формат — удали `.webp`).
5. **Сервер** (нужен ffmpeg): `cd server && pip install -r requirements.txt`, ключ сервисного
   аккаунта сохрани как `server/service-account.json`, переменные из `.env.example`,
   запуск: `uvicorn main:app --host 0.0.0.0 --port 8000`.
6. **API модели**: впиши `FAL_KEY` и проверь id моделей на fal.ai.
7. **APK без Android Studio**: репозиторий GitHub + Secrets `KEYSTORE_BASE64`,
   `KEYSTORE_PASSWORD`, `BACKEND_URL` → Actions → Build APK. Файлы: `com.sora25new.app.apk` и по архитектурам.

## Заметки
- Проект не собирался в этой среде (нет Android SDK): первая сборка может потребовать правок.
- В манифесте включён http для теста; для боевого сервера используй https.
