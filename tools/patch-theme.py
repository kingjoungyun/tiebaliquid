"""Patch the app's DataStore preferences (.preferences_pb) for theme / accent.

Usage: python patch-theme.py <prefs.pb> <light|dark|amoled> [accentHex]

DataStore Preferences wire format (androidx.datastore.preferences):
  PreferenceMap { repeated MapEntry preferences = 1; }
  MapEntry { string key = 1; Value value = 2; }
  Value { oneof { bool boolean = 1; float float = 2; int32 integer = 3;
                  int64 long = 4; string string = 5; StringSet string_set = 6; ... } }

A string Value is therefore `0x2a <len> <utf8>` (field 5, wire type 2).
Entry lengths are varints, not fixed single bytes (the old script assumed 1 byte).
"""
import sys

TARGETS = {
    "light": "tieba",
    "dark": "amoled_dark",
    "amoled": "amoled_dark",
}


def read_varint(data, offset):
    result = 0
    shift = 0
    while True:
        if offset >= len(data):
            raise ValueError("truncated varint")
        byte = data[offset]
        offset += 1
        result |= (byte & 0x7F) << shift
        if not (byte & 0x80):
            return result, offset
        shift += 7
        if shift > 63:
            raise ValueError("varint too long")


def write_varint(value):
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return bytes(out)


def read_entry(entry):
    """Split one MapEntry into (key, raw Value bytes)."""
    offset = 0
    key = None
    value = None
    while offset < len(entry):
        tag = entry[offset]
        offset += 1
        field, wire = tag >> 3, tag & 0x07
        if wire != 2:
            raise ValueError("unexpected wire type in MapEntry: %d" % wire)
        length, offset = read_varint(entry, offset)
        payload = entry[offset:offset + length]
        offset += length
        if field == 1:
            key = payload.decode("utf-8")
        elif field == 2:
            value = payload
    if key is None or value is None:
        raise ValueError("incomplete MapEntry")
    return key, value


def write_entry(key, value):
    key_bytes = key.encode("utf-8")
    body = b"\x0a" + write_varint(len(key_bytes)) + key_bytes
    body += b"\x12" + write_varint(len(value)) + value
    return b"\x0a" + write_varint(len(body)) + body


def string_value(text):
    data = text.encode("utf-8")
    return b"\x2a" + write_varint(len(data)) + data


def main():
    if len(sys.argv) < 3:
        print(__doc__, file=sys.stderr)
        return 2
    path, mode = sys.argv[1], sys.argv[2]
    accent = sys.argv[3] if len(sys.argv) > 3 and sys.argv[3] else None
    if mode not in TARGETS:
        print("unknown mode:", mode, file=sys.stderr)
        return 2

    data = open(path, "rb").read()
    entries = []
    offset = 0
    while offset < len(data):
        tag = data[offset]
        offset += 1
        if tag != 0x0A:
            raise ValueError("unexpected top-level tag 0x%02x at %d" % (tag, offset - 1))
        length, offset = read_varint(data, offset)
        entry = data[offset:offset + length]
        offset += length
        entries.append(read_entry(entry))

    drop = {"theme"}
    if accent:
        drop.add("custom_primary_color")
    kept = [(k, v) for (k, v) in entries if k not in drop]
    kept.append(("theme", string_value(TARGETS[mode])))
    if accent:
        kept.append(("custom_primary_color", string_value(accent)))

    out = b"".join(write_entry(k, v) for (k, v) in kept)
    open(path, "wb").write(out)
    print("entries:", len(entries), "->", len(kept), "| theme:", TARGETS[mode],
          "| accent:", accent or "(unchanged)", "| bytes:", len(data), "->", len(out))
    return 0


if __name__ == "__main__":
    sys.exit(main())
