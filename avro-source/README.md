# Source of the Avro-derived parts of NextVoice

NextVoice's Bengali phonetic keyboard is built on the work of OmicronLab's Avro Keyboard.
The files here are the parts of the app that are covered by Mozilla Public License terms,
published so that anyone who has the app can get their source, as those licences ask.

| File | What it is | Origin | Licence |
| --- | --- | --- | --- |
| `AvroPhonetic.kt` | Kotlin port of the Avro Phonetic algorithm (`PhoneticParser.java`) | [omicronlab/JAvroPhonetic](https://github.com/omicronlab/JAvroPhonetic), branch `xml` | MPL 1.1 (`MPL-1.1.txt`) |
| `avro_rules.json` | The Avro Phonetic rules (`phonetic.xml`) converted to JSON, unchanged in content | same | MPL 1.1 |
| `bn_words.txt` | Avro's Bengali dictionary, one word per line; only 25 damaged entries were removed | `avrodict.js` in [sarim/ibus-avro](https://github.com/sarim/ibus-avro) | MPL 2.0 (`MPL-2.0.txt`) |
| `build_avro_rules.py`, `build_avro_dictionary.py` | The scripts that produced the two data files | written for NextVoice | MPL 1.1 / 2.0 as above |

The rest of NextVoice is not part of this package. The Avro port passes all 414 test cases
of the original JAvroPhonetic library.
