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
            title = "Сценарий",
            steps = listOf(
                "Откройте талоны и проверьте доступность на сегодня.",
                "Перед выдачей откройте QR на нужный прием пищи.",
            ),
        )

        HintScreenKey.STUDENT_COUPONS -> ScreenHintContent(
            title = "Проверка талонов",
            steps = listOf(
                "«Доступен» — можно сразу открывать QR.",
                "«Использован» — повторная выдача недоступна.",
            ),
        )

        HintScreenKey.STUDENT_QR -> ScreenHintContent(
            title = "Показ QR",
            steps = listOf(
                "Покажите QR повару и дождитесь результата скана.",
                "Если были оффлайн, после сети нажмите обновление.",
            ),
        )

        HintScreenKey.STUDENT_MENU -> ScreenHintContent(
            title = "Проверка меню",
            steps = listOf(
                "Выберите дату и локацию столовой.",
                "Проверьте блюда для своего типа питания.",
            ),
        )

        HintScreenKey.CHEF_DASHBOARD -> ScreenHintContent(
            title = "Смена повара",
            steps = listOf(
                "Перед стартом проверьте синхронизацию и откройте сканер.",
                "Подтверждайте недельный отчет только в открытом окне.",
            ),
        )

        HintScreenKey.CHEF_SCANNER -> ScreenHintContent(
            title = "Тест сканера",
            steps = listOf(
                "Сканируйте QR и сразу сверяйте ФИО и тип питания.",
                "Если есть несинхронизированные операции — нажмите синхронизацию.",
            ),
        )

        HintScreenKey.CHEF_MENU_MANAGE -> ScreenHintContent(
            title = "Тест меню",
            steps = listOf(
                "Выберите дату, затем добавьте блюдо или импортируйте CSV.",
                "После копирования меню проверьте целевую дату.",
            ),
        )

        HintScreenKey.CHEF_STATS -> ScreenHintContent(
            title = "История сканов",
            steps = listOf(
                "Проверьте отказы и причину в карточке операции.",
                "После оффлайн-работы дождитесь успешной синхронизации.",
            ),
        )

        HintScreenKey.REGISTRATOR_DASHBOARD -> ScreenHintContent(
            title = "Регистратор",
            steps = listOf(
                "Создавайте пользователей и назначайте им роли/группы.",
                "После изменений проверьте карточку пользователя.",
            ),
        )

        HintScreenKey.REGISTRATOR_USERS -> ScreenHintContent(
            title = "Тест пользователей",
            steps = listOf(
                "Нажмите «+» у группы — новая форма откроется с этой группой.",
                "После смены ролей убедитесь, что группа и категория заданы.",
            ),
        )

        HintScreenKey.REGISTRATOR_USER_CREATE -> ScreenHintContent(
            title = "Создание",
            steps = listOf(
                "Заполните ФИО, роли и обязательные поля группы/категории.",
                "Сохраните логин и пароль из окна после создания.",
            ),
        )

        HintScreenKey.REGISTRATOR_GROUPS -> ScreenHintContent(
            title = "Группы",
            steps = listOf(
                "Перед удалением группы переведите всех студентов.",
                "После назначений проверьте состав и куратора группы.",
            ),
        )

        HintScreenKey.CURATOR_DASHBOARD -> ScreenHintContent(
            title = "Куратор",
            steps = listOf(
                "Сначала заполните табель, затем проверьте статистику и отчеты.",
                "Если студент без категории — сначала назначьте категорию.",
            ),
        )

        HintScreenKey.CURATOR_ROSTER -> ScreenHintContent(
            title = "Тест табеля",
            steps = listOf(
                "Выберите группу, дату и отметки; для отсутствия задайте причину.",
                "Сохраните табель и проверьте копирование на другую дату.",
            ),
        )

        HintScreenKey.CURATOR_STATS -> ScreenHintContent(
            title = "Тест статистики",
            steps = listOf(
                "Выберите дату и группу, затем проверьте статусы по студентам.",
                "Если пусто — проверьте, что табель на дату сохранен.",
            ),
        )

        HintScreenKey.CURATOR_CATEGORIES -> ScreenHintContent(
            title = "Категории",
            steps = listOf(
                "Назначьте категорию студентам без питания в табеле.",
                "После сохранения вернитесь и повторите сохранение табеля.",
            ),
        )

        HintScreenKey.CURATOR_REPORTS -> ScreenHintContent(
            title = "Тест отчетов",
            steps = listOf(
                "Выберите период и группу, затем сформируйте отчет.",
                "Проверьте причины «не питается» перед экспортом.",
            ),
        )

        HintScreenKey.ADMIN_DASHBOARD -> ScreenHintContent(
            title = "Администратор",
            steps = listOf(
                "Проверяйте отчеты питания и вкладку подозрений.",
                "Перед экспортом уточняйте период и группу.",
            ),
        )

        HintScreenKey.ADMIN_REPORTS -> ScreenHintContent(
            title = "Отчеты",
            steps = listOf(
                "Выберите «с даты/по дату» и при необходимости группу.",
                "Сформируйте и экспортируйте отчет после проверки строк.",
            ),
        )
    }
}
