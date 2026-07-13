from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

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
    dry_run: bool

    @property
    def processed_count(self) -> int:
        return len(self.updates)

    def to_message(self) -> str:
        action = "Planned" if self.dry_run else "Wrote"
        suffix = " No DICOM files matched." if not self.updates else ""
        return f"{action} {self.processed_count} DICOM file(s).{suffix}"


def process_dicom_paths(
    input_path: Path,
    output_path: Path,
    *,
    name_factory: NameFactory,
    recursive: bool = True,
    include_all: bool = False,
    extension: str = ".dcm",
    force: bool = False,
    overwrite: bool = False,
    dry_run: bool = False,
) -> ProcessSummary:
    pydicom = _load_pydicom()
    input_path = input_path.resolve()
    output_path = output_path.resolve()

    files = tuple(
        _iter_input_files(
            input_path,
            recursive=recursive,
            include_all=include_all,
            extension=extension,
        )
    )

    if input_path.is_dir() and output_path == input_path:
        raise DicomNameRealizerError("output directory must be different from input directory")

    updates: list[FileUpdate] = []
    for source_file in files:
        destination = _build_output_path(source_file, input_path, output_path)
        if not dry_run and destination.exists() and not overwrite:
            raise DicomNameRealizerError(
                f"output already exists: {destination}. Pass --overwrite to replace it."
            )

        dataset = pydicom.dcmread(str(source_file), force=force, stop_before_pixels=dry_run)
        source_key = _patient_consistency_key(dataset, source_file)
        old_name = str(getattr(dataset, "PatientName", ""))
        new_name = name_factory.name_for_key(source_key)

        if not dry_run:
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
                written=not dry_run,
            )
        )

    return ProcessSummary(updates=tuple(updates), dry_run=dry_run)


def write_mapping_csv(path: Path, updates: Iterable[FileUpdate]) -> None:
    import csv

    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as csv_file:
        writer = csv.DictWriter(
            csv_file,
            fieldnames=(
                "source_path",
                "output_path",
                "source_key",
                "old_patient_name",
                "new_patient_name",
                "written",
            ),
        )
        writer.writeheader()
        for update in updates:
            writer.writerow(
                {
                    "source_path": str(update.source_path),
                    "output_path": str(update.output_path),
                    "source_key": update.source_key,
                    "old_patient_name": update.old_patient_name,
                    "new_patient_name": update.new_patient_name,
                    "written": update.written,
                }
            )


def _load_pydicom():
    try:
        import pydicom
    except ModuleNotFoundError as exc:
        raise MissingDependencyError(
            "pydicom is required. Install with: python -m pip install -e ."
        ) from exc
    return pydicom


def _iter_input_files(
    input_path: Path,
    *,
    recursive: bool,
    include_all: bool,
    extension: str,
) -> Iterable[Path]:
    if input_path.is_file():
        yield input_path
        return

    if not input_path.is_dir():
        raise DicomNameRealizerError(f"input path does not exist: {input_path}")

    normalized_extension = extension.lower()
    candidates = input_path.rglob("*") if recursive else input_path.iterdir()
    for candidate in sorted(candidates):
        if not candidate.is_file():
            continue
        if include_all or candidate.suffix.lower() == normalized_extension:
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
