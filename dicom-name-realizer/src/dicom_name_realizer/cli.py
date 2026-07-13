from __future__ import annotations

import argparse
import sys
from pathlib import Path

from .dicom_io import (
    DicomNameRealizerError,
    MissingDependencyError,
    process_dicom_paths,
)
from .names import NameFactory


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="dicom-name-realizer",
        description="Replace anonymized DICOM PatientName values with realistic synthetic names.",
    )
    parser.add_argument("input", type=Path, help="DICOM file or directory to scan recursively.")
    parser.add_argument("output", type=Path, help="Directory for the modified copies.")
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Replace files that already exist in the output path.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    try:
        summary = process_dicom_paths(
            args.input,
            args.output,
            name_factory=NameFactory(),
            overwrite=args.overwrite,
        )
    except MissingDependencyError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2
    except (DicomNameRealizerError, ValueError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    print(summary.to_message())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
