#!/usr/bin/env python3
"""检查 PR 是否提高了受影响产物的语义化版本。"""

from __future__ import annotations

from dataclasses import dataclass
from functools import total_ordering
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


SEMVER_PATTERN = re.compile(
    r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"
    r"(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?"
    r"(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$"
)
LEGACY_PATTERN = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)$")
SHARED_PATHS = {"CHANGELOG.md", "CONTRIBUTING.md", "README.md", ".gitignore"}


@total_ordering
@dataclass(frozen=True)
class SemVer:
    core: tuple[int, int, int]
    prerelease: tuple[str, ...] | None = None

    @classmethod
    def parse(cls, value: str, *, allow_legacy: bool = False) -> "SemVer":
        match = SEMVER_PATTERN.fullmatch(value)
        if match:
            prerelease = tuple(match.group(4).split(".")) if match.group(4) else None
            if prerelease and any(part.isdigit() and len(part) > 1 and part.startswith("0") for part in prerelease):
                raise ValueError(f"预发布数字标识不能包含前导零: {value}")
            return cls(tuple(int(match.group(i)) for i in range(1, 4)), prerelease)

        legacy = LEGACY_PATTERN.fullmatch(value) if allow_legacy else None
        if legacy:
            return cls((int(legacy.group(1)), int(legacy.group(2)), 0))
        raise ValueError(f"版本必须使用 MAJOR.MINOR.PATCH 格式: {value}")

    def __lt__(self, other: object) -> bool:
        if not isinstance(other, SemVer):
            return NotImplemented
        if self.core != other.core:
            return self.core < other.core
        if self.prerelease is None:
            return False
        if other.prerelease is None:
            return True
        for left, right in zip(self.prerelease, other.prerelease):
            if left == right:
                continue
            left_numeric = left.isdigit()
            right_numeric = right.isdigit()
            if left_numeric and right_numeric:
                return int(left) < int(right)
            if left_numeric != right_numeric:
                return left_numeric
            return left < right
        return len(self.prerelease) < len(other.prerelease)


def run_git(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        check=True,
        text=True,
        encoding="utf-8",
        stdout=subprocess.PIPE,
    )
    return result.stdout.strip()


def pom_version(xml_text: str) -> str:
    root = ET.fromstring(xml_text)
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    element = root.find("m:version", namespace)
    if element is None or not element.text:
        raise ValueError("pom.xml 中缺少 project.version")
    return element.text.strip()


def current_pom_version(path: str) -> str:
    return pom_version(Path(path).read_text(encoding="utf-8"))


def base_pom_version(base_sha: str, path: str) -> str:
    return pom_version(run_git("show", f"{base_sha}:{path}"))


def is_shared_path(path: str) -> bool:
    return path in SHARED_PATHS or path.startswith(".github/")


def check_component(name: str, path: str, base_sha: str) -> str:
    old_text = base_pom_version(base_sha, path)
    new_text = current_pom_version(path)
    old_version = SemVer.parse(old_text, allow_legacy=True)
    new_version = SemVer.parse(new_text)
    if new_version <= old_version:
        raise ValueError(f"{name} 版本必须提高: {old_text} -> {new_text}")
    print(f"{name} 版本检查通过: {old_text} -> {new_text}")
    return new_text


def main() -> int:
    if len(sys.argv) != 2:
        print("用法: check_versions.py <base-sha>", file=sys.stderr)
        return 2

    base_sha = sys.argv[1]
    changed_paths = [path for path in run_git("diff", "--name-only", f"{base_sha}...HEAD").splitlines() if path]
    if not changed_paths:
        print("没有需要检查的变更。")
        return 0

    product_paths = [path for path in changed_paths if not is_shared_path(path)]
    plugin_changed = any(not path.startswith("simpban-web/") for path in product_paths)
    web_changed = any(path.startswith("simpban-web/") for path in product_paths)

    # 只有仓库级文件变化时，默认由插件版本承载该变更。
    if not plugin_changed and not web_changed:
        plugin_changed = True

    try:
        if "CHANGELOG.md" not in changed_paths:
            raise ValueError("每个变更集都必须更新 CHANGELOG.md")

        updated_versions: list[tuple[str, str]] = []
        if plugin_changed:
            version = check_component("Minecraft 插件", "pom.xml", base_sha)
            updated_versions.append(("Minecraft 插件", version))
        if web_changed:
            version = check_component("网页服务", "simpban-web/pom.xml", base_sha)
            updated_versions.append(("网页服务", version))

        changelog = Path("CHANGELOG.md").read_text(encoding="utf-8")
        for name, version in updated_versions:
            if f"## [{version}]" not in changelog:
                raise ValueError(f"CHANGELOG.md 中缺少 {name} {version} 的版本标题")
    except (ET.ParseError, OSError, subprocess.CalledProcessError, ValueError) as error:
        print(f"版本检查失败: {error}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
