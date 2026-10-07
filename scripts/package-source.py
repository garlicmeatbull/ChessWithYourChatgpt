#!/usr/bin/env python3
"""Package corresponding source, including NNUE, without credentials/build output."""
from pathlib import Path
import subprocess
import zipfile

root = Path(__file__).resolve().parent.parent
files = subprocess.check_output(['git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'], cwd=root).decode().split('\0')
net = 'vendor/stockfish/src/nn-1a298aa575a0.nnue'
assert (root / net).is_file(), 'Run scripts/stockfish-assets.py first'
files.append(net)
output = root / 'artifacts/ChessCoach-0.2.0-source.zip'
output.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(output, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for name in sorted(set(files)):
        path = root / name
        if not name or not path.is_file(): continue
        assert not path.is_symlink(), f'Unexpected source symlink: {name}'
        archive.write(path, 'ChessCoach-source/' + name)
print(output)
