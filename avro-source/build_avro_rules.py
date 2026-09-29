"""Converts the official Avro Phonetic rules into the JSON asset the keyboard reads.

The rules (phonetic.xml) and the algorithm they drive come from OmicronLab's
JAvroPhonetic, https://github.com/omicronlab/JAvroPhonetic (branch xml), released
under the Mozilla Public License 1.1. The converted rules ship as
app/src/main/assets/avro_rules.json and the port of the algorithm is
app/src/main/kotlin/com/nextvoice/app/ime/AvroPhonetic.kt, which carries the MPL
notice. The library's own test cases are extracted to
app/src/test/resources/avro_official_cases.tsv so the port is checked against them.

    python scripts/build_avro_rules.py path/to/phonetic.xml [path/to/AvroTest.java]
"""

import io
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "avro_rules.json")
CASES = os.path.join(ROOT, "app", "src", "test", "resources", "avro_official_cases.tsv")


def text(node, tag):
    child = node.find(tag)
    return (child.text or "") if child is not None else ""


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    root = ET.parse(sys.argv[1]).getroot()
    classes = root.find("classes")
    patterns, seen = [], set()
    for p in root.find("patterns").findall("pattern"):
        find = text(p, "find")
        if find in seen:
            print("duplicate pattern:", find, file=sys.stderr)
        seen.add(find)
        rules = []
        rules_node = p.find("rules")
        for r in (rules_node if rules_node is not None else []):
            matches = []
            for m in r.find("find").findall("match"):
                scope = m.get("scope")
                negative = scope.startswith("!")
                matches.append([m.get("type"), scope.lstrip("!"), 1 if negative else 0, (m.text or "")])
            rules.append({"m": matches, "r": text(r, "replace")})
        patterns.append({"f": find, "r": text(p, "replace"), "u": rules})
    data = {
        "vowel": text(classes, "vowel"),
        "consonant": text(classes, "consonant"),
        "caseSensitive": text(classes, "casesensitive"),
        "patterns": patterns,
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    io.open(OUT, "w", encoding="utf-8", newline="\n").write(json.dumps(data, ensure_ascii=False, separators=(",", ":")))
    print(f"{len(patterns)} patterns -> {os.path.relpath(OUT, ROOT)} ({os.path.getsize(OUT) // 1024} KB)")

    if len(sys.argv) > 2:
        source = io.open(sys.argv[2], encoding="utf-8").read()
        pairs = re.findall(r'assertEquals\(\s*"([^"]*)"\s*,\s*avro\.parse\(\s*"([^"]*)"\s*\)\s*\)', source)
        os.makedirs(os.path.dirname(CASES), exist_ok=True)
        with io.open(CASES, "w", encoding="utf-8", newline="\n") as f:
            for expected, typed in pairs:
                f.write(f"{typed}\t{expected}\n")
        print(f"{len(pairs)} official test cases -> {os.path.relpath(CASES, ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
