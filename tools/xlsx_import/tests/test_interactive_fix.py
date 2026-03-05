import tempfile
import unittest
from pathlib import Path

from openpyxl import Workbook

from tools.xlsx_import.import_xlsx import (
    InteractivePrompter,
    RejectRow,
    interactive_fix_rejects,
    parse_workbook,
)


class ScriptedPrompter(InteractivePrompter):
    def __init__(self, actions, edits=None):
        self._actions = list(actions)
        self._edits = list(edits or [])
        self._active_edit = {}

    def choose_action(self, reject: RejectRow, current_values):
        if not self._actions:
            return "skip"
        action = self._actions.pop(0)
        if action == "edit":
            self._active_edit = self._edits.pop(0) if self._edits else {}
        return action

    def ask_text(self, label: str, current_value: str) -> str:
        mapping = {
            "ФИО студента": "student_fio",
            "Группа": "group",
            "Классный руководитель": "curator",
            "Завтрак": "breakfast",
            "Обед": "lunch",
            "Комментарий": "comment",
        }
        key = mapping.get(label)
        if key is None:
            return current_value
        return self._active_edit.get(key, current_value)

    def ask_category(self, current_value: str) -> str:
        return self._active_edit.get("category", current_value)


class InteractiveFixTests(unittest.TestCase):
    def _create_invalid_workbook(self, path: Path) -> None:
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
        ws.append([1, "Иванов Иван Иванович", "студент", "ИСП-11", "", "СВО", "1", "", "", ""])
        wb.save(path)

    def test_interactive_edit_fixes_row(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            xlsx_path = Path(tmp_dir) / "broken.xlsx"
            self._create_invalid_workbook(xlsx_path)

            parsed = parse_workbook(xlsx_path)
            self.assertEqual(len(parsed.rejects), 1)

            prompter = ScriptedPrompter(
                actions=["edit"],
                edits=[
                    {
                        "student_fio": "Иванов Иван Иванович",
                        "group": "ИСП-11",
                        "category": "СВО",
                        "curator": "Петров Петр Петрович",
                        "breakfast": "1",
                        "lunch": "",
                        "comment": "",
                    }
                ],
            )

            fix_result = interactive_fix_rejects(
                xlsx_path=xlsx_path,
                rejects=parsed.rejects,
                prompter=prompter,
            )

            self.assertFalse(fix_result.aborted)
            self.assertEqual(fix_result.fixed_rows, 1)
            self.assertEqual(fix_result.skipped_rows, 0)
            self.assertIsNotNone(fix_result.fixed_xlsx_path)

            reparsed = parse_workbook(Path(fix_result.fixed_xlsx_path))
            self.assertEqual(len(reparsed.rejects), 0)
            self.assertEqual(len(reparsed.valid_students), 1)

    def test_interactive_skip_keeps_reject(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            xlsx_path = Path(tmp_dir) / "broken.xlsx"
            self._create_invalid_workbook(xlsx_path)

            parsed = parse_workbook(xlsx_path)
            fix_result = interactive_fix_rejects(
                xlsx_path=xlsx_path,
                rejects=parsed.rejects,
                prompter=ScriptedPrompter(actions=["skip"]),
            )

            self.assertFalse(fix_result.aborted)
            self.assertEqual(fix_result.fixed_rows, 0)
            self.assertEqual(fix_result.skipped_rows, 1)
            self.assertIsNotNone(fix_result.fixed_xlsx_path)

            reparsed = parse_workbook(Path(fix_result.fixed_xlsx_path))
            self.assertEqual(len(reparsed.rejects), 1)

    def test_interactive_abort(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            xlsx_path = Path(tmp_dir) / "broken.xlsx"
            self._create_invalid_workbook(xlsx_path)

            parsed = parse_workbook(xlsx_path)
            fix_result = interactive_fix_rejects(
                xlsx_path=xlsx_path,
                rejects=parsed.rejects,
                prompter=ScriptedPrompter(actions=["abort"]),
            )

            self.assertTrue(fix_result.aborted)
            self.assertEqual(fix_result.fixed_rows, 0)
            self.assertEqual(fix_result.skipped_rows, 0)
            self.assertIsNotNone(fix_result.fixed_xlsx_path)
            self.assertTrue(Path(fix_result.fixed_xlsx_path).exists())


if __name__ == "__main__":
    unittest.main()
