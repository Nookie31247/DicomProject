from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from .names import NameFactory


class DicomNameRealizerError(RuntimeError):
    """Base exception for expected CLI failures."""


class MissingDependencyError(DicomNameRealizerError):
    """Raised when pydicom is not installed."""


@dataclass(frozen=True)
class FileUpdate:
    source_path: Path
    output_path: Path
    source_key: str
    old_patient_name: str
    new_patient_name: str
    written: bool


@dataclass(frozen=True)
class ProcessSummary:
    updates: tuple[FileUpdate, ...]
    skipped_count: int

    @property
    def processed_count(self) -> int:
        return len(self.updates)

    def to_message(self) -> str:
        suffix = " No DICOM files matched." if not self.updates else ""
        return (
            f"Wrote {self.processed_count} DICOM file(s); "
            f"skipped {self.skipped_count} non-DICOM file(s).{suffix}"
        )


def process_dicom_paths(
    input_path: Path,
    output_path: Path,
    *,
    name_factory: NameFactory,
    overwrite: bool = False,
) -> ProcessSummary:
    pydicom = _load_pydicom()
    input_path = input_path.resolve()
    output_path = output_path.resolve()

    files = tuple(_iter_input_files(input_path))
    if input_path.is_dir() and _is_relative_to(output_path, input_path):
        raise DicomNameRealizerError(
            "output directory must be outside the input directory"
        )

    updates: list[FileUpdate] = []
    skipped_count = 0
    for source_file in files:
        destination = _build_output_path(source_file, input_path, output_path)
        try:
            dataset = pydicom.dcmread(str(source_file))
        except pydicom.errors.InvalidDicomError:
            skipped_count += 1
            continue

        if destination.exists() and not overwrite:
            raise DicomNameRealizerError(
                f"output already exists: {destination}. Pass --overwrite to replace it."
            )

        source_key = _patient_consistency_key(dataset, source_file)
        old_name = str(getattr(dataset, "PatientName", ""))
        new_name = name_factory.name_for_key(source_key)

        destination.parent.mkdir(parents=True, exist_ok=True)
        dataset.PatientName = new_name
        dataset.save_as(str(destination))

        updates.append(
            FileUpdate(
                source_path=source_file,
                output_path=destination,
                source_key=source_key,
                old_patient_name=old_name,
                new_patient_name=new_name,
                written=True,
            )
        )

    return ProcessSummary(updates=tuple(updates), skipped_count=skipped_count)


def _load_pydicom():
    try:
        import pydicom
    except ModuleNotFoundError as exc:
        raise MissingDependencyError(
            "pydicom is required. Install with: python -m pip install -e ."
        ) from exc
    return pydicom


def _iter_input_files(input_path: Path):
    if input_path.is_file():
        yield input_path
        return

    if not input_path.is_dir():
        raise DicomNameRealizerError(f"input path does not exist: {input_path}")

    for candidate in sorted(input_path.rglob("*")):
        if candidate.is_file():
            yield candidate


def _build_output_path(source_file: Path, input_path: Path, output_path: Path) -> Path:
    if input_path.is_file():
        if output_path.suffix:
            return output_path
        return output_path / source_file.name

    relative_path = source_file.relative_to(input_path)
    return output_path / relative_path


def _patient_consistency_key(dataset, source_file: Path) -> str:
    for keyword in ("PatientID", "PatientName", "StudyInstanceUID"):
        value = getattr(dataset, keyword, None)
        text = "" if value is None else str(value).strip()
        if text:
            return f"{keyword}:{text}"
    return f"Path:{source_file.as_posix()}"


def _is_relative_to(path: Path, parent: Path) -> bool:
    try:
        path.relative_to(parent)
    except ValueError:
        return False
    return True
