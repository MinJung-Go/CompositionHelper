#!/usr/bin/env python3
"""Verify 16 KiB alignment of 64-bit native libraries in an Android APK."""

import argparse
import struct
import zipfile

PAGE_SIZE = 16 * 1024
ELF_HEADER = struct.Struct("<16sHHIQQQIHHHHHH")
PROGRAM_HEADER = struct.Struct("<IIQQQQQQ")
ZIP_LOCAL_HEADER = struct.Struct("<IHHHHHIIIHH")


def check_apk(path):
    problems = []
    checked = 0
    with zipfile.ZipFile(path) as apk, open(path, "rb") as raw_apk:
        for library in apk.infolist():
            if not library.filename.endswith(".so") or not library.filename.startswith(
                ("lib/arm64-v8a/", "lib/x86_64/")
            ):
                continue
            checked += 1
            with apk.open(library) as elf:
                header = ELF_HEADER.unpack(elf.read(ELF_HEADER.size))
                if not header[0].startswith(b"\x7fELF\x02\x01"):
                    problems.append(f"{library.filename}: expected 64-bit little-endian ELF")
                    continue
                program_offset, program_size, program_count = header[5], header[9], header[10]
                loads = []
                for index in range(program_count):
                    elf.seek(program_offset + index * program_size)
                    program = PROGRAM_HEADER.unpack(elf.read(PROGRAM_HEADER.size))
                    if program[0] == 1:  # PT_LOAD
                        loads.append(program)
                if not loads or any(
                    program[7] < PAGE_SIZE or (program[2] - program[3]) % PAGE_SIZE
                    for program in loads
                ):
                    problems.append(f"{library.filename}: ELF LOAD segments are not 16 KiB aligned")

            if library.compress_type == zipfile.ZIP_STORED:
                raw_apk.seek(library.header_offset)
                header = ZIP_LOCAL_HEADER.unpack(raw_apk.read(ZIP_LOCAL_HEADER.size))
                data_offset = library.header_offset + ZIP_LOCAL_HEADER.size + header[-2] + header[-1]
                if data_offset % PAGE_SIZE:
                    problems.append(f"{library.filename}: uncompressed APK entry is not 16 KiB aligned")

    if not checked:
        problems.append("No 64-bit native libraries were found")
    return checked, problems


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk")
    args = parser.parse_args()
    count, problems = check_apk(args.apk)
    if problems:
        raise SystemExit("\n".join(problems))
    print(f"Checked {count} native libraries for 16 KiB alignment.")
