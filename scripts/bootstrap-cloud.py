#!/usr/bin/env python3
"""Pinned Linux x86_64 toolchains, installed outside the checkout; TLS + SHA256."""
import hashlib
from pathlib import Path
import shutil
import stat
import tarfile
import tempfile
import urllib.request
import zipfile

tools = Path('/workspace/toolchains')
downloads = tools / 'downloads'
downloads.mkdir(parents=True, exist_ok=True)

def fetch(url, digest):
    archive = downloads / url.rsplit('/', 1)[-1]
    if not archive.exists() or hashlib.sha256(archive.read_bytes()).hexdigest() != digest:
        temporary = archive.with_suffix('.partial')
        with urllib.request.urlopen(url, timeout=120) as response, temporary.open('wb') as output:
            shutil.copyfileobj(response, output)
        assert hashlib.sha256(temporary.read_bytes()).hexdigest() == digest, 'Download checksum mismatch'
        temporary.replace(archive)
    return archive

jdk = tools / 'jdk-21.0.8+9'
if not (jdk / 'bin/javac').exists():
    archive = fetch('https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.8%2B9/OpenJDK21U-jdk_x64_linux_hotspot_21.0.8_9.tar.gz', 'f2dc5418092c43003db8f9005c4a286e1c0104fea96ccdd49e8ebd037cac9219')
    with tarfile.open(archive) as tar:
        tar.extractall(tools, filter='data')

packages = [
    ('platform-35_r02.zip', '0988cacad01b38a18a47bac14a0695f246bc76c1b06c0eeb8eb0dc825ab0c8e0', 'android-35', 'platforms/android-35', 'android.jar'),
    ('build-tools_r35_linux.zip', 'bd3a4966912eb8b30ed0d00b0cda6b6543b949d5ffe00bea54c04c81e1561d88', 'android-15', 'build-tools/35.0.0', 'aapt2'),
    ('android-ndk-r27c-linux.zip', '59c2f6dc96743b5daf5d1626684640b20a6bd2b1d85b13156b90333741bad5cc', 'android-ndk-r27c', 'ndk/27.2.12479018', 'toolchains/llvm/prebuilt/linux-x86_64/bin/clang'),
]
for filename, digest, prefix, target, check in packages:
    destination = tools / 'android-sdk' / target
    if (destination / check).exists(): continue
    archive = fetch('https://dl.google.com/android/repository/' + filename, digest)
    with tempfile.TemporaryDirectory(dir=tools) as staging:
        base = Path(staging)
        with zipfile.ZipFile(archive) as z:
            for item in z.infolist():
                path = base / item.filename
                assert path.resolve().is_relative_to(base), 'Unsafe archive path'
                mode = item.external_attr >> 16
                if item.is_dir(): path.mkdir(parents=True, exist_ok=True); continue
                path.parent.mkdir(parents=True, exist_ok=True)
                if stat.S_ISLNK(mode):
                    link = z.read(item).decode()
                    assert (path.parent / link).resolve().is_relative_to(base), 'Unsafe archive link'
                    path.symlink_to(link)
                else:
                    with z.open(item) as source, path.open('wb') as out: shutil.copyfileobj(source, out)
                    if mode: path.chmod(stat.S_IMODE(mode))
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(base / prefix), destination)
print('JDK 21, Android API 35, build-tools 35.0.0, NDK r27c available')
