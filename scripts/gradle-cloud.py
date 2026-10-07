#!/usr/bin/env python3
"""Cloud proxy/trust initialization; normal desktops can just use ./gradlew."""
import os
from pathlib import Path
import subprocess
import sys
from urllib.parse import urlsplit

root = Path(__file__).resolve().parent.parent
tools = Path('/workspace/toolchains')
env = os.environ.copy()
if (tools / 'jdk-21.0.8+9').exists():
    env.setdefault('JAVA_HOME', str(tools / 'jdk-21.0.8+9'))
if (tools / 'android-sdk').exists():
    env.setdefault('ANDROID_SDK_ROOT', str(tools / 'android-sdk'))
env.setdefault('GRADLE_USER_HOME', str(tools / 'gradle-cache'))
env.setdefault('ANDROID_USER_HOME', str(tools / 'android-user'))
Path(env['ANDROID_USER_HOME']).mkdir(parents=True, exist_ok=True)
options = env.get('JAVA_TOOL_OPTIONS', '')
if Path('/etc/ssl/certs/java/cacerts').is_file():
    options += ' -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'
proxy = urlsplit(env.get('HTTPS_PROXY', ''))
if proxy.hostname:
    for scheme in ('http', 'https'):
        options += f' -D{scheme}.proxyHost={proxy.hostname} -D{scheme}.proxyPort={proxy.port or 80}'
    options += ' -Dhttp.nonProxyHosts=localhost|127.*|[::1]'
env['JAVA_TOOL_OPTIONS'] = options.strip()
result = subprocess.run([str(root / 'gradlew'), '--no-daemon', *sys.argv[1:]], cwd=root, env=env)
sys.exit(result.returncode)
