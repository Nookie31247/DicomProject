from __future__ import annotations

import random
import secrets
from dataclasses import dataclass
from dataclasses import field


# Each entry is a complete DICOM Person Name value in FAMILY^GIVEN form.
KO_NAMES = (
    "KIM^MINJUN", "LEE^SEOJUN", "PARK^DOYUN", "CHOI^YEJUN",
    "JUNG^SIWOO", "KANG^JUWON", "CHO^JIHO", "YOON^JUNSEO",
    "JANG^HYUNWOO", "LIM^GEONWOO", "HAN^SEOYEON", "OH^HAYOON",
    "SEO^JIYOO", "SHIN^JISU", "KWON^EUNSEO", "HWANG^YUNA",
    "AHN^SOYUL", "SONG^HARIN", "RYU^JIWOO", "HONG^SUA",
    "JEON^DAEUN", "MOON^YUJIN", "BAE^MINSEO", "NAM^YEWON",
    "KIM^SEONGMIN", "LEE^DONGHYUN", "PARK^SEUNGHYUN", "CHOI^TAEYANG",
    "JUNG^WOOJIN", "KANG^JUNYOUNG", "CHO^MINJI", "YOON^SEULGI",
    "JANG^SOJUNG", "LIM^NAEUN", "HAN^CHAEWON", "OH^SOMIN",
    "SEO^JIHYE", "SHIN^YESEUL", "KWON^HYEJIN", "HWANG^EUNJI",
)

EN_NAMES = (
    "SMITH^JAMES", "JOHNSON^MARY", "WILLIAMS^ROBERT", "BROWN^PATRICIA",
    "JONES^JOHN", "GARCIA^JENNIFER", "MILLER^MICHAEL", "DAVIS^LINDA",
    "MARTINEZ^DAVID", "RODRIGUEZ^ELIZABETH", "ANDERSON^WILLIAM", "TAYLOR^BARBARA",
    "THOMAS^RICHARD", "MOORE^SUSAN", "MARTIN^JOSEPH", "LEE^JESSICA",
    "PEREZ^THOMAS", "THOMPSON^SARAH", "WHITE^CHARLES", "HARRIS^KAREN",
    "CLARK^DANIEL", "LEWIS^NANCY", "YOUNG^MATTHEW", "WALKER^LISA",
    "HALL^CHRISTOPHER", "ALLEN^AMANDA", "KING^JOSHUA", "WRIGHT^MELISSA",
    "SCOTT^ANDREW", "GREEN^STEPHANIE", "BAKER^RYAN", "ADAMS^REBECCA",
    "NELSON^BRANDON", "CARTER^LAURA", "MITCHELL^KEVIN", "PEREZ^EMILY",
    "ROBERTS^JACOB", "TURNER^OLIVIA", "PHILLIPS^ETHAN", "CAMPBELL^SOPHIA",
)

JA_NAMES = (
    "SATO^HARUTO", "SUZUKI^YUTO", "TAKAHASHI^SOTA", "TANAKA^REN",
    "WATANABE^YUKI", "ITO^KENTA", "YAMAMOTO^RYOTA", "NAKAMURA^TAKUMI",
    "KOBAYASHI^DAIKI", "KATO^SHOTA", "YOSHIDA^SAKURA", "YAMADA^YUI",
    "SASAKI^HINA", "YAMAGUCHI^RIN", "MATSUDA^AKARI", "INOUYE^MIO",
    "SHIMIZU^NANAMI", "HAYASHI^KOHARU", "ABE^MOMOKA", "IKEDA^KANON",
)

ALL_NAMES = KO_NAMES + EN_NAMES + JA_NAMES


@dataclass
class NameFactory:
    """Randomly assign synthetic names while keeping one name per patient key."""

    seed: str | None = None
    _assigned_names: dict[str, str] = field(default_factory=dict, init=False)
    _available_names: list[str] = field(default_factory=list, init=False)
    _random: random.Random = field(init=False, repr=False)

    def __post_init__(self) -> None:
        self._random = random.Random(self.seed) if self.seed is not None else secrets.SystemRandom()
        self._refill_names()

    def name_for_key(self, key: str) -> str:
        if key not in self._assigned_names:
            if not self._available_names:
                self._refill_names()
            self._assigned_names[key] = self._available_names.pop()
        return self._assigned_names[key]

    def _refill_names(self) -> None:
        self._available_names = list(ALL_NAMES)
        self._random.shuffle(self._available_names)
