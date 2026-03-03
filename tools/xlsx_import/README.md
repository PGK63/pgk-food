# XLSX Import Utility

Утилита импортирует из Excel-файла группы, кураторов, студентов и категории через backend API.

## Что делает

- Читает `.xlsx` с колонками (`ФИО обучающегося`, `статус`, `Группа`, `Категория`, `Завтрак`, `Обед`, `Комментарии...`, `Классный руководитель`).
- Импортирует только строки со статусом `студент`.
- Категории маппит в backend enum:
  - `СВО -> SVO`
  - `Многодетные -> MANY_CHILDREN`
- Автоматически пытается исправить слепленное ФИО студента по CamelCase.
- Дубликаты студентов в одной группе отправляет в `reject.csv`.
- Режим `clean-load`: перед импортом проверяет, что в системе нет групп/кураторов/студентов.

## Установка

```bash
cd tools/xlsx_import
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

> В системах с PEP 668 (externally-managed-environment) используйте именно `venv`.

## Запуск

`dry-run` (по умолчанию, без записи в API):

```bash
export IMPORT_PASSWORD='your_password'
.venv/bin/python tools/xlsx_import/import_xlsx.py \
  --xlsx "/home/wsr/Загрузки/Telegram Desktop/питание.xlsx" \
  --base-url "http://localhost:8080" \
  --login "admin" \
  --password-env "IMPORT_PASSWORD" \
  --out-dir "/tmp/pgk-import-run"
```

`apply` (с записью в API):

```bash
export IMPORT_PASSWORD='your_password'
.venv/bin/python tools/xlsx_import/import_xlsx.py \
  --xlsx "/home/wsr/Загрузки/Telegram Desktop/питание.xlsx" \
  --base-url "http://localhost:8080" \
  --login "admin" \
  --password-env "IMPORT_PASSWORD" \
  --out-dir "/tmp/pgk-import-run" \
  --apply
```

## CLI параметры

- `--xlsx` путь к входному файлу.
- `--base-url` базовый URL backend.
- `--login` логин администратора/регистратора.
- `--password-env` имя переменной окружения с паролем.
- `--out-dir` директория для артефактов.
- `--apply` включает реальную запись в API (без флага работает `dry-run`).
- `--disable-rollback` отключает rollback при ошибке в середине импорта.
- `--timeout` HTTP timeout (сек), по умолчанию `15`.
- `--max-attempts` количество попыток для network/5xx, по умолчанию `4`.

## Выходные файлы

В `--out-dir` создаются:

- `report.json` — итоговый отчет (статусы, счетчики, preflight, rollback).
- `reject.csv` — отклоненные строки с причиной.
- `credentials.xlsx` — учетные данные:
  - лист `Кураторы`;
  - лист `Студенты`.

Файл `credentials.xlsx` создается с правами `600` на POSIX.

## Тесты

```bash
cd /home/wsr/pgk-food
tools/xlsx_import/.venv/bin/python -m unittest discover -s tools/xlsx_import/tests -t .
```

## Ограничения текущей итерации

- Колонки `Завтрак/Обед/Комментарии` не импортируются в `/api/v1/roster`.
- Дата рождения не импортируется.
- Импорт рассчитан на чистую схему (breaking change rollout).
