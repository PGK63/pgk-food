package com.example.pgk_food.shared.ui.components

object ActionHelpCatalog {
    private val details: Map<String, String> = mapOf(
        "nav.back" to "Назад.",
        "nav.settings" to "Открыть настройки.",
        "session.logout" to "Выйти из аккаунта.",
        "users.create" to "Создать пользователя.",
        "users.filter.reset" to "Сбросить фильтры.",
        "users.group.quick-create" to "Создать пользователя в этой группе.",
        "users.row.edit" to "Открыть карточку пользователя.",
        "users.row.delete" to "Удалить пользователя.",
        "groups.create" to "Создать группу.",
        "groups.assign.curator" to "Назначить куратора.",
        "groups.transfer" to "Перевести или переименовать.",
        "groups.unassign.curator" to "Снять куратора.",
        "groups.delete" to "Удалить группу.",
        "groups.member.remove" to "Убрать из группы.",
        "scanner.sync" to "Синхронизировать данные.",
        "scanner.download" to "Загрузить данные.",
        "menu.add.item" to "Добавить блюдо.",
        "menu.item.delete" to "Удалить блюдо.",
        "menu.date.pick" to "Выбрать дату меню.",
        "menu.copy.date.pick" to "Выбрать дату копирования.",
        "roster.copy-day" to "Копировать день.",
        "roster.date.pick" to "Выбрать дату табеля.",
        "roster.copy.date.pick" to "Выбрать дату для копии.",
        "roster.absence.from.pick" to "Выбрать начало периода.",
        "roster.absence.to.pick" to "Выбрать конец периода.",
        "stats.date.pick" to "Выбрать дату статистики.",
        "categories.group.search" to "Выбрать группу.",
        "reports.group.pick" to "Выбрать группу для отчета.",
        "search.clear" to "Очистить поиск.",
        "dialog.close" to "Закрыть окно.",
        "password.visibility" to "Показать или скрыть пароль.",
    )

    fun resolve(actionId: String?, fallbackDescription: String?): String? {
        val normalizedActionId = actionId?.trim().takeUnless { it.isNullOrBlank() }
        val detailed = normalizedActionId?.let(details::get)
        if (!detailed.isNullOrBlank()) return detailed
        return fallbackDescription?.trim().takeUnless { it.isNullOrBlank() }
    }
}
