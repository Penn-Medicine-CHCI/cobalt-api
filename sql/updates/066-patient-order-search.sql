BEGIN;
SELECT _v.register_patch('066-patient-order-search', NULL, NULL);

-- These trigram indices permit efficient autocomplete-like searching, e.g.
--
--   SELECT *
--   FROM patient_order
--   WHERE patient_first_name ILIKE '%mike%';

CREATE INDEX patient_order_patient_first_name_trgm_idx
ON patient_order
USING GIN (patient_first_name gin_trgm_ops);

CREATE INDEX patient_order_patient_last_name_trgm_idx
ON patient_order
USING GIN (patient_last_name gin_trgm_ops);

CREATE INDEX patient_order_patient_mrn_trgm_idx
ON patient_order
USING GIN (patient_mrn gin_trgm_ops);

ALTER TABLE screening_session ALTER COLUMN target_account_id DROP NOT NULL;

ALTER TABLE patient_order_care_type ADD COLUMN severity INTEGER;

UPDATE patient_order_care_type SET severity = -1 WHERE patient_order_care_type_id = 'UNSPECIFIED';
UPDATE patient_order_care_type SET severity = 0 WHERE patient_order_care_type_id = 'SUBCLINICAL';
UPDATE patient_order_care_type SET severity = 1 WHERE patient_order_care_type_id = 'COLLABORATIVE';
UPDATE patient_order_care_type SET severity = 2 WHERE patient_order_care_type_id = 'SPECIALTY';
UPDATE patient_order_care_type SET severity = 3 WHERE patient_order_care_type_id = 'SAFETY_PLANNING';

ALTER TABLE patient_order_care_type ALTER COLUMN severity SET NOT NULL;

CREATE VIEW v_patient_order AS
WITH
po_query AS (
  select *
  from patient_order
),
poo_query AS (
  -- Count up the patient outreach attempts for each patient order
  select poq.patient_order_id, count(poo.*) as outreach_count
  from patient_order_outreach poo, po_query poq
  where poq.patient_order_id=poo.patient_order_id
  group by poq.patient_order_id
),
poomax_query AS (
  -- Pick the most recent patient outreach attempt for each patient order
  select poo.*
  from po_query poq
  join patient_order_outreach poo on poq.patient_order_id=poo.patient_order_id
  left join patient_order_outreach poo2 on poo.patient_order_id = poo2.patient_order_id and poo.outreach_date_time < poo2.outreach_date_time
  where poo2.patient_order_outreach_id is null
),
ss_query as (
  -- Pick the most recently-created screening session for the patient order
  select ss.*, a.first_name, a.last_name
  from po_query poq
  join screening_session ss on poq.patient_order_id=ss.patient_order_id
  join account a on ss.created_by_account_id=a.account_id
  left join screening_session ss2 on ss.patient_order_id = ss2.patient_order_id and ss.created < ss2.created
  where ss2.screening_session_id is null
),
triage_query as (
  -- Pick the most-severe triage for each patient order.
  -- Use a window function because it's easier to handle the join needed to order by severity
	WITH poct_cte AS (
	   SELECT poq.patient_order_id, poct.patient_order_care_type_id, poct.description as patient_order_care_type_description, pot.patient_order_triage_id,
	            RANK() OVER (PARTITION BY poq.patient_order_id
	            ORDER BY poct.severity DESC
	            ) AS r
	      from po_query poq, patient_order_triage pot, patient_order_care_type poct
	      where poq.patient_order_id=pot.patient_order_id
		  and pot.patient_order_care_type_id=poct.patient_order_care_type_id
		  and pot.active=true
	)
	SELECT patient_order_care_type_id, patient_order_care_type_description, patient_order_id
	FROM poct_cte
	WHERE r = 1
)
-- We need the DISTINCT here because patient outreach attempts with identical "most recent" times will cause duplicate rows
select distinct
  tq.patient_order_care_type_id,
  tq.patient_order_care_type_description,
	coalesce(pooq.outreach_count, 0) as outreach_count,
	poomaxq.outreach_date_time as most_recent_outreach_date_time,
	ssq.screening_session_id as most_recent_screening_session_id,
	ssq.created_by_account_id as most_recent_screening_session_created_by_account_id,
	ssq.first_name as most_recent_screening_session_created_by_account_first_name,
	ssq.last_name as most_recent_screening_session_created_by_account_last_name,
	ssq.completed as most_recent_screening_session_completed,
	ssq.completed_at as most_recent_screening_session_completed_at,
	panel_account.first_name as panel_account_first_name,
	panel_account.last_name as panel_account_last_name,
	poq.*
from po_query poq
left outer join poo_query pooq ON poq.patient_order_id = pooq.patient_order_id
left outer join poomax_query poomaxq ON poq.patient_order_id = poomaxq.patient_order_id
left outer join ss_query ssq ON poq.patient_order_id = ssq.patient_order_id
left outer join triage_query tq ON poq.patient_order_id = tq.patient_order_id
left outer join account panel_account ON poq.panel_account_id = panel_account.account_id;

COMMIT;