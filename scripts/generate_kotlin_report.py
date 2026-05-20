#!/usr/bin/env python3
"""
Generate a Kotlin/Android-only declarations report and annotated PDF.
Writes FULL_REPORT_KOTLIN.md and FULL_REPORT_KOTLIN_ANNOTATED.pdf in the repo root.
"""
import os
import re
from pathlib import Path
import importlib.util

ROOT = Path(__file__).resolve().parents[1]
OUT_MD = ROOT / 'FULL_REPORT_KOTLIN.md'
OUT_ANNOTATED_MD = ROOT / 'FULL_REPORT_KOTLIN_ANNOTATED.md'
OUT_PDF = ROOT / 'FULL_REPORT_KOTLIN_ANNOTATED.pdf'

kt_class_re = re.compile(r'^\s*(?:public\s+|private\s+|protected\s+)?(?:sealed\s+)?(?:data\s+)?(?:annotation\s+class\s+|object\s+|enum\s+class\s+|class\s+|interface\s+|enum\s+)\s*([A-Za-z0-9_]+)')
kt_fun_re = re.compile(r'^\s*(?:public\s+|private\s+|protected\s+)?(?:suspend\s+)?fun\s+([A-Za-z0-9_]+)')

INCLUDE_PATH_SUBSTR = ('app/', 'core-', 'archive/')
EXCLUDE_SUBPATHS = ('/.venv/', '/build/')

report_lines = ['# KOTLIN & ANDROID DECLARATIONS\n\n']
annot_lines = ['# KOTLIN & ANDROID DECLARATIONS (ANNOTATED)\n\n']

files_scanned = 0
matches_found = 0

for dirpath, dirnames, filenames in os.walk(ROOT):
    # skip build and venv folders
    if any(ex in dirpath for ex in EXCLUDE_SUBPATHS):
        continue
    for fname in filenames:
        if not fname.endswith('.kt'):
            continue
        fpath = Path(dirpath) / fname
        rel = fpath.relative_to(ROOT)
        # include only kotlin/Android modules
        if not any(sub in str(rel) for sub in INCLUDE_PATH_SUBSTR):
            continue
        files_scanned += 1
        try:
            with open(fpath, 'r', encoding='utf-8', errors='ignore') as f:
                lines = f.readlines()
        except Exception:
            continue
        file_matches = []
        for i, line in enumerate(lines, start=1):
            s = line.rstrip('\n')
            if kt_class_re.match(s) or kt_fun_re.match(s):
                file_matches.append((i, s.strip()))
        if file_matches:
            matches_found += len(file_matches)
            report_lines.append(f'## {rel}\n')
            report_lines.append('```\n')
            for ln, txt in file_matches:
                report_lines.append(f'L{ln}: {txt}\n')
            report_lines.append('```\n\n')

            # Add annotated section
            annot_lines.append(f'## {rel}\n')
            annot_lines.append('```\n')
            for ln, txt in file_matches:
                # simple heuristic explanations
                expl = None
                if 'data class' in txt:
                    expl = 'Data model — holds structured fields.'
                elif 'enum class' in txt:
                    expl = 'Enumeration — named constants.'
                elif 'sealed class' in txt:
                    expl = 'Sealed hierarchy — closed set of subclasses.'
                elif 'interface' in txt:
                    expl = 'Interface — contract for implementations.'
                elif 'object' in txt:
                    expl = 'Singleton object — shared utilities/DI.'
                elif 'class' in txt:
                    name = re.search(r'class\s+([A-Za-z0-9_]+)', txt)
                    nm = name.group(1) if name else ''
                    if nm.endswith('ViewModel'):
                        expl = 'ViewModel — manages UI state and interactions.'
                    elif nm.endswith('Repository'):
                        expl = 'Repository — coordinates data sources for a feature.'
                    elif nm.endswith('Dao') or nm.endswith('DAO'):
                        expl = 'DAO — Room database access object.'
                    elif nm.endswith('Activity') or nm.endswith('Fragment'):
                        expl = 'UI component — activity/fragment for screens.'
                    elif 'Ai' in nm or 'AI' in nm or nm.lower().startswith('ai'):
                        expl = 'AI component — orchestrates LLM/AI requests.'
                    else:
                        expl = 'Class — app component; see source for details.'
                elif txt.startswith('fun') or ' fun ' in txt or txt.lstrip().startswith('suspend fun'):
                    fn = re.search(r'fun\s+([A-Za-z0-9_]+)', txt)
                    fname = fn.group(1) if fn else 'function'
                    if fname.startswith('get'):
                        expl = 'Getter/fetcher — retrieves data.'
                    elif fname.startswith('set') or fname.startswith('save'):
                        expl = 'Persists or updates data.'
                    elif fname.startswith('create'):
                        expl = 'Creates resources/documents.'
                    elif fname.startswith('observe') or 'Flow' in txt:
                        expl = 'Returns observable/Flow for reactive updates.'
                    elif fname.startswith('generate'):
                        expl = 'Generates content (often AI).' 
                    else:
                        expl = 'Function — performs a feature-specific operation.'
                else:
                    expl = 'Declaration.'
                annot_lines.append(f'L{ln}: {txt}  — {expl}\n')
            annot_lines.append('```\n\n')

# write outputs
OUT_MD.write_text('\n'.join(report_lines), encoding='utf-8')
OUT_ANNOTATED_MD.write_text('\n'.join(annot_lines), encoding='utf-8')
print(f'Wrote {OUT_MD} and {OUT_ANNOTATED_MD} (scanned {files_scanned} files, found {matches_found} lines)')

# build pdf from annotated markdown
try:
    spec = importlib.util.spec_from_file_location('generate_pdf', str(ROOT / 'docs' / 'generate_pdf.py'))
    gp = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(gp)
    gp.build_pdf(str(OUT_ANNOTATED_MD), str(OUT_PDF))
    print('PDF created at', OUT_PDF)
except Exception as e:
    print('PDF generation failed:', e)
    print('You can generate the PDF manually with docs/generate_pdf.py')
