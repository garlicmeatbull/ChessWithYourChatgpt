#!/usr/bin/env python3
"""Direct ADB transport for this cloud machine's read-only host home.

Install adb-shell into /workspace/toolchains/adb-python. Only connects to
the local test emulator (127.0.0.1:5555); normal devices use Android SDK adb.
"""
from pathlib import Path
import sys
sys.path.insert(0, '/workspace/toolchains/adb-python')
from adb_shell.adb_device import AdbDeviceTcp
from adb_shell.auth.keygen import keygen
from adb_shell.auth.sign_pythonrsa import PythonRSASigner

def connect():
    key=Path('/workspace/toolchains/android-user/adb-test-key')
    key.parent.mkdir(parents=True,exist_ok=True)
    if not key.exists():keygen(str(key))
    signer=PythonRSASigner(key.with_suffix('.pub').read_text(),key.read_text())
    device=AdbDeviceTcp('127.0.0.1',5555,default_transport_timeout_s=30)
    device.connect(rsa_keys=[signer],auth_timeout_s=10)
    return device

if __name__=='__main__':
    device=connect()
    try:
        if sys.argv[1]=='push':device.push(sys.argv[2],sys.argv[3],transport_timeout_s=120)
        elif sys.argv[1]=='pull':device.pull(sys.argv[2],sys.argv[3],transport_timeout_s=120)
        else:print(device.shell(sys.argv[1],transport_timeout_s=180,read_timeout_s=180),end='')
    finally:device.close()
