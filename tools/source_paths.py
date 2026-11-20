"""Resolve production sources that live in functional sub-directories of one package.

Every file under ``module/src/main/java/com/jiusis/mistep`` still declares
``package com.jiusis.mistep;``; only its directory changed.  Tools keep writing
``src/'MiStepHost.java'`` and get the real path back.
"""
from pathlib import Path

PACKAGE = 'module/src/main/java/com/jiusis/mistep'


class SourceRoot:
    """A package directory whose ``/`` operator finds files in sub-directories."""

    def __init__(self, package):
        self.package = Path(package)
        self._index = None

    def _find(self, name):
        direct = self.package / name
        if direct.exists():
            return direct
        if self._index is None:
            self._index = {}
            for path in sorted(self.package.rglob('*.java')):
                self._index.setdefault(path.name, path)
        return self._index.get(name, direct)

    def __truediv__(self, name):
        return self._find(str(name))

    def __fspath__(self):
        return str(self.package)

    def __str__(self):
        return str(self.package)

    def __repr__(self):
        return 'SourceRoot(%s)' % self.package


def package_dir(root, override=None):
    """The package directory under ``root``, or ``override`` when a caller supplies one."""
    return SourceRoot(Path(override) if override else Path(root) / PACKAGE)
