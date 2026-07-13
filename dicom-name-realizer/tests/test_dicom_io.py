import sys
import tempfile
import types
import unittest
from pathlib import Path

from dicom_name_realizer.dicom_io import process_dicom_paths
from dicom_name_realizer.names import NameFactory


class FakeDataset:
    def __init__(self, patient_id="TEST0001"):
        self.PatientID = patient_id
        self.PatientName = "ANONYMOUS"
        self.StudyInstanceUID = "1.2.3"

    def save_as(self, path):
        Path(path).write_text(str(self.PatientName), encoding="utf-8")


class DicomProcessingTests(unittest.TestCase):
    def setUp(self):
        self.original_pydicom = sys.modules.get("pydicom")
        self.calls = []

        class InvalidDicomError(Exception):
            pass

        def dcmread(path):
            self.calls.append({"path": path})
            if Path(path).suffix == ".txt":
                raise InvalidDicomError
            return FakeDataset()

        sys.modules["pydicom"] = types.SimpleNamespace(
            dcmread=dcmread,
            errors=types.SimpleNamespace(InvalidDicomError=InvalidDicomError),
        )

    def tearDown(self):
        if self.original_pydicom is None:
            sys.modules.pop("pydicom", None)
        else:
            sys.modules["pydicom"] = self.original_pydicom

    def test_process_writes_synthetic_patient_name(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source_dir = root / "input"
            output_dir = root / "output"
            source_file = source_dir / "nested" / "image.dcm"
            source_file.parent.mkdir(parents=True)
            source_file.write_bytes(b"fake")

            summary = process_dicom_paths(
                source_dir,
                output_dir,
                name_factory=NameFactory(seed="demo"),
            )

            output_file = output_dir / "nested" / "image.dcm"
            self.assertEqual(summary.processed_count, 1)
            self.assertTrue(output_file.exists())
            self.assertRegex(output_file.read_text(encoding="utf-8"), r"^[A-Z]+\^[A-Z]+$")
            self.assertNotEqual(output_file.read_text(encoding="utf-8"), "ANONYMOUS")
            self.assertEqual(source_file.read_bytes(), b"fake")

    def test_non_dicom_files_are_skipped(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source_dir = root / "input"
            output_dir = root / "output"
            source_dir.mkdir()
            (source_dir / "notes.txt").write_text("not dicom", encoding="utf-8")

            summary = process_dicom_paths(
                source_dir,
                output_dir,
                name_factory=NameFactory(seed="demo"),
            )

            self.assertEqual(summary.processed_count, 0)
            self.assertEqual(summary.skipped_count, 1)
            self.assertFalse((output_dir / "notes.txt").exists())


if __name__ == "__main__":
    unittest.main()
