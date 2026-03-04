package com.example.pgk_food.shared.ui.components

import com.example.pgk_food.shared.util.HintScreenKey

data class ScreenHintContent(
    val title: String = "Подсказка",
    val steps: List<String>,
    val note: String? = null,
    val inlineHints: List<String> = emptyList(),
)

object HintCatalog {
    fun content(screen: HintScreenKey): ScreenHintContent = when (screen) {
        HintScreenKey.STUDENT_DASHBOARD -> ScreenHintContent(
            title = "Быстрый старт",
            steps = listOf(
                "Проверьте талоны на сегодня.",
                "Откройте QR перед выдачей.",
            ),
        )

        HintScreenKey.STUDENT_COUPONS -> ScreenHintContent(
            title = "Статусы талонов",
            steps = listOf(
                "Доступен: можно открыть QR.",
                "Использован: повторная выдача недоступна.",
            ),
        )

        HintScreenKey.STUDENT_QR -> ScreenHintContent(
            title = "Показ QR",
            steps = listOf(
                "Не сворачивайте экран во время скана.",
                "После оффлайн-работы обновите данные.",
            ),
        )

        HintScreenKey.STUDENT_MENU -> ScreenHintContent(
            title = "Меню",
            steps = listOf(
                "Выберите нужную столовую.",
                "Проверьте тип питания у блюда.",
            ),
        )

        HintScreenKey.CHEF_DASHBOARD -> ScreenHintContent(
            title = "Повар",
            steps = listOf(
                "Сканер подтверждает выдачу питания.",
                "Меню и отчеты обновляйте ежедневно.",
            ),
        )

        HintScreenKey.CHEF_SCANNER -> ScreenHintContent(
            title = "Сканер",
            steps = listOf(
                "Проверьте ФИО и тип питания после скана.",
                "При сети синхронизируйте оффлайн-операции.",
            ),
        )

        HintScreenKey.CHEF_MENU_MANAGE -> ScreenHintContent(
            title = "Меню",
            steps = listOf(
                "Выберите дату и локацию.",
                "После импорта проверьте ошибки.",
            ),
        )

        HintScreenKey.CHEF_STATS -> ScreenHintContent(
            title = "История сканов",
            steps = listOf(
                "Пики отказов обычно связаны с QR или сетью.",
                "После оффлайн-сканов проверьте синхронизацию.",
            ),
        )

        HintScreenKey.REGISTRATOR_DASHBOARD -> ScreenHintContent(
            title = "Роль регистратора",
            steps = listOf(
                "Регистратор ведет учет пользователей и групп.",
                "Отчисление выполняет куратор.",
            ),
        )

        HintScreenKey.REGISTRATOR_USERS -> ScreenHintContent(
            title = "Пользователи",
            steps = listOf(
                "Проверьте фильтры перед массовыми действиями.",
                "После изменения ролей проверьте группу и категорию.",
            ),
        )

        HintScreenKey.REGISTRATOR_USER_CREATE -> ScreenHintContent(
            title = "Создание пользователя",
            steps = listOf(
                "Заполните все обязательные поля.",
                "Сохраните выданные логин и пароль.",
            ),
        )

        HintScreenKey.REGISTRATOR_GROUPS -> ScreenHintContent(
            title = "Группы",
            steps = listOf(
                "Перед удалением переведите студентов.",
                "После изменений проверьте состав группы.",
            ),
        )

        HintScreenKey.CURATOR_DASHBOARD -> ScreenHintContent(
            title = "Роль куратора",
            steps = listOf(
                "Заполняйте табель вовремя.",
                "Категории и отчеты ведите по актуальным данным.",
            ),
        )

        HintScreenKey.CURATOR_ROSTER -> ScreenHintContent(
            title = "Табель",
            steps = listOf(
                "Выберите дату и группу.",
                "После изменений нажмите «Сохранить».",
            ),
        )

        HintScreenKey.CURATOR_STATS -> ScreenHintContent(
            title = "Статистика",
            steps = listOf(
                "Проверьте дату и группу перед анализом.",
                "Если данных нет, проверьте заполнение табеля.",
            ),
        )

        HintScreenKey.CURATOR_CATEGORIES -> ScreenHintContent(
            title = "Категории",
            steps = listOf(
                "Категория должна совпадать с текущим статусом студента.",
                "После изменения проверьте карточку студента.",
            ),
        )

        HintScreenKey.CURATOR_REPORTS -> ScreenHintContent(
            title = "Отчеты",
            steps = listOf(
                "Сначала задайте период и группу.",
                "Перед экспортом проверьте строки отчета.",
            ),
        )

        HintScreenKey.ADMIN_DASHBOARD -> ScreenHintContent(
            title = "Администратор",
            steps = listOf(
                "Контролируйте отчеты и подозрительные операции.",
                "Перед экспортом проверяйте фильтры.",
            ),
        )

        HintScreenKey.ADMIN_REPORTS -> ScreenHintContent(
            title = "Отчеты",
            steps = listOf(
                "Проверьте диапазон дат.",
                "При необходимости выберите группу.",
            ),
        )
    }
}
