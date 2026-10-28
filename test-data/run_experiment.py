import json, os, sys, time, urllib.request

tok = sys.argv[1]
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'run_experiment_result.json')
req = urllib.request.Request('http://localhost:8001/api/admin/evaluation/run-experiment',
    data=b'{}', headers={'Content-Type': 'application/json', 'token': tok}, method='POST')
t0 = time.time()
status = {'state': 'running', 'startedAt': time.strftime('%H:%M:%S')}
json.dump(status, open(out, 'w', encoding='utf-8'), ensure_ascii=False)
try:
    with urllib.request.urlopen(req, timeout=2400) as r:
        body = json.loads(r.read().decode())
    body['elapsed'] = round(time.time() - t0, 1)
    body['state'] = 'done'
    json.dump(body, open(out, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    print('DONE', body.get('code'))
except Exception as e:
    status['state'] = 'error'
    status['error'] = f'{type(e).__name__}: {e}'
    status['elapsed'] = round(time.time() - t0, 1)
    json.dump(status, open(out, 'w', encoding='utf-8'), ensure_ascii=False)
    print('ERROR', e)
