#!/usr/bin/env python3
"""
Generate a FULL_REPORT.md listing classes/functions per source file and render to PDF.
Usage:
  python3 scripts/generate_full_report.py

This script writes FULL_REPORT.md and FULL_REPORT.pdf at the repository root.
"""
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT_MD = ROOT / "FULL_REPORT.md"
OUT_PDF = ROOT / "FULL_REPORT.pdf"

# Regexes
kt_class_re = re.compile(r'^\s*(?:public\s+|private\s+|protected\s+)?(?:sealed\s+)?(?:data\s+)?(?:annotation\s+class\s+|object\s+|enum\s+class\s+|class\s+|interface\s+|enum\s+)\s*([A-Za-z0-9_]+)')
kt_fun_re = re.compile(r'^\s*(?:public\s+|private\s+|protected\s+)?(?:suspend\s+)?fun\s+([A-Za-z0-9_]+)')
java_class_re = re.compile(r'^\s*(?:public\s+|private\s+|protected\s+)?(?:class|interface|enum)\s+([A-Za-z0-9_]+)')
py_class_re = re.compile(r'^\s*class\s+([A-Za-z0-9_]+)')
py_def_re = re.compile(r'^\s*def\s+([A-Za-z0-9_]+)')

INCLUDE_EXT = ('.kt', '.java', '.py')

report_lines = []
report_lines.append('# FULL PROJECT DECLARATIONS\n\n')
report_lines.append('This file lists declaration lines (classes, interfaces, enums, objects, and functions) found across the project.\n\n')

files_scanned = 0
matches_found = 0

for dirpath, dirnames, filenames in os.walk(ROOT):
    # Skip build folders to speed up
    if 'build' in dirpath.split(os.sep):
        continue
    for fname in filenames:
        if not fname.endswith(INCLUDE_EXT):
            continue
        files_scanned += 1
        fpath = Path(dirpath) / fname
        rel = fpath.relative_to(ROOT)
        try:
            with open(fpath, 'r', encoding='utf-8', errors='ignore') as f:
                lines = f.readlines()
        except Exception:
            continue
        file_matches = []
        for i, line in enumerate(lines, start=1):
            s = line.rstrip('\n')
            if fname.endswith('.kt'):
                if kt_class_re.match(s) or kt_fun_re.match(s):
                    file_matches.append((i, s.strip()))
                    continue
            if fname.endswith('.java'):
                if java_class_re.match(s):
                    file_matches.append((i, s.strip()))
                    continue
            if fname.endswith('.py'):
                if py_class_re.match(s) or py_def_re.match(s):
                    file_matches.append((i, s.strip()))
                    continue
        if file_matches:
            matches_found += len(file_matches)
            report_lines.append(f'## {rel}\n\n')
            report_lines.append('```\n')
            for ln, txt in file_matches:
                report_lines.append(f'L{ln}: {txt}\n')
            report_lines.append('```\n\n')

with open(OUT_MD, 'w', encoding='utf-8') as f:
    f.writelines(report_lines)

print(f'Wrote {OUT_MD} (scanned {files_scanned} files, found {matches_found} declaration lines).')

# Try to import the provided PDF builder
try:
    import importlib.util
    spec = importlib.util.spec_from_file_location('generate_pdf', str(ROOT / 'docs' / 'generate_pdf.py'))
    gp = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(gp)
    gp.build_pdf(str(OUT_MD), str(OUT_PDF))
    print(f'PDF created at: {OUT_PDF}')
except Exception as e:
    print('PDF generation failed:', e)
    print('You can generate the PDF manually by running docs/generate_pdf.py build_pdf(...) or using the script with Python.')
