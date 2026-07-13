import unittest

from dicom_name_realizer.names import ALL_NAMES, EN_NAMES, JA_NAMES, KO_NAMES, NameFactory


class NameFactoryTests(unittest.TestCase):
    def test_same_key_keeps_the_same_name(self):
        factory = NameFactory(seed="demo")

        self.assertEqual(
            factory.name_for_key("PatientID:TEST0001"),
            factory.name_for_key("PatientID:TEST0001"),
        )

    def test_seed_can_make_assignments_repeatable_for_tests(self):
        first = NameFactory(seed="demo")
        second = NameFactory(seed="demo")

        self.assertEqual(
            first.name_for_key("PatientID:TEST0001"),
            second.name_for_key("PatientID:TEST0001"),
        )

    def test_patient_name_uses_dicom_person_name_components(self):
        name = NameFactory(seed="demo").name_for_key("PatientName:ANON")

        self.assertRegex(name, r"^[A-Z]+\^[A-Z]+$")

    def test_name_pools_have_the_requested_total(self):
        self.assertEqual(len(KO_NAMES), 40)
        self.assertEqual(len(EN_NAMES), 40)
        self.assertEqual(len(JA_NAMES), 20)
        self.assertEqual(len(ALL_NAMES), 100)

    def test_first_hundred_patients_receive_unique_names(self):
        factory = NameFactory(seed="demo")
        assigned = {factory.name_for_key(f"PatientID:{index}") for index in range(100)}

        self.assertEqual(assigned, set(ALL_NAMES))


if __name__ == "__main__":
    unittest.main()
