BEGIN;
SELECT _v.register_patch('239-patient-order-scheduling-dept-override.sql', NULL, NULL);

INSERT INTO patient_order_scheduled_message_type VALUES ('APPOINTMENT_BOOKING_REQUEST', 'IC_APPOINTMENT_BOOKING_REQUEST', 'Appointment Booking Request', 4);
UPDATE patient_order_scheduled_message_type SET display_order=5 WHERE patient_order_scheduled_message_type_id='RESOURCE_CHECK_IN';

-- Ability to override scheduling department
ALTER TABLE patient_order ADD COLUMN override_scheduling_epic_department_id UUID REFERENCES epic_department(epic_department_id);

-- Add footprint enum value for override updates
INSERT INTO footprint_event_group_type VALUES ('PATIENT_ORDER_UPDATE_OVERRIDE_SCHEDULING_EPIC_DEPARTMENT', 'Patient Order Update Override Scheduling Epic Department');

-- Recreate `v_all_patient_order` and `v_patient_order` to add new `override_scheduling_epic_department_id` column
DROP VIEW v_patient_order;
DROP VIEW v_all_patient_order;

CREATE VIEW v_all_patient_order AS
 WITH poo AS (
         SELECT patient_order_outreach.patient_order_id,
            count(*) AS outreach_count,
            max(patient_order_outreach.outreach_date_time) AS max_outreach_date_time
           FROM patient_order_outreach
          WHERE patient_order_outreach.deleted = false
          GROUP BY patient_order_outreach.patient_order_id
        ), reason_for_referral_query AS (
         SELECT por.patient_order_id,
            string_agg(porr.description::text, ', '::text ORDER BY por.display_order) AS reason_for_referral
           FROM patient_order_referral por
             JOIN patient_order_referral_reason porr ON por.patient_order_referral_reason_id::text = porr.patient_order_referral_reason_id::text
          GROUP BY por.patient_order_id
        ), smg AS (
         SELECT posmg_1.patient_order_id,
            count(*) AS scheduled_message_group_delivered_count,
            max(posmg_1.scheduled_at_date_time) AS max_delivered_scheduled_message_group_date_time
           FROM patient_order_scheduled_message_group posmg_1
          WHERE posmg_1.deleted = false AND (EXISTS ( SELECT 1
                   FROM patient_order_scheduled_message posm
                     JOIN scheduled_message sm ON sm.scheduled_message_id = posm.scheduled_message_id
                     JOIN message_log ml ON ml.message_id = sm.message_id AND ml.message_status_id = 'DELIVERED'::text
                  WHERE posm.patient_order_scheduled_message_group_id = posmg_1.patient_order_scheduled_message_group_id
                 LIMIT 1))
          GROUP BY posmg_1.patient_order_id
        ), next_resource_check_in_scheduled_message_group_query AS (
         SELECT DISTINCT ON (posmg_1.patient_order_id) posmg_1.patient_order_id,
            posmg_1.patient_order_scheduled_message_group_id AS next_resource_check_in_scheduled_message_group_id,
            posmg_1.scheduled_at_date_time AS next_resource_check_in_scheduled_at_date_time
           FROM patient_order_scheduled_message_group posmg_1
             JOIN patient_order po ON posmg_1.patient_order_id = po.patient_order_id
             JOIN institution i_1 ON po.institution_id::text = i_1.institution_id::text
             LEFT JOIN patient_order_scheduled_message posm ON posmg_1.patient_order_scheduled_message_group_id = posm.patient_order_scheduled_message_group_id
             LEFT JOIN scheduled_message sm ON posm.scheduled_message_id = sm.scheduled_message_id
             LEFT JOIN message_log ml ON sm.message_id = ml.message_id
          WHERE posmg_1.patient_order_scheduled_message_type_id::text = 'RESOURCE_CHECK_IN'::text AND posmg_1.deleted = false AND (posmg_1.scheduled_at_date_time AT TIME ZONE i_1.time_zone) > now() AND (ml.message_status_id IS NULL OR ml.message_status_id <> 'DELIVERED'::text)
          ORDER BY posmg_1.patient_order_id, posmg_1.scheduled_at_date_time, posmg_1.patient_order_scheduled_message_group_id
        ), next_appt_query AS (
         SELECT DISTINCT ON (app.patient_order_id) app.patient_order_id,
            app.appointment_id,
            app.canceled,
            p.provider_id,
            p.name AS provider_name,
            app.start_time AS appointment_start_time,
            app.created_by_account_id
           FROM appointment app
             JOIN provider p ON app.provider_id = p.provider_id
          WHERE app.canceled = false
          ORDER BY app.patient_order_id, app.start_time, app.appointment_id
        ), recent_voicemail_task_query AS (
         SELECT DISTINCT ON (povt.patient_order_id) povt.patient_order_id,
            povt.patient_order_voicemail_task_id,
            povt.completed AS patient_order_voicemail_task_completed
           FROM patient_order_voicemail_task povt
          WHERE povt.deleted = false
          ORDER BY povt.patient_order_id, povt.created DESC, povt.patient_order_voicemail_task_id
        ), next_scheduled_outreach_query AS (
         SELECT DISTINCT ON (poso.patient_order_id) poso.patient_order_id,
            poso.patient_order_scheduled_outreach_id AS next_scheduled_outreach_id,
            poso.scheduled_at_date_time AS next_scheduled_outreach_scheduled_at_date_time,
            poso.patient_order_outreach_type_id AS next_scheduled_outreach_type_id,
            poso.patient_order_scheduled_outreach_reason_id AS next_scheduled_outreach_reason_id
           FROM patient_order_scheduled_outreach poso
          WHERE poso.patient_order_scheduled_outreach_status_id::text = 'SCHEDULED'::text
          ORDER BY poso.patient_order_id, poso.scheduled_at_date_time, poso.patient_order_scheduled_outreach_id
        ), most_recent_message_delivered_query AS (
         SELECT DISTINCT ON (posmg_1.patient_order_id) posmg_1.patient_order_id,
            ml.delivered AS most_recent_message_delivered_at
           FROM patient_order_scheduled_message_group posmg_1
             JOIN patient_order_scheduled_message posm ON posmg_1.patient_order_scheduled_message_group_id = posm.patient_order_scheduled_message_group_id
             JOIN scheduled_message sm ON posm.scheduled_message_id = sm.scheduled_message_id
             JOIN message_log ml ON sm.message_id = ml.message_id
          WHERE ml.message_status_id = 'DELIVERED'::text
          ORDER BY posmg_1.patient_order_id, ml.delivered DESC
        ), ss_query AS (
         SELECT DISTINCT ON (ss.patient_order_id) ss.screening_session_id,
            ss.screening_flow_version_id,
            ss.target_account_id,
            ss.created_by_account_id,
            ss.completed,
            ss.crisis_indicated,
            ss.created,
            ss.last_updated,
            ss.skipped,
            ss.skipped_at,
            ss.completed_at,
            ss.crisis_indicated_at,
            ss.patient_order_id,
            ss.group_session_id,
            ss.account_check_in_action_id,
            ss.metadata,
            a.first_name,
            a.last_name,
            a.role_id
           FROM screening_session ss
             JOIN screening_flow_version sfv ON ss.screening_flow_version_id = sfv.screening_flow_version_id
             JOIN institution i_1 ON sfv.screening_flow_id = i_1.integrated_care_screening_flow_id
             JOIN account a ON ss.created_by_account_id = a.account_id
          WHERE i_1.institution_id::text = a.institution_id::text AND ss.skipped = false
          ORDER BY ss.patient_order_id, ss.created DESC
        ), ss_intake_query AS (
         SELECT DISTINCT ON (ss.patient_order_id) ss.screening_session_id,
            ss.screening_flow_version_id,
            ss.target_account_id,
            ss.created_by_account_id,
            ss.completed,
            ss.crisis_indicated,
            ss.created,
            ss.last_updated,
            ss.skipped,
            ss.skipped_at,
            ss.completed_at,
            ss.crisis_indicated_at,
            ss.patient_order_id,
            ss.group_session_id,
            ss.account_check_in_action_id,
            ss.metadata,
            a.first_name,
            a.last_name,
            a.role_id
           FROM screening_session ss
             JOIN screening_flow_version sfv ON ss.screening_flow_version_id = sfv.screening_flow_version_id
             JOIN institution i_1 ON sfv.screening_flow_id = i_1.integrated_care_intake_screening_flow_id
             JOIN account a ON ss.created_by_account_id = a.account_id
          WHERE i_1.institution_id::text = a.institution_id::text AND ss.skipped = false
          ORDER BY ss.patient_order_id, ss.created DESC
        ), permitted_regions_query AS (
         SELECT institution_integrated_care_region.institution_id,
            array_agg(institution_integrated_care_region.region_abbreviation) AS permitted_region_abbreviations
           FROM institution_integrated_care_region
          GROUP BY institution_integrated_care_region.institution_id
        ), recent_scheduled_screening_query AS (
         SELECT DISTINCT ON (poss.patient_order_id) poss.patient_order_scheduled_screening_id,
            poss.patient_order_id,
            poss.account_id,
            poss.scheduled_date_time,
            poss.calendar_url,
            poss.canceled,
            poss.canceled_at,
            poss.created,
            poss.last_updated
           FROM patient_order_scheduled_screening poss
          WHERE poss.canceled = false
          ORDER BY poss.patient_order_id, poss.scheduled_date_time
        ), recent_po_query AS (
         SELECT poq_1.patient_order_id,
            lag(poq_1.episode_closed_at) OVER (PARTITION BY poq_1.patient_mrn, poq_1.institution_id ORDER BY poq_1.order_date) AS most_recent_episode_closed_at
           FROM patient_order poq_1
        )
 SELECT potg.patient_order_care_type_id,
    poct.description AS patient_order_care_type_description,
    potg.patient_order_triage_source_id,
    COALESCE(poo.outreach_count, 0::bigint) AS outreach_count,
    poo.max_outreach_date_time AS most_recent_outreach_date_time,
    COALESCE(smg.scheduled_message_group_delivered_count, 0::bigint) AS scheduled_message_group_delivered_count,
    smg.max_delivered_scheduled_message_group_date_time AS most_recent_delivered_scheduled_message_group_date_time,
    COALESCE(poo.outreach_count, 0::bigint) + COALESCE(smg.scheduled_message_group_delivered_count, 0::bigint) AS total_outreach_count,
    GREATEST(poo.max_outreach_date_time, smg.max_delivered_scheduled_message_group_date_time) AS most_recent_total_outreach_date_time,
    ssq.screening_session_id AS most_recent_screening_session_id,
    ssq.created AS most_recent_screening_session_created_at,
    ssq.created_by_account_id AS most_recent_screening_session_created_by_account_id,
    ssq.role_id AS most_recent_screening_session_created_by_account_role_id,
    ssq.first_name AS most_recent_screening_session_created_by_account_first_name,
    ssq.last_name AS most_recent_screening_session_created_by_account_last_name,
    ssq.completed AS most_recent_screening_session_completed,
    ssq.completed_at AS most_recent_screening_session_completed_at,
        CASE
            WHEN ssq.completed = true THEN 'COMPLETE'::text
            WHEN ssq.screening_session_id IS NOT NULL THEN 'IN_PROGRESS'::text
            WHEN rssq.scheduled_date_time IS NOT NULL THEN 'SCHEDULED'::text
            ELSE 'NOT_SCREENED'::text
        END AS patient_order_screening_status_id,
        CASE
            WHEN ssq.completed = true THEN 'Complete'::text
            WHEN ssq.screening_session_id IS NOT NULL THEN 'In Progress'::text
            WHEN rssq.scheduled_date_time IS NOT NULL THEN 'Scheduled'::text
            ELSE 'Not Screened'::text
        END AS patient_order_screening_status_description,
        CASE
            WHEN poq.patient_account_id = ssq.created_by_account_id THEN true
            ELSE false
        END AS most_recent_screening_session_by_patient,
    ssq.screening_session_id IS NOT NULL AND ssq.completed = false AND ssq.created < (now() - '01:00:00'::interval) AS most_recent_screening_session_appears_abandoned,
        CASE
            WHEN ssq.completed = true AND poq.encounter_synced_at IS NULL THEN 'NEEDS_DOCUMENTATION'::text
            WHEN ssq.completed = true AND poq.encounter_synced_at IS NOT NULL THEN 'DOCUMENTED'::text
            ELSE 'NOT_DOCUMENTED'::text
        END AS patient_order_encounter_documentation_status_id,
    ssiq.screening_session_id AS most_recent_intake_screening_session_id,
    ssiq.created AS most_recent_intake_screening_session_created_at,
    ssiq.created_by_account_id AS most_recent_intake_screening_session_created_by_account_id,
    ssiq.role_id AS most_recent_intake_screening_session_created_by_account_role_id,
    ssiq.first_name AS most_recent_intake_screening_session_created_by_account_fn,
    ssiq.last_name AS most_recent_intake_screening_session_created_by_account_ln,
    ssiq.completed AS most_recent_intake_screening_session_completed,
    ssiq.completed_at AS most_recent_intake_screening_session_completed_at,
        CASE
            WHEN ssiq.completed = true THEN 'COMPLETE'::text
            WHEN ssiq.screening_session_id IS NOT NULL THEN 'IN_PROGRESS'::text
            ELSE 'NOT_SCREENED'::text
        END AS patient_order_intake_screening_status_id,
        CASE
            WHEN ssiq.completed = true THEN 'Complete'::text
            WHEN ssiq.screening_session_id IS NOT NULL THEN 'In Progress'::text
            ELSE 'Not Screened'::text
        END AS patient_order_intake_screening_status_description,
        CASE
            WHEN poq.patient_account_id = ssiq.created_by_account_id THEN true
            ELSE false
        END AS most_recent_intake_screening_session_by_patient,
    ssiq.screening_session_id IS NOT NULL AND ssiq.completed = false AND ssiq.created < (now() - '01:00:00'::interval) AS most_recent_intake_screening_session_appears_abandoned,
    ssiq.screening_session_id IS NOT NULL AND ssiq.completed = true AND (ssq.screening_session_id IS NOT NULL AND ssq.completed = true OR ssq.screening_session_id IS NULL OR ssq.screening_session_id IS NOT NULL AND ssq.completed = false AND ssiq.created > ssq.created) AS most_recent_intake_and_clinical_screenings_satisfied,
    panel_account.first_name AS panel_account_first_name,
    panel_account.last_name AS panel_account_last_name,
    pod.description AS patient_order_disposition_description,
        CASE
            WHEN potg.patient_order_care_type_id = 'SPECIALTY'::text THEN 'SPECIALTY_CARE'::text
            WHEN potg.patient_order_care_type_id = 'SUBCLINICAL'::text THEN 'SUBCLINICAL'::text
            WHEN potg.patient_order_care_type_id = 'COLLABORATIVE'::text THEN 'MHP'::text
            ELSE 'NOT_TRIAGED'::text
        END AS patient_order_triage_status_id,
        CASE
            WHEN potg.patient_order_care_type_id = 'SPECIALTY'::text THEN 'Specialty Care'::text
            WHEN potg.patient_order_care_type_id = 'SUBCLINICAL'::text THEN 'Subclinical'::text
            WHEN potg.patient_order_care_type_id = 'COLLABORATIVE'::text THEN 'MHP'::text
            ELSE 'Not Triaged'::text
        END AS patient_order_triage_status_description,
    pocr.description AS patient_order_closure_reason_description,
    date_part('year'::text, age(poq.order_date::timestamp with time zone, poq.patient_birthdate::timestamp with time zone))::integer AS patient_age_on_order_date,
    date_part('year'::text, age(poq.order_date::timestamp with time zone, poq.patient_birthdate::timestamp with time zone))::integer < 18 AS patient_below_age_threshold,
    rpq.most_recent_episode_closed_at,
    date_part('day'::text, now() - rpq.most_recent_episode_closed_at)::integer < 30 AS most_recent_episode_closed_within_date_threshold,
    rssq.patient_order_scheduled_screening_id,
    rssq.scheduled_date_time AS patient_order_scheduled_screening_scheduled_date_time,
    rssq.calendar_url AS patient_order_scheduled_screening_calendar_url,
    poq.patient_order_disposition_id::text = 'OPEN'::text AND (poq.patient_order_intake_wants_services_status_id::text = 'NO'::text OR poq.patient_order_intake_location_status_id::text = 'INVALID'::text OR (poq.patient_order_intake_insurance_status_id::text = ANY (ARRAY['INVALID'::character varying, 'CHANGED_RECENTLY'::character varying]::text[]))) OR poq.patient_order_disposition_id::text = 'OPEN'::text AND ssq.screening_session_id IS NULL AND rssq.scheduled_date_time IS NULL AND (COALESCE(poo.outreach_count, 0::bigint) + COALESCE(smg.scheduled_message_group_delivered_count, 0::bigint)) > 0 AND ((GREATEST(poo.max_outreach_date_time, smg.max_delivered_scheduled_message_group_date_time) + make_interval(days => i.integrated_care_outreach_followup_day_offset)) AT TIME ZONE i.time_zone) <= now() AS outreach_followup_needed,
    naq.appointment_start_time,
    naq.provider_id,
    naq.provider_name,
    naq.appointment_id,
        CASE
            WHEN naq.appointment_id IS NOT NULL THEN true
            ELSE false
        END AS appointment_scheduled,
        CASE
            WHEN naq.created_by_account_id = poq.patient_account_id THEN true
            ELSE false
        END AS appointment_scheduled_by_patient,
    rvtq.patient_order_voicemail_task_id AS most_recent_patient_order_voicemail_task_id,
    rvtq.patient_order_voicemail_task_completed AS most_recent_patient_order_voicemail_task_completed,
    rfrq.reason_for_referral,
    patient_address.street_address_1 AS patient_address_street_address_1,
    patient_address.locality AS patient_address_locality,
    patient_address.region AS patient_address_region,
    patient_address.postal_code AS patient_address_postal_code,
    patient_address.country_code AS patient_address_country_code,
    patient_address.region = ANY (prq.permitted_region_abbreviations::text[]) AS patient_address_region_accepted,
    poq.patient_first_name IS NOT NULL AND poq.patient_last_name IS NOT NULL AND poq.patient_phone_number IS NOT NULL AND poq.patient_email_address IS NOT NULL AND poq.patient_birthdate IS NOT NULL AND patient_address.street_address_1 IS NOT NULL AND patient_address.locality IS NOT NULL AND patient_address.region IS NOT NULL AND patient_address.postal_code IS NOT NULL AS patient_demographics_completed,
    poq.patient_first_name IS NOT NULL AND poq.patient_last_name IS NOT NULL AND poq.patient_phone_number IS NOT NULL AND poq.patient_email_address IS NOT NULL AND poq.patient_birthdate IS NOT NULL AND patient_address.street_address_1 IS NOT NULL AND patient_address.locality IS NOT NULL AND (patient_address.region = ANY (prq.permitted_region_abbreviations::text[])) AND patient_address.postal_code IS NOT NULL AS patient_demographics_accepted,
    posmg.scheduled_at_date_time AS resource_check_in_scheduled_at_date_time,
    poq.patient_order_resource_check_in_response_status_id = 'NONE'::text AND posmg.scheduled_at_date_time IS NOT NULL AND (posmg.scheduled_at_date_time AT TIME ZONE i.time_zone) < now() AS resource_check_in_response_needed,
    porcirs.description AS patient_order_resource_check_in_response_status_description,
    poq.patient_demographics_confirmed_at IS NOT NULL AS patient_demographics_confirmed,
    date_part('day'::text, COALESCE(poq.episode_closed_at, now()) - (poq.order_date + make_interval(mins => poq.order_age_in_minutes))::timestamp with time zone) AS episode_duration_in_days,
    ed.name AS epic_department_name,
    ed.department_id AS epic_department_department_id,
    mrmdq.most_recent_message_delivered_at,
    nsoq.next_scheduled_outreach_id,
    nsoq.next_scheduled_outreach_scheduled_at_date_time,
    nsoq.next_scheduled_outreach_type_id,
    nsoq.next_scheduled_outreach_reason_id,
    GREATEST(mrmdq.most_recent_message_delivered_at,
        CASE
            WHEN (poo.max_outreach_date_time AT TIME ZONE i.time_zone) < now() THEN (poo.max_outreach_date_time AT TIME ZONE i.time_zone)
            ELSE NULL::timestamp with time zone
        END,
        CASE
            WHEN ssq.screening_session_id IS NOT NULL AND (ssq.target_account_id IS NULL OR ssq.target_account_id <> ssq.created_by_account_id) THEN ssq.created
            ELSE NULL::timestamp with time zone
        END) AS last_contacted_at,
        CASE
            WHEN ssq.screening_session_id IS NULL AND rssq.scheduled_date_time = LEAST(rssq.scheduled_date_time, nsoq.next_scheduled_outreach_scheduled_at_date_time) THEN 'ASSESSMENT'::text
            WHEN nsoq.next_scheduled_outreach_scheduled_at_date_time = LEAST(
            CASE
                WHEN ssq.screening_session_id IS NULL THEN rssq.scheduled_date_time::timestamp with time zone
                ELSE '9999-12-31 23:59:59+00'::timestamp with time zone
            END, nsoq.next_scheduled_outreach_scheduled_at_date_time::timestamp with time zone) AND nsoq.next_scheduled_outreach_reason_id::text = 'RESOURCE_FOLLOWUP'::text THEN 'RESOURCE_FOLLOWUP'::text
            WHEN nsoq.next_scheduled_outreach_scheduled_at_date_time = LEAST(
            CASE
                WHEN ssq.screening_session_id IS NULL THEN rssq.scheduled_date_time::timestamp with time zone
                ELSE '9999-12-31 23:59:59+00'::timestamp with time zone
            END, nsoq.next_scheduled_outreach_scheduled_at_date_time::timestamp with time zone) AND nsoq.next_scheduled_outreach_reason_id::text = 'OTHER'::text THEN 'OTHER'::text
            WHEN poo.max_outreach_date_time IS NULL AND smg.max_delivered_scheduled_message_group_date_time IS NULL AND ssiq.screening_session_id IS NULL THEN 'WELCOME_MESSAGE'::text
            WHEN ssq.screening_session_id IS NULL AND rssq.scheduled_date_time IS NULL AND (poo.max_outreach_date_time IS NOT NULL OR smg.max_delivered_scheduled_message_group_date_time IS NOT NULL) AND ((GREATEST(poo.max_outreach_date_time, smg.max_delivered_scheduled_message_group_date_time) + make_interval(days => i.integrated_care_outreach_followup_day_offset)) AT TIME ZONE i.time_zone) <= now() THEN 'ASSESSMENT_OUTREACH'::text
            WHEN nrcismgq.next_resource_check_in_scheduled_message_group_id IS NOT NULL THEN 'RESOURCE_CHECK_IN'::text
            ELSE NULL::text
        END AS next_contact_type_id,
        CASE
            WHEN ssq.screening_session_id IS NULL AND rssq.scheduled_date_time = LEAST(rssq.scheduled_date_time, nsoq.next_scheduled_outreach_scheduled_at_date_time) THEN rssq.scheduled_date_time
            WHEN nsoq.next_scheduled_outreach_scheduled_at_date_time = LEAST(
            CASE
                WHEN ssq.screening_session_id IS NULL THEN rssq.scheduled_date_time::timestamp with time zone
                ELSE '9999-12-31 23:59:59+00'::timestamp with time zone
            END, nsoq.next_scheduled_outreach_scheduled_at_date_time::timestamp with time zone) THEN nsoq.next_scheduled_outreach_scheduled_at_date_time
            WHEN poo.max_outreach_date_time IS NULL AND smg.max_delivered_scheduled_message_group_date_time IS NULL AND ssiq.screening_session_id IS NULL THEN NULL::timestamp without time zone
            WHEN ssq.screening_session_id IS NULL AND rssq.scheduled_date_time IS NULL AND (poo.max_outreach_date_time IS NOT NULL OR smg.max_delivered_scheduled_message_group_date_time IS NOT NULL) AND ((GREATEST(poo.max_outreach_date_time, smg.max_delivered_scheduled_message_group_date_time) + make_interval(days => i.integrated_care_outreach_followup_day_offset)) AT TIME ZONE i.time_zone) <= now() THEN NULL::timestamp without time zone
            WHEN nrcismgq.next_resource_check_in_scheduled_message_group_id IS NOT NULL THEN nrcismgq.next_resource_check_in_scheduled_at_date_time
            ELSE NULL::timestamp without time zone
        END AS next_contact_scheduled_at,
    poq.patient_order_id,
    poq.patient_order_import_id,
    poq.institution_id,
    poq.patient_account_id,
    poq.panel_account_id,
    poq.encounter_department_id,
    poq.encounter_department_id_type,
    poq.encounter_department_name,
    poq.referring_practice_id,
    poq.referring_practice_id_type,
    poq.referring_practice_name,
    poq.ordering_provider_id,
    poq.ordering_provider_id_type,
    poq.ordering_provider_last_name,
    poq.ordering_provider_first_name,
    poq.ordering_provider_middle_name,
    poq.billing_provider_id,
    poq.billing_provider_id_type,
    poq.billing_provider_last_name,
    poq.billing_provider_first_name,
    poq.billing_provider_middle_name,
    poq.patient_last_name,
    poq.patient_first_name,
    poq.patient_mrn,
    poq.patient_unique_id,
    poq.patient_unique_id_type,
    poq.patient_birth_sex_id,
    poq.patient_birthdate,
    poq.patient_address_id,
    poq.primary_payor_id,
    poq.primary_payor_name,
    poq.primary_plan_id,
    poq.primary_plan_name,
    poq.order_date,
    poq.order_age_in_minutes,
    poq.order_id,
    poq.routing,
    poq.associated_diagnosis,
    poq.patient_phone_number,
    poq.preferred_contact_hours,
    poq.comments,
    poq.cc_recipients,
    poq.last_active_medication_order_summary,
    poq.medications,
    poq.recent_psychotherapeutic_medications,
    poq.episode_closed_at,
    poq.test_patient_email_address,
    poq.test_patient_password,
    poq.created,
    poq.last_updated,
    poq.patient_order_closure_reason_id,
    poq.resources_sent_at,
    poq.patient_ethnicity_id,
    poq.patient_race_id,
    poq.patient_gender_identity_id,
    poq.patient_language_code,
    poq.patient_email_address,
    poq.patient_order_disposition_id,
    poq.episode_closed_by_account_id,
    poq.patient_order_safety_planning_status_id,
    poq.connected_to_safety_planning_at,
    poq.patient_order_resourcing_status_id,
    poq.resources_sent_note,
    poq.last_modified,
    poq.patient_order_consent_status_id,
    poq.consent_status_updated_at,
    poq.consent_status_updated_by_account_id,
    poq.patient_order_resource_check_in_response_status_id,
    poq.resource_check_in_response_status_updated_at,
    poq.resource_check_in_response_status_updated_by_account_id,
    poq.patient_demographics_confirmed_at,
    poq.patient_demographics_confirmed_by_account_id,
    poq.patient_order_resourcing_type_id,
    poq.patient_order_care_preference_id,
    poq.in_person_care_radius,
    poq.in_person_care_radius_distance_unit_id,
    poq.resource_check_in_scheduled_message_group_id,
    poq.primary_plan_accepted,
    poq.test_patient_order,
    poq.patient_order_demographics_import_status_id,
    poq.patient_demographics_imported_at,
    poq.patient_order_intake_wants_services_status_id,
    poq.patient_order_intake_location_status_id,
    poq.patient_order_intake_insurance_status_id,
    poq.epic_department_id,
    poq.encounter_csn,
    poq.encounter_synced_at,
    poq.reference_number,
    poq.patient_preferred_pronoun_id,
    poq.patient_clinical_sex_id,
    poq.patient_legal_sex_id,
    poq.patient_administrative_gender_id,
    poq.resource_packet_id,
    poq.patient_order_referral_source_id,
    poq.override_scheduling_epic_department_id
   FROM patient_order poq
     LEFT JOIN patient_order_disposition pod ON poq.patient_order_disposition_id::text = pod.patient_order_disposition_id::text
     LEFT JOIN patient_order_closure_reason pocr ON poq.patient_order_closure_reason_id::text = pocr.patient_order_closure_reason_id::text
     LEFT JOIN institution i ON poq.institution_id::text = i.institution_id::text
     LEFT JOIN permitted_regions_query prq ON poq.institution_id::text = prq.institution_id::text
     LEFT JOIN patient_order_resource_check_in_response_status porcirs ON poq.patient_order_resource_check_in_response_status_id = porcirs.patient_order_resource_check_in_response_status_id
     LEFT JOIN epic_department ed ON poq.epic_department_id = ed.epic_department_id
     LEFT JOIN address patient_address ON poq.patient_address_id = patient_address.address_id
     LEFT JOIN poo ON poq.patient_order_id = poo.patient_order_id
     LEFT JOIN smg ON poq.patient_order_id = smg.patient_order_id
     LEFT JOIN ss_query ssq ON poq.patient_order_id = ssq.patient_order_id
     LEFT JOIN ss_intake_query ssiq ON poq.patient_order_id = ssiq.patient_order_id
     LEFT JOIN patient_order_triage_group potg ON poq.patient_order_id = potg.patient_order_id AND potg.active = true
     LEFT JOIN patient_order_care_type poct ON potg.patient_order_care_type_id = poct.patient_order_care_type_id::text
     LEFT JOIN account panel_account ON poq.panel_account_id = panel_account.account_id
     LEFT JOIN recent_po_query rpq ON poq.patient_order_id = rpq.patient_order_id
     LEFT JOIN recent_scheduled_screening_query rssq ON poq.patient_order_id = rssq.patient_order_id
     LEFT JOIN next_appt_query naq ON poq.patient_order_id = naq.patient_order_id
     LEFT JOIN recent_voicemail_task_query rvtq ON poq.patient_order_id = rvtq.patient_order_id
     LEFT JOIN reason_for_referral_query rfrq ON poq.patient_order_id = rfrq.patient_order_id
     LEFT JOIN next_scheduled_outreach_query nsoq ON poq.patient_order_id = nsoq.patient_order_id
     LEFT JOIN most_recent_message_delivered_query mrmdq ON poq.patient_order_id = mrmdq.patient_order_id
     LEFT JOIN next_resource_check_in_scheduled_message_group_query nrcismgq ON poq.patient_order_id = nrcismgq.patient_order_id
     LEFT JOIN patient_order_scheduled_message_group posmg ON poq.resource_check_in_scheduled_message_group_id = posmg.patient_order_scheduled_message_group_id AND posmg.deleted = false;

CREATE VIEW v_patient_order AS
SELECT * FROM v_all_patient_order
WHERE patient_order_disposition_id != 'ARCHIVED';

COMMIT;