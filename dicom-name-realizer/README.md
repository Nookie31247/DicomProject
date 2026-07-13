# DICOM Name Realizer

지정한 경로와 모든 하위 경로에서 DICOM 파일을 찾아, 익명화된 `PatientName`을 실제 사람처럼 보이는 가상 이름으로 변경하는 간단한 Python 프로그램입니다.

입력 파일은 수정하지 않습니다. 이름이 변경된 복사본을 별도의 출력 경로에 저장하며, 기존 하위 폴더 구조도 그대로 유지합니다.

## 주요 기능

- DICOM 태그 `(0010,0010) PatientName`만 변경합니다.
- 모든 원본 파일을 그대로 보존합니다.
- 입력 폴더의 하위 경로를 재귀적으로 탐색합니다.
- DICOM이 아닌 파일은 자동으로 건너뜁니다.
- 100개의 가상 이름 중 하나를 자동으로 배정합니다.
- 한 번의 실행에서 같은 환자에 속하는 파일에는 같은 이름을 배정합니다.
- 한국어권 이름 40개, 영미권 이름 40개, 일본어권 이름 20개가 포함되어 있습니다.

## 사전 준비

Windows용 Python 3.12 64비트 설치를 권장합니다. 설치할 때 `Add python.exe to PATH` 항목을 선택하면 PowerShell에서 `python` 명령을 바로 사용할 수 있습니다.

설치 여부는 다음 명령으로 확인할 수 있습니다.

```powershell
python --version
```

Python 버전을 출력하는 명령입니다. `Python 3.12.x`와 비슷하게 표시되면 정상입니다.

## 프로젝트 설치

PowerShell에서 다음 명령을 순서대로 실행합니다.

```powershell
cd C:\Users\Java_Fullstack\Projects\DicomProject\dicom-name-realizer
```

현재 PowerShell 위치를 `dicom-name-realizer` 프로젝트 폴더로 이동합니다. 이후 명령은 이 폴더를 기준으로 실행됩니다.

```powershell
python -m venv .venv
```

프로젝트 폴더 안에 `.venv`라는 독립적인 Python 가상환경을 만듭니다. 이 프로젝트에서 사용하는 패키지를 다른 Python 프로젝트와 분리해 관리할 수 있습니다.

```powershell
.\.venv\Scripts\python -m pip install -e .
```

가상환경의 Python과 `pip`를 사용해 프로젝트 및 필수 패키지인 `pydicom`을 설치합니다. `-e`는 현재 소스 코드를 수정하면 재설치하지 않아도 변경 내용이 바로 반영되는 개발용 설치 방식이고, 마지막 `.`은 현재 폴더의 프로젝트를 뜻합니다.

설치는 처음 한 번만 하면 됩니다.

## 사용법

입력 폴더와 출력 폴더를 차례로 지정합니다.

```powershell
.\.venv\Scripts\dicom-name-realizer.exe "C:\dicom\input" "C:\dicom\output"
```

- `.\.venv\Scripts\dicom-name-realizer.exe`는 가상환경에 설치된 프로그램을 실행합니다.
- `"C:\dicom\input"`은 원본 DICOM 파일이 들어 있는 입력 폴더입니다. 모든 하위 폴더도 자동으로 확인합니다.
- `"C:\dicom\output"`은 이름이 변경된 복사본을 저장할 출력 폴더입니다. 폴더가 없으면 자동으로 만듭니다.
- 경로를 큰따옴표로 감싸면 경로에 공백이 있어도 정상적으로 인식됩니다.

예를 들어 입력 폴더에 다음 파일이 있다면:

```text
C:\dicom\input\study-a\image001.dcm
```

처리된 파일은 다음 위치에 생성됩니다.

```text
C:\dicom\output\study-a\image001.dcm
```

이미 출력 파일이 존재하면 원치 않는 덮어쓰기를 막기 위해 작업을 중단합니다. 기존 출력 파일을 덮어쓰려면 `--overwrite` 옵션을 추가합니다.

```powershell
.\.venv\Scripts\dicom-name-realizer.exe "C:\dicom\input" "C:\dicom\output" --overwrite
```

`--overwrite`는 출력 경로에 같은 이름의 파일이 있을 때 기존 파일을 새 결과로 교체하라는 의미입니다. 원본 입력 파일에는 영향을 주지 않습니다.

도움말을 확인하려면 다음 명령을 사용합니다.

```powershell
.\.venv\Scripts\dicom-name-realizer.exe --help
```

## 주의사항

- `PatientName`은 DICOM PN 형식인 `성^이름` 형태로 저장됩니다.
- `PatientID`, UID, 생년월일, 접수번호를 비롯한 다른 태그는 변경하지 않습니다.
- 동일 환자 판별에는 `PatientID`, 기존 `PatientName`, `StudyInstanceUID`, 파일 경로 순서로 사용할 수 있는 첫 번째 값을 사용합니다.
- 출력 폴더는 입력 폴더와 같거나 입력 폴더의 하위 경로일 수 없습니다.
- 반드시 이미 비식별화되었거나 테스트용으로 생성된 DICOM 파일에만 사용하세요.
