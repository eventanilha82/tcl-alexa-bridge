#!/usr/bin/env python3
"""Fetch the locked Java build dependencies; print only their classpath."""
import hashlib
import json
import os
from pathlib import Path
import sys
import urllib.request

root = Path(__file__).resolve().parents[1]
cache = root/'build/dependencies'
cache.mkdir(parents=True, exist_ok=True)
paths = []
for entry in json.loads((root/'dependencies.lock.json').read_text()):
    target = cache/entry['name']
    if not target.exists():
        with urllib.request.urlopen(entry['url'], timeout=30) as response:
            data = response.read(8*1024*1024)
        if len(data) != entry['bytes'] or hashlib.sha256(data).hexdigest() != entry['sha256']:
            raise RuntimeError('Dependency integrity failure: '+entry['name'])
        temporary = target.with_suffix('.jar.tmp')
        temporary.write_bytes(data)
        temporary.replace(target)
        print('Fetched '+entry['name'], file=sys.stderr)
    if hashlib.sha256(target.read_bytes()).hexdigest() != entry['sha256']:
        raise RuntimeError('Cached dependency integrity failure: '+entry['name'])
    paths.append(str(target))
print(os.pathsep.join(paths))
