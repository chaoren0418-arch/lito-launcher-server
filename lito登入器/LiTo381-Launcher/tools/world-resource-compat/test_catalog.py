import copy
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET
import catalog


class CatalogTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.xml = (Path(__file__).resolve().parent / "build/inputs/effectlist2.xml").read_bytes()
        cls.native, cls.rows = catalog.native_rows()
        cls.tree = ET.fromstring(cls.xml)

    def compile(self, tree):
        return catalog.compile_catalog(ET.tostring(tree), self.native, self.rows)

    def test_all_identity_bindings(self):
        blob, report = catalog.compile_catalog(self.xml, self.native, self.rows)
        self.assertEqual(len(blob), 24 + 20 * 95)
        self.assertEqual(report["mapped_slots"], 95)
        self.assertEqual(len(report["changed_icons"]), 18)
        self.assertEqual(len(report["unmapped"]), 1)

    def test_each_identity_drift_refused(self):
        for row in range(76):
            with self.subTest(row=row):
                tree = copy.deepcopy(self.tree)
                tree[row].set("startMsg", "65530")
                with self.assertRaises(ValueError):
                    self.compile(tree)
        for _, key, _, _ in catalog.EXTENDED:
            tree = copy.deepcopy(self.tree)
            tree[key].set("startMsgName", "UNRELATED_EFFECT")
            with self.assertRaises(ValueError):
                self.compile(tree)

    def test_blue_potion_cannot_alias_meditation(self):
        tree = copy.deepcopy(self.tree)
        tree[38].set("startMsgName", "MEDITATION_S")
        with self.assertRaises(ValueError):
            self.compile(tree)
        rows = list(self.rows)
        rows[38] = rows[45]
        with self.assertRaises(ValueError):
            catalog.compile_catalog(self.xml, self.native, rows)

    def test_never_remap_by_duplicate_message(self):
        # Haste has additional XML rows with identical startMsg=849.
        # Only identity 0 binds slot 0; appended "NEW_HASTE" does not replace it.
        tree = copy.deepcopy(self.tree)
        tree[345].set("icon", "123")
        _, report = self.compile(tree)
        self.assertEqual(report["bindings"][0]["effect"]["id"], "0")
        self.assertEqual(report["bindings"][0]["effect"]["icon"], "5397")

    def test_updates_and_malformed_input(self):
        tree = copy.deepcopy(self.tree)
        tree[13].set("icon", "999")
        _, report = self.compile(tree)
        self.assertIn({"slot": 13, "from": 777, "to": 999}, report["changed_icons"])
        for field, value in [("icon", "-1"), ("icon", "65536"), ("id", "1")]:
            tree = copy.deepcopy(self.tree)
            tree[0].set(field, value)
            with self.assertRaises(ValueError):
                self.compile(tree)
        with self.assertRaises(ValueError):
            catalog.parse_effects(b'<!DOCTYPE effectlist><effectlist count="0"/>')
        with self.assertRaises(ValueError):
            catalog.parse_effects(b'<effectlist count="1"/>')


if __name__ == "__main__":
    unittest.main()
