import sys
import tempfile
import types
import unittest
from pathlib import Path

from dicom_name_realizer.dicom_io import process_dicom_paths
from dicom_name_realizer.names import NameFactory


class FakeDataset:
    def __init__(self):
        self.PatientID = "TEST0001"
        self.PatientName = "ANONYMOUS"
        self.StudyInstanceUID = "1.2.3"

    def save_as(self, path):
        Path(path).write_text(str(self.PatientName), encoding="utf-8")


class DicomProcessingTests(unittest.TestCase):
    def setUp(self):
        self.original_pydicom = sys.modules.get("pydicom")
        self.calls = []

        def dcmread(path, *, force=False, stop_before_pixels=False):
            self.calls.append(
                {
                    "path": path,
                    "force": force,
                    "stop_before_pixels": stop_before_pixels,
                }
            )
            return FakeDataset()

        sys.modules["pydicom"] = types.SimpleNamespace(dcmread=dcmread)

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
                name_factory=NameFactory(locale="ko", seed="demo"),
            )

            output_file = output_dir / "nested" / "image.dcm"
            self.assertEqual(summary.processed_count, 1)
            self.assertTrue(output_file.exists())
            self.assertRegex(output_file.read_text(encoding="utf-8"), r"^[A-Z]+\^[A-Z]+$")
            self.assertNotEqual(output_file.read_text(encoding="utf-8"), "ANONYMOUS")

    def test_dry_run_does_not_write_output(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source_file = root / "image.dcm"
            output_file = root / "out.dcm"
            source_file.write_bytes(b"fake")

            summary = process_dicom_paths(
                source_file,
                output_file,
                name_factory=NameFactory(locale="en"),
                dry_run=True,
            )

            self.assertEqual(summary.processed_count, 1)
            self.assertFalse(output_file.exists())
            self.assertTrue(self.calls[0]["stop_before_pixels"])


if __name__ == "__main__":
    unittest.main()
