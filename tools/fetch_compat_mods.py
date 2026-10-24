#!/usr/bin/env python3
"""Fetch the exact, hash-verified third-party mods used by the compatibility tests."""
import hashlib
import json
from pathlib import Path
from urllib.request import urlopen

root = Path(__file__).resolve().parents[1]
destination = root / 'run/compat-mods'
destination.mkdir(parents=True, exist_ok=True)
for mod in json.loads((root/'tools/compat-mods.lock.json').read_text()):
    path = destination / mod['filename']
    if path.exists():
        if hashlib.sha512(path.read_bytes()).hexdigest() != mod['sha512']:
            raise RuntimeError(f'Existing file has an unexpected hash; refusing to overwrite: {path}')
    else:
        data = urlopen(mod['url'], timeout=120).read()
        if hashlib.sha512(data).hexdigest() != mod['sha512']:
            raise RuntimeError(f'Checksum failed: {mod["filename"]}')
        path.write_bytes(data)
    print(f'Verified {mod["project"]} {mod["version"]}')
