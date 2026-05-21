#!/usr/bin/env python3
"""
Annotate FULL_REPORT.md with short explanations for each declaration line.
Writes FULL_REPORT_ANNOTATED.md and attempts to render FULL_REPORT_ANNOTATED.pdf via docs/generate_pdf.py.
"""
import os
import re
from pathlib import Path
import importlib.util

ROOT = Path(__file__).resolve().parents[1]
IN_MD = ROOT / 'FULL_REPORT.md'
OUT_MD = ROOT / 'FULL_REPORT_ANNOTATED.md'
OUT_PDF = ROOT / 'FULL_REPORT_ANNOTATED.pdf'

if not IN_MD.exists():
    print('INPUT missing:', IN_MD)
    raise SystemExit(1)

text = IN_MD.read_text(encoding='utf-8')
lines = text.splitlines()

in_code = False
current_file = None
out_lines = []

# heuristics
fun_prefix_map = [
    ('signIn', 'Performs user sign-in.'),
    ('signUp', 'Performs user sign-up.'),
    ('get', 'Returns or fetches data.'),
    ('set', 'Sets or updates a value.'),
    ('save', 'Saves data to storage or remote.'),
    ('load', 'Loads data from storage or remote.'),
    ('create', 'Creates a resource or document.'),
    ('delete', 'Deletes a resource.'),
    ('observe', 'Observes changes and returns a Flow/Stream.'),
    ('generate', 'Generates content (often AI-generated).'),
    ('build', 'Constructs or prepares an object.'),
    ('map', 'Maps or converts between representations.'),
    ('encrypt', 'Encrypts data.'),
    ('decrypt', 'Decrypts data.'),
    ('init', 'Initialisation routine.'),
    ('main', 'Entry point / main routine.'),
    ('setup', 'Setup/configuration routine.'),
    ('provide', 'DI provider method.'),
]

def explain_declaration(decl_line, file_path):
    s = decl_line
    # Kotlin patterns
    if 'data class' in s:
        m = re.search(r'data class\s+([A-Za-z0-9_]+)', s)
        name = m.group(1) if m else None
        return f"Data model (`{name}`) — holds structured data used across the app." if name else "Data model." 
    if 'enum class' in s:
        m = re.search(r'enum class\s+([A-Za-z0-9_]+)', s)
        name = m.group(1) if m else None
        return f"Enumeration (`{name}`) — defines a set of named constants." if name else "Enumeration." 
    if 'sealed class' in s:
        m = re.search(r'sealed class\s+([A-Za-z0-9_]+)', s)
        name = m.group(1) if m else None
        return f"Sealed class (`{name}`) — represents a closed hierarchy of types." if name else "Sealed class." 
    if 'annotation class' in s:
        m = re.search(r'annotation class\s+([A-Za-z0-9_]+)', s)
        name = m.group(1) if m else None
        return f"Annotation (`{name}`) — used for DI/metadata annotations." if name else "Annotation class." 
    if re.search(r'\binterface\b', s):
        m = re.search(r'interface\s+([A-Za-z0-9_]+)', s)
        name = m.group(1) if m else None
        return f"Interface (`{name}`) — contract implemented by components." if name else "Interface." 
    if re.search(r'\bobject\b', s):
        m = re.search(r'object\s+([A-Za-z0-9_]+)', s)
        name = m.group(1) if m else None
        return f"Singleton/object (`{name}`) — holds shared state or DI bindings." if name else "Object (singleton)." 
    if re.search(r'\bclass\b', s):
        # try to extract class name
        m = re.search(r'class\s+([A-Za-z0-9_]+)', s)
        name = m.group(1) if m else ''
        # heuristics based on name suffix
        low = name.lower()
        if name.endswith('ViewModel'):
            return f"ViewModel (`{name}`) — manages UI state and business logic for related screens." 
        if name.endswith('Repository'):
            return f"Repository (`{name}`) — handles data operations and coordination for a feature." 
        if name.endswith('Dao') or name.endswith('DAO'):
            return f"DAO (`{name}`) — Room Data Access Object for database operations." 
        if name.endswith('Activity') or name.endswith('Fragment'):
            return f"Android UI component (`{name}`) — activity/fragment presenting screens to the user." 
        if name.endswith('Manager'):
            return f"Manager (`{name}`) — orchestration/utility for managing resources or behaviors." 
        if name.endswith('Service'):
            return f"Service (`{name}`) — provides specific operations (often background tasks)." 
        if name.endswith('Module') or name.endswith('Module$'):
            return f"Dependency injection module (`{name}`) — provides DI bindings." 
        if name.endswith('Helper') or name.endswith('Utils') or name.endswith('Util'):
            return f"Utility (`{name}`) — helper methods for common tasks." 
        if 'Ai' in name or 'AI' in name or low.startswith('ai'):
            return f"AI-related component (`{name}`) — coordinates AI/LLM interactions." 
        if 'Auth' in name:
            return f"Authentication component (`{name}`) — handles user authentication flows." 
        if 'Database' in name or 'Db' in name:
            return f"Database abstraction (`{name}`) — encapsulates DB access/setup." 
        if name:
            return f"Class (`{name}`) — component used by the app (see file context: {file_path})."
        return "Class declaration." 
    # functions (Kotlin)
    m = re.search(r'fun\s+([A-Za-z0-9_]+)', s)
    if m:
        fname = m.group(1)
        for prefix, expl in fun_prefix_map:
            if fname.startswith(prefix):
                return f"Function `{fname}` — {expl}"
        # fallback heuristics
        if 'observe' in s or 'Flow' in s:
            return f"Function `{fname}` — returns observable/Flow data." 
        if 'suspend' in s:
            return f"Suspend function `{fname}` — coroutine-friendly async operation." 
        return f"Function `{fname}` — performs a specific operation used by the surrounding feature." 

    # Java/Generated classes
    m = re.search(r'public\s+final\s+class\s+([A-Za-z0-9_]+)', s)
    if m:
        name = m.group(1)
        return f"Generated Java class (`{name}`) — build-time utility or factory." 

    # Python
    m = re.search(r'^class\s+([A-Za-z0-9_]+)', s)
    if m:
        name = m.group(1)
        return f"Python class `{name}` — used by the app's scripts/tools (see {file_path})." 
    m = re.search(r'^def\s+([A-Za-z0-9_]+)', s)
    if m:
        name = m.group(1)
        for prefix, expl in fun_prefix_map:
            if name.startswith(prefix):
                return f"Function `{name}` — {expl}"
        return f"Function `{name}` — utility or script function." 

    return 'Declaration (no heuristic available).'

# Allowed scan roots and exclusions
ALLOWED_ROOT_PREFIXES = (str(ROOT / 'app') + os.sep, str(ROOT / 'docs') + os.sep, str(ROOT / 'Q_base appwrite') + os.sep)
EXCLUDE_SUBPATHS = (os.sep + '.venv' + os.sep, os.sep + 'build' + os.sep)

# Process
for line in lines:
    if line.startswith('## '):
        current_file = line[3:].strip()
        # If the file path is outside allowed prefixes, mark and continue without annotating
        fp = str((ROOT / current_file).resolve())
        allowed = False
        for p in ALLOWED_ROOT_PREFIXES:
            if fp.startswith(str(Path(p).resolve())):
                allowed = True
                break
        # Also allow top-level python files (direct children of ROOT)
        if not allowed and (ROOT / current_file).suffix == '.py' and (ROOT / current_file).parent == ROOT:
            allowed = True
        # Skip entries inside excluded subpaths
        for ex in EXCLUDE_SUBPATHS:
            if ex in fp:
                allowed = False
                break
        # store allowed flag in current_file var by prefixing with a marker
        if allowed:
            current_file = current_file
        else:
            current_file = None
        out_lines.append(line)
        continue
    if line.strip() == '```':
        out_lines.append(line)
        in_code = not in_code
        continue
    if in_code and line.strip().startswith('L'):
        # parse L12: declaration
        m = re.match(r'L(\d+):\s*(.*)', line)
        if m:
            ln = m.group(1)
            decl = m.group(2)
            expl = explain_declaration(decl, current_file)
            out_lines.append(f"L{ln}: {decl}  — {expl}")
            continue
    out_lines.append(line)

# Write annotated md
OUT_MD.write_text('\n'.join(out_lines), encoding='utf-8')
print('Wrote', OUT_MD)

# Try to render PDF using docs/generate_pdf.py
try:
    spec = importlib.util.spec_from_file_location('generate_pdf', str(ROOT / 'docs' / 'generate_pdf.py'))
    gp = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(gp)
    gp.build_pdf(str(OUT_MD), str(OUT_PDF))
    print('PDF created at', OUT_PDF)
except Exception as e:
    print('PDF generation failed:', e)
    print('You can create the PDF manually using docs/generate_pdf.py')
