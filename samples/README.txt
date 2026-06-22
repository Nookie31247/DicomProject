한울병원 교육용 샘플 DICOM 팩 (sample_pack_L1)
================================================
본 팩의 모든 파일은 비식별화(De-identification) 처리를 거쳤습니다.
- 환자 식별 태그 치환(ANONYMOUS^KIM/TEST0001, ANONYMOUS^LEE/TEST0002), 생년월일·의뢰의 등 제거
- private 태그 전체 제거, 모든 비표준 UID 재매핑(시퀀스 내부 포함)
- 바이트 수준 PHI 잔존 검사 통과 (장비 제조사/모델명은 표준상 유지되는 비식별 정보)
- CR 영상의 "RCC" 표기는 촬영 방향 마커(Right Cranio-Caudal)로 환자정보가 아닙니다

구성:
  CR_breast_001~002.dcm   유방촬영 2장 (1354x1010, 12bit, Explicit VR LE)
  CT_brain_001~004.dcm    두부 CT 연속 4단면 (512x512, 12bit, Explicit VR LE)
  CT_brain_implicit.dcm   Implicit VR LE 교보재 (제2강 STEP3용)

교육 목적 외 사용 금지. 재배포 시 본 안내문을 포함하세요.
