"""Turns Avro's Bengali word list into the plain-text asset the keyboard searches.

The words are OmicronLab's Avro dictionary as shipped in ibus-avro
(https://github.com/sarim/ibus-avro, file avrodict.js), released under the Mozilla
Public License 2.0. This script only removes broken entries and writes one word per
line; it does not add, alter or translate any word. The result is
app/src/main/assets/bn_words.txt, and the licence text is
app/src/main/assets/licenses/MPL-2.0.txt.

    python scripts/build_avro_dictionary.py path/to/avrodict.js
"""

import io
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "bn_words.txt")


def clean(word: str) -> bool:
    """Bengali letters, signs and digits only; a few entries in the original are damaged."""
    return bool(word) and all(0x0980 <= ord(c) <= 0x09FF or c in "‌‍" for c in word)


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    source = io.open(sys.argv[1], encoding="utf-8").read()
    tables = json.loads(source[source.index("{"): source.rindex("}") + 1])
    words = {w for table in tables.values() for w in table}
    kept = sorted(w for w in words if clean(w))
    io.open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(kept) + "\n")
    print(f"{len(kept)} of {len(words)} words kept ({len(words) - len(kept)} damaged entries dropped) "
          f"-> {os.path.relpath(OUT, ROOT)} ({os.path.getsize(OUT) // 1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
