// DICOM 파일의

export const dicomTagDictionary: Record<string, string> = {
  // Patient Information (0010)
  "(0010,0010)": "Patient's Name",
  "(0010,0020)": "Patient ID",
  "(0010,0030)": "Patient's Birth Date",
  "(0010,0040)": "Patient's Sex",
  "(0010,1010)": "Patient's Age",
  "(0010,1020)": "Patient's Size",
  "(0010,1030)": "Patient's Weight",
  
  // Study & Series Information (0008)
  "(0008,0008)": "Image Type",
  "(0008,0020)": "Study Date",
  "(0008,0021)": "Series Date",
  "(0008,0030)": "Study Time",
  "(0008,0031)": "Series Time",
  "(0008,0050)": "Accession Number",
  "(0008,0060)": "Modality",
  "(0008,0070)": "Manufacturer",
  "(0008,0080)": "Institution Name",
  "(0008,0090)": "Referring Physician's Name",
  "(0008,1010)": "Station Name",
  "(0008,1030)": "Study Description",
  "(0008,103E)": "Series Description",
  "(0008,1090)": "Manufacturer's Model Name",

  // Image & Pixel Information (0028)
  "(0028,0002)": "Samples per Pixel",
  "(0028,0004)": "Photometric Interpretation",
  "(0028,0010)": "Rows",
  "(0028,0011)": "Columns",
  "(0028,0030)": "Pixel Spacing",
  "(0028,0100)": "Bits Allocated",
  "(0028,0101)": "Bits Stored",
  "(0028,0102)": "High Bit",
  "(0028,0103)": "Pixel Representation",
  "(0028,1050)": "Window Center",
  "(0028,1051)": "Window Width",
  "(0028,1052)": "Rescale Intercept",
  "(0028,1053)": "Rescale Slope",

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
};
