"""Local integration smoke test: requires running backend + MySQL and an admin account.
Usage: python scripts/smoke_test.py --admin admin@example.test --password '...' 
Creates one test employee and one test leave, confirms backend search/filter and approval, and uploads a sample PDF.
"""
import argparse
import http.cookiejar
import json
import random
import string
import urllib.error
import urllib.parse
import urllib.request

parser=argparse.ArgumentParser()
parser.add_argument('--base',default='http://localhost:8080')
parser.add_argument('--admin',required=True)
parser.add_argument('--password',required=True)
args=parser.parse_args()

def client():
    jar=http.cookiejar.CookieJar()
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))

def call(opener,method,path,data=None,expected=(200,),file=None):
    headers={}
    if method!='GET':
        csrf=json.loads(opener.open(args.base+'/api/auth/csrf').read().decode())
        headers['X-XSRF-TOKEN']=csrf['token']
    if file is not None:
        boundary='----LEAVEHUBSMOKE12345'
        data=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="medical-test.pdf"\r\n'
              'Content-Type: application/pdf\r\n\r\n').encode()+file+f'\r\n--{boundary}--\r\n'.encode()
        headers['Content-Type']='multipart/form-data; boundary='+boundary
    elif data is not None:
        data=json.dumps(data).encode()
        headers['Content-Type']='application/json'
    request=urllib.request.Request(args.base+path,data=data,headers=headers,method=method)
    try:
        with opener.open(request) as response:
            assert response.status in expected,(response.status,response.read())
            body=response.read()
            return json.loads(body) if body else None
    except urllib.error.HTTPError as e:
        if e.code in expected:return {'status':e.code}
        raise AssertionError(f'{method} {path} failed HTTP {e.code}: {e.read().decode()}') from e

admin=client()
call(admin,'POST','/api/auth/login',{'email':args.admin,'password':args.password})
print('✓ Admin authenticated')
depts=call(admin,'GET','/api/departments')
assert depts
suffix=''.join(random.choices(string.ascii_lowercase+string.digits,k=9))
email=f'smoke-{suffix}@example.test'
employee=call(admin,'POST','/api/employees',{'fullName':'Smoke Test Person','email':email,'gender':'OTHER',
    'departmentId':depts[0]['id'],'password':'TempPass987!'},expected=(201,))
users=call(admin,'GET',f'/api/employees?search=Smoke&gender=OTHER&departmentId={depts[0]["id"]}&active=true')
assert any(u['id']==employee['id'] for u in users['content'])
print('✓ Employee creation and combined filters')
worker=client()
call(worker,'POST','/api/auth/login',{'email':email,'password':'TempPass987!'})
leave=call(worker,'POST','/api/leaves',{'leaveType':'MEDICAL','startDate':'2027-01-10','endDate':'2027-01-11','reason':'Clinical appointment documentation'},expected=(201,))
assert leave['status']=='PENDING'
print('✓ Leave submission')
pdf=b'%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\n%%EOF'
attachment=call(worker,'POST',f'/api/leaves/{leave["id"]}/attachments',file=pdf,expected=(201,))
assert attachment['originalName']=='medical-test.pdf'
print('✓ Authenticated medical-document upload')
results=call(admin,'GET',f'/api/leaves?search=Clinical&status=PENDING&leaveType=MEDICAL&gender=OTHER&employeeId={employee["id"]}&startFrom=2027-01-01&endTo=2027-01-31')
assert any(l['id']==leave['id'] for l in results['content'])
print('✓ Combined server-side leave filters')
call(worker,'PATCH',f'/api/leaves/{leave["id"]}/approve',{},expected=(403,))
print('✓ Employee cannot approve leave')
approved=call(admin,'PATCH',f'/api/leaves/{leave["id"]}/approve',{})
assert approved['status']=='APPROVED'
call(admin,'PATCH',f'/api/leaves/{leave["id"]}/approve',{},expected=(409,))
print('✓ Approve once; invalid status transitions rejected')
print('Smoke test finished; one test employee + leave were created in the database.')
