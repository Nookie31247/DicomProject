from __future__ import annotations

import argparse
import sys
from pathlib import Path

from .dicom_io import (
    DicomNameRealizerError,
    MissingDependencyError,
    process_dicom_paths,
    write_mapping_csv,
)
from .names import NameFactory


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="dicom-name-realizer",
        description="Replace anonymized DICOM PatientName values with realistic synthetic names.",
    )
    parser.add_argument("input", type=Path, help="Input DICOM file or directory.")
    parser.add_argument("output", type=Path, help="Output DICOM file or directory.")
    parser.add_argument(
        "--locale",
        default="ko",
        help="Synthetic name pool: ko, en, or mixed. Default: ko.",
    )
    parser.add_argument(
        "--seed",
        default="",
        help="Seed for deterministic name generation. Same seed and patient key produce the same name.",
    )
    parser.add_argument(
        "--extension",
        default=".dcm",
        help="File extension to process when input is a directory. Default: .dcm.",
    )
    parser.add_argument(
        "--include-all",
        action="store_true",
        help="Try every file in the input directory instead of filtering by extension.",
    )
    parser.add_argument(
        "--no-recursive",
        action="store_true",
        help="Only process files directly under the input directory.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Pass force=True to pydicom for files without a standard DICOM preamble.",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Replace files that already exist in the output path.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show what would be processed without writing DICOM files.",
    )
    parser.add_argument(
        "--mapping-csv",
        type=Path,
        help="Optional CSV path for the original-to-synthetic name mapping.",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    try:
        summary = process_dicom_paths(
            args.input,
            args.output,
            name_factory=NameFactory(locale=args.locale, seed=args.seed),
            recursive=not args.no_recursive,
            include_all=args.include_all,
            extension=args.extension,
            force=args.force,
            overwrite=args.overwrite,
            dry_run=args.dry_run,
        )
        if args.mapping_csv:
            write_mapping_csv(args.mapping_csv, summary.updates)
    except MissingDependencyError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2
    except (DicomNameRealizerError, ValueError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    print(summary.to_message())
    if args.mapping_csv:
        print(f"Mapping CSV: {args.mapping_csv}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
