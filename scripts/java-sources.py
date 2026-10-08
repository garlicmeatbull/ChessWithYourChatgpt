#!/usr/bin/env python3
"""Fetch pinned corresponding source for the APK's Apache Java dependencies."""
import hashlib,json,urllib.request
from pathlib import Path
root=Path(__file__).resolve().parent.parent
for item in json.loads((root/'vendor/java/sources.json').read_text()):
    destination=root/'vendor/java'/item['name']
    data=destination.read_bytes() if destination.is_file() else urllib.request.urlopen(item['url'],timeout=60).read()
    assert hashlib.sha256(data).hexdigest()==item['sha256'], 'Dependency source checksum mismatch: '+item['name']
    destination.write_bytes(data)
print('Java dependency source checksums verified')
