#!/usr/bin/env python3
"""Fail builds whose executable does not contain the complete verified NNUE."""
import hashlib
import mmap
from pathlib import Path
import sys

root = Path(__file__).resolve().parent.parent
net = (root / 'vendor/stockfish/src/nn-1a298aa575a0.nnue').read_bytes()
expected = '1a298aa575a085434d29027978dc36867fe9c5bcea9376654b7a8eba1e52dfc2'
assert hashlib.sha256(net).hexdigest() == expected, 'Invalid source NNUE'
with open(sys.argv[1], 'rb') as binary:
    with mmap.mmap(binary.fileno(), 0, access=mmap.ACCESS_READ) as data:
        offset = data.find(net[:128])
        assert offset >= 0, 'NNUE missing from executable'
        assert hashlib.sha256(data[offset:offset + len(net)]).hexdigest() == expected, 'Incomplete embedded NNUE'
print('Complete Stockfish 19 NNUE verified:', sys.argv[1])
