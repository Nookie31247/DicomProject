//Specific Character Set -> TextDecoder 인코딩 매핑
const CHARSET_MAP: Record<string, string> = {
    "ISO_IR 6": "ascii",
    "ISO_IR 100": "iso-8859-1",
    "ISO_IR 192": "utf-8",   // 지금 파일에서 나온 값
    "ISO_IR 149": "euc-kr",  // 구형 한글 인코딩(KS X 1001)
    "GB18030": "gb18030",
};

export function getTextDecoder(ds: any): TextDecoder {
    const raw = ds.string("x00080005") || "ISO_IR 6";
    const primary = raw.split("\\")[0].trim(); // 확장 문자셋(\\로 연결)이면 첫 값만 사용
    const encoding = CHARSET_MAP[primary] || "utf-8";
    try {
        return new TextDecoder(encoding);
    } catch {
        return new TextDecoder("utf-8");
    }
}

//문자열 VR만 raw byte로 직접 디코딩
const MULTIBYTE_VRS = new Set(["PN", "LO", "SH", "ST", "LT", "UT", "CS"]);

export function readElementValue(ds: any, tag: string, decoder: TextDecoder): string {
    const element = ds.elements[tag];
    if (!element) return "";

    if (element.vr && MULTIBYTE_VRS.has(element.vr)) {
        const bytes = ds.byteArray.slice(element.dataOffset, element.dataOffset + element.length);
        return decoder.decode(bytes).replace(/\0/g, "").trim();
    }

    try {
        return ds.string(tag) ?? "";
    } catch {
        return "";
    }
}

// 바이너리/시퀀스 계열: 텍스트로 표시할 값 자체가 없음
const BINARY_VRS = new Set(["OB", "OW", "OF", "OD", "OL", "UN", "SQ"]);

// 숫자 계열: string()이 아니라 전용 getter로 읽어야 함
const NUMERIC_VRS: Record<string, (ds: any, tag: string) => number | undefined> = {
    US: (ds, tag) => ds.uint16(tag),
    SS: (ds, tag) => ds.int16(tag),
    UL: (ds, tag) => ds.uint32(tag),
    SL: (ds, tag) => ds.int32(tag),
    FL: (ds, tag) => ds.float(tag),
    FD: (ds, tag) => ds.double(tag),
};

export function getElementDisplayValue(ds: any, tag: string, decoder: TextDecoder): string | null {
    const element = ds.elements[tag];
    if (!element) return null;

    const vr = element.vr;

    // Pixel Data 등 바이너리는 아예 텍스트로 안 보여줌
    if (vr && BINARY_VRS.has(vr)) {
        return `[Binary Data, ${element.length} bytes]`;
    }

    // 숫자 VR은 string()이 아니라 숫자 getter로
    if (vr && NUMERIC_VRS[vr]) {
        const num = NUMERIC_VRS[vr](ds, tag);
        return num !== undefined ? String(num) : null;
    }

    // 나머지 문자열 계열
    return readElementValue(ds, tag, decoder);
}