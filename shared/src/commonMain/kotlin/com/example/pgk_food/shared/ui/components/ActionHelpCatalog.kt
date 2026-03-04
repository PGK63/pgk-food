package com.example.pgk_food.shared.ui.components

object ActionHelpCatalog {
    private val details: Map<String, String> = mapOf(
        "nav.back" to "Возвращает на предыдущий экран без сохранения несохраненных изменений.",
        "nav.settings" to "Открывает настройки уведомлений, подсказок и масштаба интерфейса.",
        "session.logout" to "Завершает текущую сессию и переводит на экран входа.",
        "users.create" to "Создает нового пользователя. После создания обязательно сохраните выданные логин и пароль.",
        "users.filter.reset" to "Сбрасывает выбранные фильтры и показывает полный список.",
        "users.group.quick-create" to "Открывает создание пользователя с предзаполненной текущей группой.",
        "users.row.edit" to "Открывает карточку пользователя для изменения ролей и данных.",
        "users.row.delete" to "Удаляет пользователя. Действие необратимо.",
        "groups.create" to "Создает новую группу.",
        "groups.assign.curator" to "Назначает куратора выбранной группе.",
        "groups.transfer" to "Переводит или переименовывает группу.",
        "groups.unassign.curator" to "Снимает куратора с группы.",
        "groups.delete" to "Удаляет группу после проверки, что студенты перенесены.",
        "groups.member.remove" to "Убирает участника из текущей группы.",
        "scanner.sync" to "Синхронизирует накопленные оффлайн-сканы с сервером.",
        "scanner.download" to "Загружает свежие ключи и справочники для стабильной работы сканера.",
        "menu.add.item" to "Открывает форму добавления нового блюда на выбранную дату.",
        "menu.item.delete" to "Удаляет выбранное блюдо из меню на текущую дату.",
        "menu.date.pick" to "Открывает выбор даты, для которой редактируется меню.",
        "menu.copy.date.pick" to "Открывает выбор даты, куда будет скопировано меню.",
        "roster.copy-day" to "Копирует отметки выбранного дня на другую дату. Перед подтверждением проверьте целевую дату.",
        "roster.date.pick" to "Открывает выбор даты табеля. Сохраните изменения перед переключением даты.",
        "roster.copy.date.pick" to "Открывает выбор даты, на которую будут скопированы отметки.",
        "roster.absence.from.pick" to "Открывает календарь для выбора даты начала периода отсутствия.",
        "roster.absence.to.pick" to "Открывает календарь для выбора даты окончания периода отсутствия.",
        "stats.date.pick" to "Открывает выбор даты для просмотра статистики группы.",
        "categories.group.search" to "Открывает поиск и выбор группы для редактирования категорий.",
        "reports.group.pick" to "Открывает выбор группы для фильтра отчета.",
        "search.clear" to "Очищает поисковую строку.",
        "dialog.close" to "Закрывает текущее окно.",
        "password.visibility" to "Показывает или скрывает введенный пароль.",
    )

    fun resolve(actionId: String?, fallbackDescription: String?): String? {
        val normalizedActionId = actionId?.trim().takeUnless { it.isNullOrBlank() }
        val detailed = normalizedActionId?.let(details::get)
        if (!detailed.isNullOrBlank()) return detailed
        return fallbackDescription?.trim().takeUnless { it.isNullOrBlank() }
    }
}
