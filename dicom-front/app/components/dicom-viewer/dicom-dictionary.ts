/**
 * DICOM 파일의 메타데이터 태그값을 String 설명문으로 변경하는 코드
 * DICOM 태그를 설명적인 이름에 매핑하는 딕셔너리입니다.
 */
export const dicomTagDictionary: Record<string, string> = {
  // File Meta Information (0002)
  "(0002,0000)": "File Meta Information Group Length",
  "(0002,0001)": "File Meta Information Version",
  "(0002,0002)": "Media Storage SOP Class UID",
  "(0002,0003)": "Media Storage SOP Instance UID",
  "(0002,0010)": "Transfer Syntax UID",
  "(0002,0012)": "Implementation Class UID",
  "(0002,0013)": "Implementation Version Name",
  "(0002,0016)": "Source Application Entity Title",
  "(0002,0100)": "Private Information Creator UID",
  "(0002,0102)": "Private Information",

  // Patient Information (0010)
  "(0010,0010)": "Patient's Name",
  "(0010,0020)": "Patient ID",
  "(0010,0030)": "Patient's Birth Date",
  "(0010,0040)": "Patient's Sex",
  "(0010,1010)": "Patient's Age",
  "(0010,1020)": "Patient's Size",
  "(0010,1030)": "Patient's Weight",
  
  // Study & Series Information (0008)
  "(0008,0005)": "Specific Character Set",
  "(0008,0008)": "Image Type",
  "(0008,0016)": "SOP Class UID",
  "(0008,0018)": "SOP Instance UID",
  "(0008,0020)": "Study Date",
  "(0008,0021)": "Series Date",
  "(0008,0023)": "Content Date",
  "(0008,0030)": "Study Time",
  "(0008,0031)": "Series Time",
  "(0008,0033)": "Content Time",
  "(0008,0050)": "Accession Number",
  "(0008,0060)": "Modality",
  "(0008,0064)": "Conversion Type",
  "(0008,0070)": "Manufacturer",
  "(0008,0080)": "Institution Name",
  "(0008,0090)": "Referring Physician's Name",
  "(0008,1010)": "Station Name",
  "(0008,1030)": "Study Description",
  "(0008,103E)": "Series Description",
  "(0008,1070)": "Operators' Name",
  "(0008,1090)": "Manufacturer's Model Name",

  // Image & Pixel Information (0028)
  "(0028,0002)": "Samples per Pixel",
  "(0028,0004)": "Photometric Interpretation",
  "(0028,0008)": "Number of Frames",
  "(0028,0010)": "Rows",
  "(0028,0011)": "Columns",
  "(0028,0030)": "Pixel Spacing",
  "(0028,0100)": "Bits Allocated",
  "(0028,0101)": "Bits Stored",
  "(0028,0102)": "High Bit",
  "(0028,0103)": "Pixel Representation",
  "(0028,0106)": "Smallest Image Pixel Value",
  "(0028,0107)": "Largest Image Pixel Value",
  "(0028,1050)": "Window Center",
  "(0028,1051)": "Window Width",
  "(0028,1052)": "Rescale Intercept",
  "(0028,1053)": "Rescale Slope",
  "(0028,2110)": "Lossy Image Compression",
  "(0028,2112)": "Lossy Image Compression Ratio",
  "(0028,2114)": "Lossy Image Compression Method",

  // Acquisition Information (0018)
  "(0018,0015)": "Body Part Examined",
  "(0018,0050)": "Slice Thickness",
  "(0018,0060)": "KVP",
  "(0018,1020)": "Software Version",
  "(0018,1100)": "Reconstruction Diameter",
  "(0018,1120)": "Gantry/Detector Tilt",
  "(0018,1130)": "Table Height",
  "(0018,1150)": "Exposure Time",
  "(0018,1151)": "X-Ray Tube Current",
  "(0018,1152)": "Exposure",
  "(0018,1210)": "Convolution Kernel",
  "(0018,5100)": "Patient Position",

  // Relationship Information (0020)
  "(0020,000D)": "Study Instance UID",
  "(0020,000E)": "Series Instance UID",
  "(0020,0010)": "Study ID",
  "(0020,0011)": "Series Number",
  "(0020,0012)": "Acquisition Number",
  "(0020,0013)": "Instance Number",
  "(0020,0032)": "Image Position (Patient)",
  "(0020,0037)": "Image Orientation (Patient)",
  "(0020,1040)": "Position Reference Indicator",
  "(0020,1041)": "Slice Location",

  // Request/Procedure Information (0032)
  "(0032,1032)": "Requesting Physician",
  "(0032,1033)": "Requesting Service",

  // Pixel Data (7FE0)
  "(7FE0,0010)": "Pixel Data",
};
