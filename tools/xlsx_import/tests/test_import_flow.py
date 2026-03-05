import json
import unittest
from unittest.mock import Mock, patch

import requests

from tools.xlsx_import.import_xlsx import (
    ApiClient,
    CuratorKey,
    ParsedStudent,
    ParsedWorkbook,
    check_clean_load,
    run_import,
)


class FakeCleanClient:
    def __init__(self, groups, curators, students, me):
        self._groups = groups
        self._curators = curators
        self._students = students
        self._me = me

    def list_groups(self):
        return list(self._groups)

    def list_users(self, role: str):
        if role == "CURATOR":
            return list(self._curators)
        if role == "STUDENT":
            return list(self._students)
        return []

    def get_me(self):
        return dict(self._me)


class FakeApiClient:
    def __init__(self, fail_on_student_index: int | None = None):
        self.fail_on_student_index = fail_on_student_index
        self._next_group_id = 100
        self._next_user_id = 1000
        self.created_groups: list[int] = []
        self.created_users: list[str] = []
        self.student_create_count = 0
        self.deleted_users: list[str] = []
        self.deleted_groups: list[int] = []

    def create_group(self, name: str):
        self._next_group_id += 1
        group_id = self._next_group_id
        self.created_groups.append(group_id)
        return {"id": group_id, "name": name}

    def create_user(self, *, role: str, name: str, surname: str, father_name: str, group_id=None, student_category=None):
        if role == "STUDENT":
            self.student_create_count += 1
            if self.fail_on_student_index == self.student_create_count:
                raise RuntimeError("student create failed")

        self._next_user_id += 1
        user_id = f"u-{self._next_user_id}"
        self.created_users.append(user_id)
        return {
            "userId": user_id,
            "login": f"login-{user_id}",
            "passwordClearText": f"pwd-{user_id}",
        }

    def add_curator_to_group(self, group_id: int, curator_id: str):
        return None

    def delete_user(self, user_id: str):
        self.deleted_users.append(user_id)

    def delete_group(self, group_id: int):
        self.deleted_groups.append(group_id)


def make_parsed_workbook() -> ParsedWorkbook:
    curator = CuratorKey(surname="Петров", name="Петр", father_name="Петрович")
    students = [
        ParsedStudent(
            sheet="ИТ",
            row=2,
            surname="Иванов",
            name="Иван",
            father_name="Иванович",
            full_name="Иванов Иван Иванович",
            group_name="ИСП-11",
            category="SVO",
            curator=curator,
            raw_category="СВО",
            raw_group="ИСП-11",
            raw_curator="Петров Петр Петрович",
            raw_comment="",
            raw_breakfast="1",
            raw_lunch="",
        ),
        ParsedStudent(
            sheet="ИТ",
            row=3,
            surname="Сидоров",
            name="Сидор",
            father_name="Сидорович",
            full_name="Сидоров Сидор Сидорович",
            group_name="ИСП-11",
            category="MANY_CHILDREN",
            curator=curator,
            raw_category="Многодетные",
            raw_group="ИСП-11",
            raw_curator="Петров Петр Петрович",
            raw_comment="",
            raw_breakfast="1",
            raw_lunch="",
        ),
    ]

    return ParsedWorkbook(
        source_path="/tmp/test.xlsx",
        processed_student_rows=2,
        skipped_non_student_rows=0,
        valid_students=students,
        rejects=[],
        sheets_seen=["ИТ"],
    )


class ImportFlowTests(unittest.TestCase):
    def test_clean_load_precheck_fail(self) -> None:
        baseline = check_clean_load(
            FakeCleanClient(
                groups=[{"id": 101}, {"id": 999}],
                curators=[{"userId": "u-admin"}],
                students=[{"userId": "u-admin"}, {"userId": "u-stud"}],
                me={"userId": "u-admin", "groupId": 101},
            )
        )
        self.assertFalse(baseline.ok)
        self.assertEqual(baseline.effective_counts["existing_groups"], 1)
        self.assertEqual(baseline.effective_counts["existing_students"], 1)

    def test_clean_load_precheck_pass_for_single_admin_baseline(self) -> None:
        baseline = check_clean_load(
            FakeCleanClient(
                groups=[{"id": 101}],
                curators=[{"userId": "u-admin"}],
                students=[{"userId": "u-admin"}],
                me={"userId": "u-admin", "groupId": 101},
            )
        )
        self.assertTrue(baseline.ok)
        self.assertEqual(baseline.raw_counts["existing_groups"], 1)
        self.assertEqual(baseline.effective_counts["existing_groups"], 0)

    def test_happy_path_apply(self) -> None:
        parsed = make_parsed_workbook()
        api = FakeApiClient()

        result = run_import(
            parsed=parsed,
            client=api,
            apply_changes=True,
            rollback_enabled=True,
            preflight_ok=True,
        )

        self.assertTrue(result.success)
        self.assertTrue(result.applied)
        self.assertEqual(len(result.state.created_group_ids), 1)
        self.assertEqual(len(result.state.created_curator_ids), 1)
        self.assertEqual(len(result.state.created_student_ids), 2)
        self.assertFalse(result.rollback_performed)

        curator_statuses = {row.status for row in result.curator_rows.values()}
        self.assertEqual(curator_statuses, {"CREATED"})
        self.assertTrue(all(row.status == "CREATED" for row in result.student_rows))

    def test_rollback_on_mid_failure(self) -> None:
        parsed = make_parsed_workbook()
        api = FakeApiClient(fail_on_student_index=2)

        result = run_import(
            parsed=parsed,
            client=api,
            apply_changes=True,
            rollback_enabled=True,
            preflight_ok=True,
        )

        self.assertFalse(result.success)
        self.assertTrue(result.rollback_performed)
        self.assertTrue(result.rollback_success)

        self.assertEqual(len(result.state.created_group_ids), 1)
        self.assertEqual(len(result.state.created_curator_ids), 1)
        self.assertEqual(len(result.state.created_student_ids), 1)

        self.assertEqual(len(result.state.deleted_group_ids), 1)
        self.assertEqual(len(result.state.deleted_curator_ids), 1)
        self.assertEqual(len(result.state.deleted_student_ids), 1)

        curator_statuses = {row.status for row in result.curator_rows.values()}
        self.assertEqual(curator_statuses, {"ROLLED_BACK"})
        self.assertEqual([row.status for row in result.student_rows], ["ROLLED_BACK", "NOT_CREATED"])

    def test_api_client_retries_on_5xx_and_timeout(self) -> None:
        client = ApiClient("http://localhost:8080", timeout_seconds=0.1, max_attempts=3)

        response_500 = requests.Response()
        response_500.status_code = 500
        response_500._content = b'{"message":"boom"}'
        response_500.url = "http://localhost:8080/api/v1/groups"

        response_200 = requests.Response()
        response_200.status_code = 200
        response_200._content = json.dumps([{"id": 1, "name": "ИСП-11"}]).encode("utf-8")
        response_200.url = "http://localhost:8080/api/v1/groups"

        with patch("tools.xlsx_import.import_xlsx.time.sleep", return_value=None):
            client.session.request = Mock(side_effect=[response_500, response_200])
            payload = client._request_json("GET", "/api/v1/groups", expected_statuses=(200,))

        self.assertEqual(payload, [{"id": 1, "name": "ИСП-11"}])
        self.assertEqual(client.session.request.call_count, 2)

        with patch("tools.xlsx_import.import_xlsx.time.sleep", return_value=None):
            client.session.request = Mock(side_effect=[requests.Timeout("t"), response_200])
            payload = client._request_json("GET", "/api/v1/groups", expected_statuses=(200,))

        self.assertEqual(payload, [{"id": 1, "name": "ИСП-11"}])
        self.assertEqual(client.session.request.call_count, 2)


if __name__ == "__main__":
    unittest.main()
