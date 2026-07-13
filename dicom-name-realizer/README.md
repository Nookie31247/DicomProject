# DICOM Name Realizer

Small Python CLI for turning anonymized DICOM `PatientName` values into realistic synthetic names for demos, training data, and local testing.

The tool does not try to recover real patient identities. It creates deterministic synthetic names and writes changed DICOM files to a separate output path by default.

## Features

- Updates DICOM tag `(0010,0010) PatientName`.
- Keeps original files untouched unless you explicitly choose an output path over them.
- Preserves directory structure when processing a folder.
- Uses deterministic synthetic names so the same anonymized patient key maps to the same new name.
- Supports `ko`, `en`, and `mixed` synthetic name pools.
- Optional dry run and CSV mapping output.

## Install

```powershell
cd dicom-name-realizer
python -m venv .venv
.\.venv\Scripts\python -m pip install -e .
```

## Usage

Process a directory of `.dcm` files into a new output directory:

```powershell
dicom-name-realizer ..\samples .\out --locale ko --seed demo
```

Run without writing files:

```powershell
dicom-name-realizer ..\samples .\out --dry-run
```

Write a CSV mapping:

```powershell
dicom-name-realizer ..\samples .\out --mapping-csv .\out\name-map.csv
```

Process files without a `.dcm` extension:

```powershell
dicom-name-realizer .\input .\out --include-all --force
```

## Notes

- `PatientName` is written in DICOM PN format: `FAMILY^GIVEN`.
- `PatientID`, UIDs, birth date, accession numbers, and other tags are not changed.
- The default consistency key is `PatientID`, then `PatientName`, then `StudyInstanceUID`, then the file path.
- Use this only with de-identified or synthetic DICOM files.
