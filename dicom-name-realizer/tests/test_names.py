import unittest

from dicom_name_realizer.names import NameFactory, normalize_locale


class NameFactoryTests(unittest.TestCase):
    def test_same_key_and_seed_are_deterministic(self):
        factory = NameFactory(locale="ko", seed="demo")

        self.assertEqual(
            factory.name_for_key("PatientID:TEST0001"),
            factory.name_for_key("PatientID:TEST0001"),
        )

    def test_different_seeds_can_change_name(self):
        key = "PatientID:TEST0001"

        self.assertNotEqual(
            NameFactory(locale="en", seed="a").name_for_key(key),
            NameFactory(locale="en", seed="b").name_for_key(key),
        )

    def test_patient_name_uses_dicom_person_name_components(self):
        name = NameFactory(locale="ko").name_for_key("PatientName:ANON")

        self.assertRegex(name, r"^[A-Z]+\^[A-Z]+$")

    def test_locale_aliases_are_supported(self):
        self.assertEqual(normalize_locale("ko-KR"), "ko")
        self.assertEqual(normalize_locale("English"), "en")
        self.assertEqual(normalize_locale("mixed"), "mixed")

    def test_invalid_locale_is_rejected(self):
        with self.assertRaises(ValueError):
            normalize_locale("fr")


if __name__ == "__main__":
    unittest.main()
