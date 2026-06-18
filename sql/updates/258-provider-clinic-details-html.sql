BEGIN;
SELECT _v.register_patch('258-provider-clinic-details-html', NULL, NULL);

ALTER TABLE provider ADD COLUMN IF NOT EXISTS details_html TEXT;
ALTER TABLE clinic ADD COLUMN IF NOT EXISTS details_html TEXT;

DO $$
DECLARE
  v_clinic_details_html CONSTANT TEXT := $details_html$
<section class="mb-8">
  <h2 class="mb-4">About</h2>
  <p class="mb-0 fs-large">
    The Penn Autism Clinic provides diagnostic assessments and ongoing support for children, teens and adults who have, or may have, autism. Clinicians can perform evaluations, offer treatment recommendations, and coordinate with intervention providers to guide families through diagnosis and long-term care.
  </p>
</section>
<section class="mb-8">
  <h2 class="mb-4">Who can self-schedule a Penn Autism Clinic appointment?</h2>
  <p class="mb-2 fs-large"><strong>If you are seeking services for yourself:</strong></p>
  <p class="mb-4 fs-large">You must be a benefits-eligible employee of UPHS or UPenn.</p>
  <p class="mb-2 fs-large"><strong>If you are seeking services for a dependent:</strong></p>
  <p class="mb-0 fs-large">You must be the legal guardian of the dependent and your dependent must be covered under your UPHS or UPenn health insurance.</p>
</section>
<section>
  <h2 class="mb-4">Accepted Insurances</h2>
  <p class="mb-4 fs-large"><strong>You will need to confirm your insurance before booking.</strong></p>
  <div class="table-responsive">
    <table class="table table-bordered align-middle mb-0">
      <thead class="table-light">
        <tr>
          <th scope="col">Health Insurance</th>
          <th scope="col">Behavioral Health Insurance</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>Aetna Choice Point-of-Service (POS) II</td>
          <td>Aetna Behavioral Health Network</td>
        </tr>
        <tr>
          <td>Aetna High Deductible Health Plan (HDHP) with Health Savings Account</td>
          <td>Aetna Behavioral Health Network</td>
        </tr>
        <tr>
          <td>Keystone/Amerihealth Health Maintenance Organization (HMO) administered by Independence Blue Cross (IBX)</td>
          <td>IBX Behavioral Health</td>
        </tr>
        <tr>
          <td>PennCare/Personal Choice Preferred Provider Organization (PPO) Plan administered by Independence Blue Cross (IBX)</td>
          <td>Quest Behavioral Health</td>
        </tr>
        <tr>
          <td>PennCare High Deductible Health Plan (HDHP) administered by Independence Blue Cross (IBX)</td>
          <td>Quest Behavioral Health</td>
        </tr>
      </tbody>
    </table>
  </div>
</section>
$details_html$;
BEGIN
  UPDATE clinic
  SET details_html = v_clinic_details_html
  WHERE institution_id = 'COBALT'
    AND LOWER(TRIM(description)) = LOWER(TRIM('Penn Autism Clinic'));
END $$;

COMMIT;
