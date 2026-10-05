#!/usr/bin/env python3
"""Upload the collège content pack's PDFs into the library, one resource per file, on its module.

The modules themselves come from the V16 content migration; this only adds the files, which
must go through the API so they land in storage. It uploads what students may see right away:
each leçon's cours and the exercise sheets. Corrigés, DC/DS subjects and the teacher's complete
documents are left out on purpose: they go up by hand, after the class or the devoir.

Safe to run twice: a resource whose title already exists on that module is skipped.

    EDUFLOW_PASSWORD=... python3 scripts/upload_college_pdfs.py \\
        --pack ~/Downloads/Plateforme_Informatique_College \\
        --api http://localhost:8080 --email admin@eduflow.dz
"""
import argparse
import json
import os
import subprocess
import sys
import urllib.request
from pathlib import Path

LEVELS = {'7eme_annee': ('SEVENTH_BASE', '7'), '8eme_annee': ('EIGHTH_BASE', '8'), '9eme_annee': ('NINTH_BASE', '9')}


def call(api, token, method, path, body=None):
    req = urllib.request.Request(api + path, method=method,
                                 data=json.dumps(body).encode() if body is not None else None)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    with urllib.request.urlopen(req) as res:
        return json.load(res)


def upload(api, token, meta, pdf):
    # urllib has no multipart support; curl does, and ships with macOS and the server image
    out = subprocess.run(
        ['curl', '-sS', '--fail-with-body', '-X', 'POST', api + '/api/v1/resources',
         '-H', 'Authorization: Bearer ' + token,
         '-F', 'resource=' + json.dumps(meta) + ';type=application/json',
         '-F', 'file=@' + str(pdf) + ';type=application/pdf'],
        capture_output=True, text=True)
    if out.returncode != 0:
        raise RuntimeError(f'{pdf}: {out.stdout or out.stderr}')


def existing_titles(api, token, course_id):
    page = call(api, token, 'GET', f'/api/v1/resources?courseId={course_id}&size=200')
    return {r['title'] for r in page['content']}


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--pack', required=True, type=Path, help='the Plateforme_Informatique_College folder')
    p.add_argument('--api', default='http://localhost:8080')
    p.add_argument('--email', required=True, help='an admin account')
    p.add_argument('--dry-run', action='store_true', help='list what would be uploaded, upload nothing')
    a = p.parse_args()

    password = os.environ.get('EDUFLOW_PASSWORD')
    if not password:
        sys.exit('Set EDUFLOW_PASSWORD to the admin password.')
    token = call(a.api, None, 'POST', '/api/v1/auth/login',
                 {'email': a.email, 'password': password})['tokens']['accessToken']

    structure = json.load(open(a.pack / 'structure.json', encoding='utf-8'))
    uploaded = skipped = 0
    for niveau in structure['niveaux']:
        level, n = LEVELS[niveau['dossier']]
        for module in niveau['modules']:
            code = f"INF{n}-M{module['numero']}"
            courses = call(a.api, token, 'GET', f'/api/v1/courses?level={level}&size=50')['content']
            course = next((c for c in courses if c['code'] == code), None)
            if course is None:
                sys.exit(f'Module {code} is missing: has the V16 content migration run?')
            have = existing_titles(a.api, token, course['id'])

            files = [(f"Leçon {l['numero']} – Cours : {l['titre']}", l['fichiers']['cours'])
                     for l in module['lecons']]
            files += [(f"{module['titre']} – Exercices série {e['serie'][-1]}", e['sujet'])
                      for e in module['exercices']]
            for title, rel in files:
                if title in have:
                    skipped += 1
                    continue
                print(f'{code}  {title}')
                if not a.dry_run:
                    upload(a.api, token, {'title': title[:255], 'type': 'PDF',
                                          'courseId': course['id'], 'level': level}, a.pack / rel)
                uploaded += 1

    verb = 'would upload' if a.dry_run else 'uploaded'
    print(f'\nDone: {verb} {uploaded}, skipped {skipped} already there.')


if __name__ == '__main__':
    main()
