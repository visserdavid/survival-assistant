"""Adds the texts of new goal steps to all 15 languages.

Usage: python tools/add_step_texts.py <texts file>
Then:  python tools/make_lang.py src/main/resources/assets/survivalassistant/lang

The texts file is a Python file (keep it outside the repository, it is only needed once) that defines
    S = {"<goal>.<step>": {"en": (label, tip), "nl": (label, tip), "de": ..., ...}, ...}
with every language of LANGS. The label is the line on the task card (short: with its counter it must fit on the card),
the tip is what the guide says. Use the terms the existing texts of that language already use.

English and Dutch go into the table in make_lang.py, before the recipe card section; the other languages are appended
to tools/lang/<code>.json. Steps that already have texts are refused.
"""
import json
import runpy
import sys

from mcjar import ROOT

CODES = {"de": "de_de", "es": "es_es", "fr": "fr_fr", "it": "it_it", "pl": "pl_pl", "pt": "pt_br", "ro": "ro_ro",
         "ru": "ru_ru", "uk": "uk_ua", "zh": "zh_cn", "ar": "ar_sa", "hi": "hi_in", "id": "id_id"}
LANGS = set(CODES) | {"en", "nl"}
MAX_LABEL = 26
MARKER = "    # ---------------- recipe card ----------------"


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    steps = runpy.run_path(sys.argv[1])["S"]
    for sid, texts in steps.items():
        if set(texts) != LANGS:
            sys.exit(f"{sid}: languages differ: {sorted(LANGS ^ set(texts))}")
        for lang, (label, _tip) in texts.items():
            if len(label) > MAX_LABEL:
                sys.exit(f"{sid} ({lang}): label longer than {MAX_LABEL}: {label!r}")

    ml = ROOT / "tools" / "make_lang.py"
    src = ml.read_text(encoding="utf-8")
    if src.count(MARKER) != 1:
        sys.exit("recipe card marker not found in make_lang.py")
    q = lambda s: json.dumps(s, ensure_ascii=False)
    lines = []
    for sid, t in steps.items():
        if f'"step.{sid}"' in src:
            sys.exit(f"{sid} already has texts")
        lines.append(f'    P + "step.{sid}": ({q(t["en"][0])}, {q(t["nl"][0])}),')
        lines.append(f'    P + "step.{sid}.tip": ({q(t["en"][1])},')
        lines.append(f'        {q(t["nl"][1])}),')
    ml.write_text(src.replace(MARKER, "\n".join(lines) + "\n\n" + MARKER), encoding="utf-8", newline="\n")

    for lang, code in CODES.items():
        p = ROOT / "tools" / "lang" / f"{code}.json"
        body = p.read_text(encoding="utf-8").rstrip()
        if not body.endswith("}"):
            sys.exit(f"{p.name}: does not end with }}")
        body = body[:-1].rstrip()
        add = []
        for sid, t in steps.items():
            add.append(f'{q("~step." + sid)}: {q(t[lang][0])}')
            add.append(f'{q("~step." + sid + ".tip")}: {q(t[lang][1])}')
        p.write_text(body + ",\n" + ",\n".join(add) + "\n}\n", encoding="utf-8", newline="\n")
    print(f"{len(steps)} steps added in {len(LANGS)} languages; now run make_lang.py")


if __name__ == "__main__":
    main()
