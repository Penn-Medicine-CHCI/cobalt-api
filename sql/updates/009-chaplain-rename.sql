BEGIN;
SELECT _v.register_patch('009-chaplain-rename', NULL, NULL);

update support_role set description='Spiritual and Emotional Support' where support_role_id='CHAPLAIN';

END;