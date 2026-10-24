#!/usr/bin/env bash
# Source this file in Codex cloud tasks. Each task is already isolated; use the existing checkout.
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/workspace/.gradle}"
if ! command -v javac >/dev/null 2>&1; then
    export JAVA_HOME=/workspace/tools/jdk-root/usr/lib/jvm/java-21-openjdk-amd64
    export PATH="$JAVA_HOME/bin:$PATH"
fi
# Java does not automatically consume HTTPS_PROXY. Store host/port only, never credentials.
python3 - <<'PY'
import os,pathlib,urllib.parse
proxy=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY',''))
if proxy.hostname and proxy.port:
    path=pathlib.Path(os.environ['GRADLE_USER_HOME'])/'gradle.properties'
    path.parent.mkdir(parents=True,exist_ok=True)
    old=path.read_text() if path.exists() else ''
    prefixes=tuple(f'systemProp.{scheme}.proxy{key}=' for scheme in ('http','https') for key in ('Host','Port'))
    lines=[line for line in old.splitlines() if not line.startswith(prefixes)]
    for scheme in ('http','https'):
        lines.extend([f'systemProp.{scheme}.proxyHost={proxy.hostname}',f'systemProp.{scheme}.proxyPort={proxy.port}'])
    path.write_text('\n'.join(lines)+'\n')
PY
