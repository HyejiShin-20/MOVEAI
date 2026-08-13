# -*- coding: utf-8 -*-
import collections
import io
import json
import re
import sys
from pathlib import Path

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

BASE = Path(__file__).resolve().parent.parent / 'datasets'
files = [
    'synthetic_dataset_A.json',
    'synthetic_dataset_B.json',
    'synthetic_dataset_C.json',
    'synthetic_dataset_D.json',
]

def norm(s):
    """loose normalization for substring check"""
    if s is None: return ''
    return re.sub(r'\s+', '', s).replace('"','').replace('"','').replace('"','')

grand = collections.Counter()

for fn in files:
    with (BASE / fn).open(encoding='utf-8') as dataset_file:
        d = json.load(dataset_file)
    print('='*72)
    print(fn, '   [', d['dataset_meta'].get('dataset_version'), ']')
    print('='*72)

    nodes = d['nodes']; routes = d['routes']; segs = d['route_segments']
    reports = d['field_reports']; queries = d['rag_test_queries']
    ks = [k for r in reports for k in r.get('expected_knowledge_items',[])]

    node_codes = {n['node_code'] for n in nodes}
    seg_codes  = {s['segment_code'] for s in segs}
    route_codes= {r['route_code'] for r in routes}
    k_codes    = [k['knowledge_code'] for k in ks]
    place_code = d['place']['place_code']

    issues = []

    # --- duplicate codes
    for label, lst in [('node',[n['node_code'] for n in nodes]),
                       ('segment',[s['segment_code'] for s in segs]),
                       ('route',[r['route_code'] for r in routes]),
                       ('knowledge',k_codes),
                       ('report',[r['report_code'] for r in reports]),
                       ('query',[q['query_code'] for q in queries])]:
        dup = [c for c,n in collections.Counter(lst).items() if n>1]
        if dup: issues.append(('DUP', f'{label} 코드 중복: {dup}'))

    # --- dangling node refs everywhere
    def chk_node(code, where):
        if code and code not in node_codes:
            issues.append(('REF', f'{where} -> 존재하지 않는 node {code}'))
    for n in nodes: chk_node(n.get('parent_node_code'), f"node {n['node_code']}.parent")
    for r in routes:
        chk_node(r.get('start_node_code'), f"route {r['route_code']}.start")
        chk_node(r.get('destination_node_code'), f"route {r['route_code']}.dest")
    for s in segs:
        chk_node(s.get('from_node_code'), f"seg {s['segment_code']}.from")
        chk_node(s.get('to_node_code'),   f"seg {s['segment_code']}.to")
        if s.get('route_code') not in route_codes:
            issues.append(('REF', f"seg {s['segment_code']} -> 없는 route {s.get('route_code')}"))
    for r in reports:
        if r.get('place_code') != place_code:
            issues.append(('REF', f"report {r['report_code']}.place={r.get('place_code')} != {place_code}"))
        chk_node(r.get('selected_scope_node_code'), f"report {r['report_code']}.scope")

    # --- route segment continuity
    for route in routes:
        route_code = route['route_code']
        route_segs = [s for s in segs if s.get('route_code') == route_code]
        if not route_segs:
            issues.append(('ROUTE', f'{route_code}에 구간이 없음'))
            continue

        sequence_numbers = [s.get('sequence_no') for s in route_segs]
        if any(not isinstance(sequence_no, int) for sequence_no in sequence_numbers):
            issues.append(('ROUTE', f'{route_code}.sequence_no가 정수가 아님: {sequence_numbers}'))
            continue

        ordered = sorted(route_segs, key=lambda segment: segment['sequence_no'])
        expected_sequence = list(range(1, len(ordered) + 1))
        actual_sequence = [segment['sequence_no'] for segment in ordered]
        if actual_sequence != expected_sequence:
            issues.append((
                'ROUTE',
                f'{route_code}.sequence_no가 1..N 연속이 아님: {actual_sequence}',
            ))

        if ordered[0].get('from_node_code') != route.get('start_node_code'):
            issues.append((
                'ROUTE',
                f"{route_code} 첫 구간.from={ordered[0].get('from_node_code')} "
                f"!= route.start={route.get('start_node_code')}",
            ))
        if ordered[-1].get('to_node_code') != route.get('destination_node_code'):
            issues.append((
                'ROUTE',
                f"{route_code} 마지막 구간.to={ordered[-1].get('to_node_code')} "
                f"!= route.destination={route.get('destination_node_code')}",
            ))

        for previous, current in zip(ordered, ordered[1:]):
            if previous.get('to_node_code') != current.get('from_node_code'):
                issues.append((
                    'ROUTE',
                    f"{route_code} seq{previous['sequence_no']}.to={previous.get('to_node_code')} "
                    f"!= seq{current['sequence_no']}.from={current.get('from_node_code')}",
                ))

    # --- knowledge target refs
    for k in ks:
        t = k.get('target',{})
        tt, tc, st, tf = t.get('target_type'), t.get('target_code'), t.get('target_resolution_status'), t.get('target_free_text')
        if tt == 'NODE':
            if tc not in node_codes: issues.append(('REF', f"{k['knowledge_code']}.target -> 없는 node {tc}"))
        elif tt == 'SEGMENT':
            if tc not in seg_codes: issues.append(('REF', f"{k['knowledge_code']}.target -> 없는 segment {tc}"))
        elif tt == 'PLACE':
            if tc not in (place_code, None): issues.append(('REF', f"{k['knowledge_code']}.target -> place {tc}"))
        elif tt == 'UNKNOWN':
            if st != 'UNRESOLVED': issues.append(('LOGIC', f"{k['knowledge_code']} UNKNOWN인데 status={st}"))
            if not tf: issues.append(('LOGIC', f"{k['knowledge_code']} UNKNOWN인데 target_free_text 없음"))
            if tc: issues.append(('LOGIC', f"{k['knowledge_code']} UNKNOWN인데 target_code={tc}"))
        if st == 'RESOLVED' and not tc:
            issues.append(('LOGIC', f"{k['knowledge_code']} RESOLVED인데 target_code 없음"))

    # --- source_excerpt containment
    tmap = {r['report_code']: r.get('transcript','') for r in reports}
    for r in reports:
        tr = norm(r.get('transcript'))
        for k in r.get('expected_knowledge_items',[]):
            ex = k.get('source_excerpt')
            if not ex:
                issues.append(('EXCERPT', f"{k['knowledge_code']} source_excerpt 없음"))
            elif norm(ex) not in tr:
                issues.append(('EXCERPT', f"{k['knowledge_code']} source_excerpt가 transcript에 없음: \"{ex[:40]}...\""))

    # --- OTHER / custom label consistency
    for k in ks:
        if k.get('category')=='OTHER' and not k.get('custom_category_label'):
            issues.append(('ENUM', f"{k['knowledge_code']} category=OTHER인데 custom label 없음"))
        if k.get('category')!='OTHER' and k.get('custom_category_label'):
            issues.append(('ENUM', f"{k['knowledge_code']} category!=OTHER인데 custom label 있음"))
        if k.get('fact_type')=='OTHER' and not k.get('custom_fact_type_label'):
            issues.append(('ENUM', f"{k['knowledge_code']} fact_type=OTHER인데 custom label 없음"))
        if k.get('traversal_method')=='OTHER' and not k.get('custom_traversal_method'):
            issues.append(('ENUM', f"{k['knowledge_code']} traversal=OTHER인데 custom 없음"))

    # --- usage_scope vs action_text
    for k in ks:
        us, at = k.get('usage_scope'), k.get('action_text')
        if us=='WARNING_ONLY' and at:
            issues.append(('SCOPE', f"{k['knowledge_code']} WARNING_ONLY인데 action_text 있음: \"{str(at)[:30]}\""))
        if us in ('ACTION_GUIDANCE','ROUTE_GUIDANCE') and not at:
            issues.append(('SCOPE', f"{k['knowledge_code']} {us}인데 action_text 없음"))

    # --- enum whitelist
    ALLOW = {
      'category': {'ACCESS','PARKING_STOPPING','LOADING','BUILDING_ENTRANCE','INTERNAL_ROUTE','ELEVATOR_STAIRS','CONGESTION_WAIT','DELIVERY_POINT','OTHER'},
      'fact_type': {'RESTRICTION','ALLOWANCE','LOCATION','INSTRUCTION','WARNING','CONDITION','OTHER'},
      'movement_mode': {'VEHICLE','PEDESTRIAN','GENERAL'},
      'traversal_method': {'DRIVE','WALK','STAIRS','ELEVATOR','ESCALATOR','CART','OTHER',None},
      'access_state': {'ALLOWED','CONDITIONAL','PROHIBITED','UNKNOWN',None},
      'usage_scope': {'WARNING_ONLY','ACTION_GUIDANCE','ROUTE_GUIDANCE','REFERENCE_ONLY'},
    }
    for k in ks:
        for f,allowed in ALLOW.items():
            v = k.get(f)
            if v not in allowed:
                issues.append(('ENUM', f"{k['knowledge_code']}.{f} = {v!r} (허용목록 밖)"))

    # --- conditions numbers must appear in transcript
    for r in reports:
        tr = r.get('transcript','')
        for k in r.get('expected_knowledge_items',[]):
            c = k.get('conditions') or {}
            for f in ('min_tonnage','max_tonnage','max_vehicle_height_m','max_vehicle_width_m'):
                v = c.get(f)
                if v is None: continue
                s1 = str(v); s2 = str(int(v)) if float(v)==int(v) else s1
                if s1 not in tr and s2 not in tr:
                    issues.append(('NUM', f"{k['knowledge_code']}.{f}={v} 이 transcript에 없음"))

    # --- RAG query refs
    all_k = set(k_codes)
    for q in queries:
        if q.get('place_code') != place_code:
            issues.append(('RAG', f"{q['query_code']}.place={q.get('place_code')}"))
        for f in ('expected_knowledge_codes','must_not_return_codes'):
            for c in q.get(f) or []:
                if c not in all_k:
                    issues.append(('RAG', f"{q['query_code']}.{f} -> 없는 knowledge {c}"))
        if not q.get('expected_knowledge_codes'):
            issues.append(('RAG', f"{q['query_code']} expected 비어있음"))

    # --- knowledge attached to route segments? (guidance 연결성)
    seg_targets = collections.Counter()
    for k in ks:
        t=k.get('target',{})
        if t.get('target_type')=='SEGMENT': seg_targets[t.get('target_code')]+=1
    node_targets = collections.Counter()
    for k in ks:
        t=k.get('target',{})
        if t.get('target_type')=='NODE': node_targets[t.get('target_code')]+=1

    # route coverage: for each route, how many knowledge reachable via its nodes/segments
    print('\n--- Guidance 연결성 (Route 단계별로 붙을 지식 수) ---')
    for r in routes:
        rsegs = sorted([s for s in segs if s.get('route_code')==r['route_code']], key=lambda x:x.get('sequence_no',0))
        tot=0
        line=[]
        for s in rsegs:
            cnt = seg_targets.get(s['segment_code'],0) + node_targets.get(s.get('to_node_code'),0) + node_targets.get(s.get('from_node_code'),0)
            tot+=cnt
            line.append(f"  seq{s.get('sequence_no')} {s['segment_code']}: {cnt}")
        print(f"  {r['route_code']} ({len(rsegs)} segs) 총 {tot}")
        for l in line: print('  '+l)

    orphan = [k['knowledge_code'] for k in ks
              if k['target'].get('target_type')=='NODE'
              and k['target'].get('target_code') not in
                  {v for s in segs for v in (s.get('from_node_code'),s.get('to_node_code'))}]
    print(f"\n  Route에 안 걸리는 NODE 지식: {len(orphan)}개 {orphan[:12]}")
    unres = [k['knowledge_code'] for k in ks if k['target'].get('target_type')=='UNKNOWN']
    print(f"  UNRESOLVED 지식        : {len(unres)}개 (segment 가산점 못 받음)")

    # --- report
    print('\n--- 정합성 이슈 ---')
    if not issues:
        print('  이슈 없음')
    else:
        by = collections.defaultdict(list)
        for cat,msg in issues: by[cat].append(msg)
        for cat in sorted(by):
            print(f'  [{cat}] {len(by[cat])}건')
            for m in by[cat][:14]: print(f'     - {m}')
            if len(by[cat])>14: print(f'     ... 외 {len(by[cat])-14}건')
            grand[cat]+=len(by[cat])
    print()

print('='*72)
print('전체 이슈 합계:', dict(grand), ' 총', sum(grand.values()), '건')
