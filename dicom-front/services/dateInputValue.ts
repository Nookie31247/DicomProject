// <input type="date">에서 쓰는 "YYYY-MM-DD" 문자열 관련 공용 유틸.
// workspace 페이지의 검사 조회 시작일/종료일, AddPatientModal의 생일 입력 등
// 여러 곳에서 같은 포맷/범위 제한 로직을 써서 여기로 뽑아뒀다.
/**
 * Date 객체를 <input type="date">용 "YYYY-MM-DD" 문자열로 포맷합니다.
 */
export const formatDateInputValue = (date: Date) => {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, "0");
    const day = `${date.getDate()}`.padStart(2, "0");

    return `${year}-${month}-${day}`;
};

/**
 * 날짜 문자열 값을 최소 및 최대 날짜 문자열 사이로 제한합니다.
 * value: <input type="date">의 value(빈 문자열 또는 "YYYY-MM-DD"), min/max도 같은 형식이다.
 *
 * 네이티브 min/max 속성은 달력 UI 탐색이나 키보드 화살표 조작에는 브라우저가 알아서
 * 범위를 지켜주지만, 연도 자리에 커서를 두고 숫자를 직접 타이핑해서 값을 완성시키면
 * 브라우저는 "폼 제출을 막는" 용도로만 min/max를 쓸 뿐 실제 value 자체는 범위 밖이어도
 * 그대로 넘겨준다. 그래서 onBlur에서 이 함수로 직접 범위를 강제한다(onChange에서 하면
 * 타이핑 도중 값이 튀어서 onBlur로 뺐다).
 *
 * 문자열을 그냥 사전식으로 비교("19999-05-12" > "2026-07-09")하면 자릿수가 다른 연도가
 * 섞였을 때 실제 크기와 안 맞을 수 있어서(첫 글자만 보면 "1"<"2"라 19999가 더 작다고
 * 나옴), Date 객체로 바꿔서 실제 시간값으로 비교한다.
 */
export const clampDateInputValue = (value: string, min: string, max: string): string => {
    if (!value) {
        return value;
    }

    const toDate = (v: string) => {
        const [y, m, d] = v.split("-").map(Number);
        return new Date(y, (m || 1) - 1, d || 1);
    };

    const valueDate = toDate(value);
    const minDate = toDate(min);
    const maxDate = toDate(max);

    if (valueDate.getTime() < minDate.getTime()) {
        return min;
    }
    if (valueDate.getTime() > maxDate.getTime()) {
        return max;
    }

    return value;
};

/**
 * 날짜 입력에 허용되는 최소 날짜 문자열을 가져옵니다.
 * 검사 목록 조회(DicomService.getStudies)의 150년 기본 범위와 맞춘 하한값.
 */
export const getMinDateInputValue = () => {
    const date = new Date();
    date.setFullYear(date.getFullYear() - 150);

    return formatDateInputValue(date);
};

/**
 * 날짜 입력에 허용되는 최대 날짜 문자열을 가져옵니다.
 */
export const getMaxDateInputValue = () => {
    return formatDateInputValue(new Date());
};
