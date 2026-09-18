BEGIN;
SELECT _v.register_patch(
  '273-local-only-cobalt-ic-ease-triage',
  ARRAY['271-local-only-cobalt-ic-ease'],
  NULL
);

-- Provisional EASE triage policy for Penn review. Keep this local mirror in
-- sync with the PENN_EASE results function.
-- Self-booking is driven by COLLABORATIVE triage; this does not use the
-- specialty-care override_scheduling_epic_department_id escape hatch.
UPDATE screening_flow_version
SET results_function=$jscode$
output.supportRoleRecommendations = [{ supportRoleId: 'MHP', weight: 1 }];
output.recommendedTagIds = [];
output.integratedCareTriages = [];

const phq9 = input.screeningSessionScreenings[0];
const promis = input.screeningSessionScreenings.length > 1
  ? input.screeningSessionScreenings[1]
  : null;
const phq9Questions = input.screeningResultsByScreeningSessionScreeningId[phq9.screeningSessionScreeningId] || [];
const phq9Question9 = phq9Questions.length > 8 ? phq9Questions[8] : null;
const phq9Question9Response = phq9Question9 && phq9Question9.screeningResponses.length > 0
  ? phq9Question9.screeningResponses[0]
  : null;
const phq9Question9Positive = !!(
  phq9Question9Response
  && phq9Question9Response.screeningAnswerOption.score > 0
);
const phq9Score = phq9.scoreAsObject.overallScore;
const promisTScore = promis && promis.scoreAsObject
  ? promis.scoreAsObject.tScore
  : null;

if (phq9Score >= 20) {
  output.integratedCareTriages.push({
    patientOrderFocusTypeId: 'EVALUATION',
    patientOrderCareTypeId: 'SPECIALTY',
    reason: 'PHQ-9 score was 20 or higher.'
  });
} else if (phq9Question9Positive || phq9Score >= 5 || (promisTScore !== null && promisTScore <= 40)) {
  const collaborativeReasons = [];

  if (phq9Question9Positive) {
    collaborativeReasons.push('PHQ-9 question 9 response was non-zero');
  }

  if (phq9Score >= 5) {
    collaborativeReasons.push('PHQ-9 score was between 5 and 19');
  }

  if (promisTScore !== null && promisTScore <= 40) {
    collaborativeReasons.push('PROMIS social roles T-score was 40 or lower');
  }

  output.integratedCareTriages.push({
    patientOrderFocusTypeId: 'EVALUATION',
    patientOrderCareTypeId: 'COLLABORATIVE',
    reason: collaborativeReasons.join('; ') + '.'
  });
} else {
  output.integratedCareTriages.push({
    patientOrderFocusTypeId: 'SELF_DIRECTED',
    patientOrderCareTypeId: 'SUBCLINICAL',
    reason: 'PHQ-9 score was below 5 and PROMIS social roles T-score was above 40.'
  });
}

output.integratedCareTriagedCareTypeId = output.integratedCareTriages[0].patientOrderCareTypeId;
$jscode$
WHERE screening_flow_version_id='1d7f6dcb-35cc-4f98-90f3-a60b4698a766';

COMMIT;
