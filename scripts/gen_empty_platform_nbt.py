#!/usr/bin/env python3
"""
Generate the empty_platform.nbt structure file used by ArcadiaTweaks gametests.

Vanilla MC 1.21+ loads structure templates as gzipped binary NBT, NOT .snbt.
The on-disk format is documented at https://minecraft.wiki/w/Structure_file.

Output path:
  src/main/resources/data/arcadiatweaks/structure/empty_platform.nbt

Schema (root compound, unnamed):
  DataVersion: TAG_Int   (3955 = MC 1.21.1)
  size:        TAG_List<TAG_Int>(3)        [3, 3, 3]
  palette:     TAG_List<TAG_Compound>      [{Name: "minecraft:stone"}, {Name: "minecraft:air"}]
  blocks:      TAG_List<TAG_Compound>      [{pos: list<int>(3), state: int}, ...]
  entities:    TAG_List<TAG_End>(0)        []
"""

from __future__ import annotations

import gzip
import struct
import sys
from pathlib import Path

TAG_END = 0
TAG_INT = 3
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10

DATAVERSION_1_21_1 = 3955


def b_string(s: str) -> bytes:
    enc = s.encode("utf-8")
    return struct.pack(">h", len(enc)) + enc


def named(tag_type: int, name: str, payload: bytes) -> bytes:
    return struct.pack(">b", tag_type) + b_string(name) + payload


def compound(items: list[bytes]) -> bytes:
    """Serialize a TAG_Compound payload (named children + TAG_End)."""
    return b"".join(items) + struct.pack(">b", TAG_END)


def list_of(elem_tag: int, payloads: list[bytes]) -> bytes:
    """Serialize a TAG_List payload."""
    if not payloads:
        return struct.pack(">b", TAG_END) + struct.pack(">i", 0)
    return struct.pack(">b", elem_tag) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def int_payload(n: int) -> bytes:
    return struct.pack(">i", n)


def build_structure() -> bytes:
    palette = list_of(
        TAG_COMPOUND,
        [
            compound([named(TAG_STRING, "Name", b_string("minecraft:stone"))]),
            compound([named(TAG_STRING, "Name", b_string("minecraft:air"))]),
        ],
    )

    blocks_entries: list[bytes] = []
    for y in range(3):
        for x in range(3):
            for z in range(3):
                state_idx = 0 if y == 0 else 1
                pos_list = list_of(TAG_INT, [int_payload(x), int_payload(y), int_payload(z)])
                blocks_entries.append(
                    compound(
                        [
                            named(TAG_LIST, "pos", pos_list),
                            named(TAG_INT, "state", int_payload(state_idx)),
                        ]
                    )
                )
    blocks = list_of(TAG_COMPOUND, blocks_entries)

    size = list_of(TAG_INT, [int_payload(3), int_payload(3), int_payload(3)])
    entities = list_of(TAG_COMPOUND, [])

    root = compound(
        [
            named(TAG_INT, "DataVersion", int_payload(DATAVERSION_1_21_1)),
            named(TAG_LIST, "size", size),
            named(TAG_LIST, "palette", palette),
            named(TAG_LIST, "blocks", blocks),
            named(TAG_LIST, "entities", entities),
        ]
    )

    return struct.pack(">b", TAG_COMPOUND) + b_string("") + root


def main() -> int:
    out_path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(
        "src/main/resources/data/arcadiatweaks/structure/empty_platform.nbt"
    )
    out_path.parent.mkdir(parents=True, exist_ok=True)
    raw = build_structure()
    out_path.write_bytes(gzip.compress(raw))
    print(f"Wrote {out_path} ({out_path.stat().st_size} bytes, {len(raw)} bytes uncompressed)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
