import tempfile
import unittest
from pathlib import Path

from openpyxl import Workbook

from tools.xlsx_import.import_xlsx import (
    map_category,
    parse_workbook,
    split_camel_token,
    split_fio,
)


class ParserTests(unittest.TestCase):
    def test_map_category(self) -> None:
        self.assertEqual(map_category("СВО"), "SVO")
        self.assertEqual(map_category("Многодетные"), "MANY_CHILDREN")
        self.assertIsNone(map_category("Неизвестно"))

    def test_split_camel_token(self) -> None:
        self.assertEqual(
            split_camel_token("НоманжановФаррухТолкунжанович"),
            ["Номанжанов", "Фаррух", "Толкунжанович"],
        )

    def test_split_fio_with_camel_fix(self) -> None:
        parts, mode = split_fio("АгаевЭльнурРовшанОглы", allow_camel_fix=True)
        self.assertIsNone(parts)
        self.assertEqual(mode, "invalid_parts_4")

        parts_ok, mode_ok = split_fio("ИскалиевРучланЕрмекович", allow_camel_fix=True)
        self.assertEqual(parts_ok, ("Искалиев", "Ручлан", "Ермекович"))
        self.assertEqual(mode_ok, "camel_case_fixed")

    def test_parse_workbook_dedup_and_rejects(self) -> None:
        wb = Workbook()
        ws = wb.active
        ws.title = "ИТ"
        ws.append(
            [
                "№ п/п",
                "ФИО обучающегося",
                "статус",
                "Группа",
                "Дата рождения",
                "Категория",
                "Завтрак",
                "Обед",
                "Комментарии",
                "Классный руководитель",
            ]
        )

        ws.append([1, "Иванов Иван Иванович", "студент", "ИСП-11", "", "СВО", "1", "", "", "Петров Петр Петрович"])
        ws.append([2, "ИскалиевРучланЕрмекович", "студент", "ИСП-11", "", "Многодетные", "1", "", "", "Петров Петр Петрович"])
        ws.append([3, "Иванов Иван Иванович", "студент", "ИСП-11", "", "СВО", "1", "", "", "Петров Петр Петрович"])
        ws.append([4, "Сидоров Сидор Сидорович", "студент", "ИСП-12", "", "UNKNOWN", "", "1", "", "Петров Петр Петрович"])
        ws.append([5, "Козлов Козьма Козьмич", "абитуриент", "ИСП-13", "", "СВО", "", "1", "", "Петров Петр Петрович"])
        ws.append([6, "Тестов Тест Тестович", "студент", "ИСП-12", "", "СВО", "", "", "", ""])

        with tempfile.TemporaryDirectory() as tmp_dir:
            xlsx_path = Path(tmp_dir) / "test.xlsx"
            wb.save(xlsx_path)

            parsed = parse_workbook(xlsx_path)

        self.assertEqual(parsed.processed_student_rows, 5)
        self.assertEqual(parsed.skipped_non_student_rows, 1)
        self.assertEqual(len(parsed.valid_students), 2)
        self.assertEqual(len(parsed.rejects), 3)
        self.assertEqual(len(parsed.groups), 1)
        self.assertEqual(len(parsed.curators), 1)

        reject_codes = {r.error_code for r in parsed.rejects}
        self.assertIn("duplicate_student", reject_codes)
        self.assertTrue(any("bad_category" in code for code in reject_codes))
        self.assertTrue(any("bad_curator_fio" in code for code in reject_codes))


if __name__ == "__main__":
    unittest.main()
