#!/usr/bin/env python3
"""Pinned official release assets, with full SHA-256 verification and no execution."""
from pathlib import Path
import argparse
import hashlib
import struct
import subprocess
import tarfile

ROOT=Path(__file__).resolve().parent.parent
ARCHIVE_SHA='ebb24051aa4a222b4daaf049b882ecf1163d370c128fe02316602643f4d5e426'
ENGINE_SHA='ffd8fc2004d3d19f9fdab95d84c92709aea92e8d3af405eda0b281b23cf1daf8'
NET_SHA='1a298aa575a085434d29027978dc36867fe9c5bcea9376654b7a8eba1e52dfc2'
NET_SIZE=98511183
URL='https://github.com/official-stockfish/Stockfish/releases/download/sf_19/stockfish-android-arm64-universal.tar.gz'
def valid(path,sha):
    return path.is_file() and hashlib.sha256(path.read_bytes()).hexdigest()==sha
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--archive',type=Path);args=parser.parse_args()
    engine=ROOT/'app/src/main/jniLibs/arm64-v8a/libstockfish.so'
    net=ROOT/'vendor/stockfish/src/nn-1a298aa575a0.nnue'
    if valid(net,NET_SHA):
        print('Verified Stockfish 19 NNUE; reusing');return
    archive=args.archive or ROOT/'app/build/downloads/stockfish19-android.tar.gz'
    if not archive.exists():
        archive.parent.mkdir(parents=True,exist_ok=True)
        subprocess.run(['curl','-fLSs','--retry','2',URL,'-o',str(archive)],check=True)
    if not valid(archive,ARCHIVE_SHA):raise SystemExit('Official Stockfish archive checksum mismatch')
    with tarfile.open(archive) as tar:
        binary=tar.extractfile('stockfish/stockfish-android-arm64-universal').read()
    if hashlib.sha256(binary).hexdigest()!=ENGINE_SHA:raise SystemExit('ARM64 executable checksum mismatch')
    # The official universal executable embeds one contiguous NNUE file.
    # Extract bytes only; never execute a downloaded binary to export data.
    header=struct.pack('<I',0x6A448AFA);position=0;network=None
    while True:
        position=binary.find(header,position)
        if position<0:break
        chunk=binary[position:position+NET_SIZE]
        if len(chunk)==NET_SIZE and hashlib.sha256(chunk).hexdigest()==NET_SHA:network=chunk;break
        position+=1
    if network is None:raise SystemExit('Verified NNUE not found in official archive')
    net.parent.mkdir(parents=True,exist_ok=True);net.write_bytes(network)
    print('Verified and installed official Stockfish 19 NNUE')
if __name__=='__main__':main()
