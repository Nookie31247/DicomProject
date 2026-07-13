from __future__ import annotations

import hashlib
from dataclasses import dataclass


KO_FAMILY_NAMES = (
    "KIM",
    "LEE",
    "PARK",
    "CHOI",
    "JUNG",
    "KANG",
    "CHO",
    "YOON",
    "JANG",
    "LIM",
    "HAN",
    "OH",
    "SEO",
    "SHIN",
    "KWON",
    "HWANG",
    "AHN",
    "SONG",
    "RYU",
    "HONG",
    "JEON",
    "MOON",
    "BAE",
    "NAM",
)

KO_GIVEN_NAMES = (
    "MINJUN",
    "SEOJUN",
    "DOYUN",
    "YEJUN",
    "SIWOO",
    "JUWON",
    "JIHO",
    "JUNSEO",
    "HYUNWOO",
    "GEONWOO",
    "SEOYEON",
    "HAYOON",
    "JIYOO",
    "JISU",
    "EUNSEO",
    "YUNA",
    "SOYUL",
    "HARIN",
    "JIWOO",
    "SUA",
    "DAEUN",
    "YUJIN",
    "MINSEO",
    "YEWON",
)

EN_FAMILY_NAMES = (
    "SMITH",
    "JOHNSON",
    "WILLIAMS",
    "BROWN",
    "JONES",
    "GARCIA",
    "MILLER",
    "DAVIS",
    "MARTINEZ",
    "RODRIGUEZ",
    "ANDERSON",
    "TAYLOR",
    "THOMAS",
    "MOORE",
    "MARTIN",
    "LEE",
    "PEREZ",
    "THOMPSON",
    "WHITE",
    "HARRIS",
    "CLARK",
    "LEWIS",
    "YOUNG",
    "WALKER",
)

EN_GIVEN_NAMES = (
    "JAMES",
    "MARY",
    "ROBERT",
    "PATRICIA",
    "JOHN",
    "JENNIFER",
    "MICHAEL",
    "LINDA",
    "DAVID",
    "ELIZABETH",
    "WILLIAM",
    "BARBARA",
    "RICHARD",
    "SUSAN",
    "JOSEPH",
    "JESSICA",
    "THOMAS",
    "SARAH",
    "CHARLES",
    "KAREN",
    "DANIEL",
    "NANCY",
    "MATTHEW",
    "LISA",
)


LOCALE_ALIASES = {
    "ko": "ko",
    "ko-kr": "ko",
    "korean": "ko",
    "en": "en",
    "en-us": "en",
    "english": "en",
    "mixed": "mixed",
}


def normalize_locale(locale: str) -> str:
    normalized = LOCALE_ALIASES.get(locale.strip().lower())
    if normalized is None:
        supported = ", ".join(sorted(set(LOCALE_ALIASES.values())))
        raise ValueError(f"unsupported locale '{locale}'. Use one of: {supported}")
    return normalized


@dataclass(frozen=True)
class NameFactory:
    locale: str = "ko"
    seed: str = ""

    def __post_init__(self) -> None:
        object.__setattr__(self, "locale", normalize_locale(self.locale))

    def name_for_key(self, key: str) -> str:
        digest = hashlib.sha256(f"{self.seed}\0{key}".encode("utf-8")).digest()
        locale = self._locale_for_digest(digest)
        family_names, given_names = self._name_pool(locale)

        family = family_names[_pick_index(digest, 1, len(family_names))]
        given = given_names[_pick_index(digest, 5, len(given_names))]
        return f"{family}^{given}"

    def _locale_for_digest(self, digest: bytes) -> str:
        if self.locale != "mixed":
            return self.locale
        return "ko" if digest[0] % 2 == 0 else "en"

    @staticmethod
    def _name_pool(locale: str) -> tuple[tuple[str, ...], tuple[str, ...]]:
        if locale == "ko":
            return KO_FAMILY_NAMES, KO_GIVEN_NAMES
        if locale == "en":
            return EN_FAMILY_NAMES, EN_GIVEN_NAMES
        raise ValueError(f"unsupported normalized locale '{locale}'")


def _pick_index(digest: bytes, offset: int, modulo: int) -> int:
    value = int.from_bytes(digest[offset : offset + 4], "big")
    return value % modulo
