#!/usr/bin/env python3
"""Build a shareable project ZIP without secrets, Git history, or runtime data."""

from __future__ import annotations

import argparse
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT = PROJECT_ROOT / 'dist' / 'MOVE-AI.zip'

EXCLUDED_DIRECTORIES = {
    '.git',
    '.gradle',
    '.idea',
    '.pytest_cache',
    '.ruff_cache',
    '.venv',
    '.vite',
    '.vscode',
    '__pycache__',
    'build',
    'data',
    'dist',
    'node_modules',
    'out',
    'venv',
}
EXCLUDED_SUFFIXES = {'.class', '.jar', '.key', '.m4a', '.mp3', '.pem', '.pyc', '.wav'}


def is_secret_env_file(path: Path) -> bool:
    """Exclude .env and variants such as .env.local, but keep .env.example."""
    return path.name == '.env' or (
        path.name.startswith('.env.') and path.name != '.env.example'
    )


def should_exclude(relative_path: Path) -> bool:
    return (
        any(part in EXCLUDED_DIRECTORIES for part in relative_path.parts)
        or is_secret_env_file(relative_path)
        or relative_path.suffix.lower() in EXCLUDED_SUFFIXES
    )


def build_release_zip(output_path: Path) -> tuple[int, int]:
    output_path = output_path.resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)

    included_files = 0
    included_bytes = 0
    with ZipFile(output_path, 'w', compression=ZIP_DEFLATED, compresslevel=9) as archive:
        for source_path in sorted(PROJECT_ROOT.rglob('*')):
            if not source_path.is_file() or source_path.is_symlink():
                continue

            relative_path = source_path.relative_to(PROJECT_ROOT)
            if should_exclude(relative_path) or source_path.resolve() == output_path:
                continue

            archive.write(source_path, relative_path.as_posix())
            included_files += 1
            included_bytes += source_path.stat().st_size

    return included_files, included_bytes


def verify_release_zip(output_path: Path) -> None:
    with ZipFile(output_path) as archive:
        forbidden = [
            name
            for name in archive.namelist()
            if should_exclude(Path(name))
        ]
    if forbidden:
        raise RuntimeError(f'배포 ZIP에 제외 대상이 포함됨: {forbidden}')


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        '--output',
        type=Path,
        default=DEFAULT_OUTPUT,
        help=f'생성할 ZIP 경로 (기본: {DEFAULT_OUTPUT})',
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    file_count, source_bytes = build_release_zip(args.output)
    verify_release_zip(args.output)
    print(f'created: {args.output.resolve()}')
    print(f'files: {file_count}, source bytes: {source_bytes}')
    print('verified exclusions: .env*, .git/, data/, build/cache artifacts, private keys, audio')


if __name__ == '__main__':
    main()
