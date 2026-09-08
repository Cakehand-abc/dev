"""Export only this task's synthetic business data; never export account secrets."""
import os,subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
cnf=Path(os.environ['TEMP'])/'smart-care-mysql-01a07b9c/client.cnf'
tables=['region','elder','doctor','health_rule','health_record','followup_plan','followup_record','watch_device','device_binding','location_point','geofence','geofence_member','geofence_alert','geofence_state']
args=['D:/Mysql/bin/mysqldump.exe','--defaults-extra-file='+str(cnf),'--no-create-info','--skip-add-locks','--skip-disable-keys','--skip-comments','--complete-insert','--skip-extended-insert','--set-gtid-purged=OFF','--column-statistics=0','--default-character-set=utf8mb4','smart_care']+tables
r=subprocess.run(args,capture_output=True,check=True)
inserts=[line for line in r.stdout.decode('utf-8').splitlines() if line.startswith('INSERT INTO')]
assert len(inserts)>1000
checks=' + '.join('(SELECT COUNT(*) FROM '+t+')' for t in tables)
text='''-- Synthetic course data only. Exported 2026-09-08 from the isolated demo.
-- Dates are fixed near 2026-09-07. Prefer Java DemoSeeder for fresh relative dates.
-- Run AFTER 00-create-database.sql and 01-schema.sql in a new dedicated empty database.
-- Do not combine with Java DemoSeeder. No sys_user/password hashes/audit records exported.
-- Bootstrap creates admin from APP_ADMIN_PASSWORD afterwards; create other roles in UI.
-- The temporary procedure fails if ANY business table is nonempty. No existing rows are deleted.
USE smart_care;
SET NAMES utf8mb4;
DELIMITER $$
CREATE PROCEDURE seed_smart_care_demo_v1()
BEGIN
 DECLARE EXIT HANDLER FOR SQLEXCEPTION
 BEGIN
  ROLLBACK;
  RESIGNAL;
 END;
 IF ('''+checks+''') <> 0 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Demo seed requires empty business tables';
 END IF;
 START TRANSACTION;
'''
for t in tables:text+='\n-- '+t+'\n'+'\n'.join(' '+s for s in inserts if s.startswith('INSERT INTO `'+t+'`'))+'\n'
text+=' COMMIT;\nEND$$\nDELIMITER ;\nCALL seed_smart_care_demo_v1();\nDROP PROCEDURE seed_smart_care_demo_v1;\n'
(root/'database/02-demo-data.sql').write_text(text,encoding='utf-8')
print('Exported synthetic rows:',len(inserts),'No account table or credentials included')
