/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cobaltplatform.api.model.db;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class StudyBeiweConfig {
	@Nonnull
	private static final Gson GSON;
	@Nonnull
	private static final Gson DEVICE_SETTINGS_GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();

		// Special formatting for Beiwe device_settings field
		DEVICE_SETTINGS_GSON = new GsonBuilder()
				.disableHtmlEscaping()
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.create();
	}

	@Nullable
	private UUID studyBeiweConfigId;
	@Nullable
	private UUID studyId;

	// Whether various device options are turned on

	@Nullable
	private Boolean accelerometer;
	@Nullable
	private Boolean gps;
	@Nullable
	private Boolean calls;
	@Nullable
	private Boolean texts;
	@Nullable
	private Boolean wifi;
	@Nullable
	private Boolean bluetooth;
	@Nullable
	private Boolean powerState;
	@Nullable
	private Boolean useAnonymizedHashing;
	@Nullable
	private Boolean useGpsFuzzing;
	@Nullable
	private Boolean callClinicianButtonEnabled;
	@Nullable
	private Boolean callResearchAssistantButtonEnabled;
	@Nullable
	private Boolean ambientAudio;

	// Whether iOS-specific data streams are turned on

	@Nullable
	private Boolean proximity;
	@Nullable
	private Boolean gyro;
	@Nullable
	private Boolean magnetometer;
	@Nullable
	private Boolean devicemotion;
	@Nullable
	private Boolean reachability;

	// Upload over cellular data or only over WiFi (WiFi-only is default)

	@Nullable
	private Boolean allowUploadOverCellularData;

	// Timer variables

	@Nullable
	private Integer accelerometerOffDurationSeconds;

	@Nullable
	private Integer accelerometerOnDurationSeconds;
	@Nullable
	private Integer accelerometerFrequency;
	@Nullable
	private Integer ambientAudioOffDurationSeconds;
	@Nullable
	private Integer ambientAudioOnDurationSeconds;
	@Nullable
	private Integer ambientAudioBitrate;
	@Nullable
	private Integer ambientAudioSamplingRate;
	@Nullable
	private Integer bluetoothOnDurationSeconds;
	@Nullable
	private Integer bluetoothTotalDurationSeconds;
	@Nullable
	private Integer bluetoothGlobalOffsetSeconds;
	@Nullable
	private Integer checkForNewSurveysFrequencySeconds;
	@Nullable
	private Integer createNewDataFilesFrequencySeconds;
	@Nullable
	private Integer gpsOffDurationSeconds;
	@Nullable
	private Integer gpsOnDurationSeconds;
	@Nullable
	private Integer secondsBeforeAutoLogout;
	@Nullable
	private Integer uploadDataFilesFrequencySeconds;
	@Nullable
	private Integer voiceRecordingMaxTimeLengthSeconds;
	@Nullable
	private Integer wifiLogFrequencySeconds;
	@Nullable
	private Integer gyroOffDurationSeconds;
	@Nullable
	private Integer gyroOnDurationSeconds;
	@Nullable
	private Integer gyroFrequency;

	// iOS-specific timer variables

	@Nullable
	private Integer magnetometerOffDurationSeconds;
	@Nullable
	private Integer magnetometerOnDurationSeconds;
	@Nullable
	private Integer devicemotionOffDurationSeconds;
	@Nullable
	private Integer devicemotionOnDurationSeconds;

	// Text strings

	@Nullable
	private String aboutPageText;
	@Nullable
	private String callClinicianButtonText;
	@Nullable
	private String consentFormText;
	@Nullable
	private String surveySubmitSuccessToastText;

	// Consent sections

	@Nullable
	@DatabaseColumn("consent_sections")
	private String consentSectionsAsString;
	@Nullable
	private Map<String, Object> consentSections;

	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	// Special accessors/mutators

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	@Nonnull
	protected Gson getDeviceSettingsGson() {
		return DEVICE_SETTINGS_GSON;
	}

	@Nullable
	public String getConsentSectionsAsString() {
		return this.consentSectionsAsString;
	}

	public void setConsentSectionsAsString(@Nullable String consentSectionsAsString) {
		this.consentSectionsAsString = consentSectionsAsString;

		String consentSections = trimToNull(consentSectionsAsString);
		this.consentSections = consentSections == null ? Map.of() : getGson().fromJson(consentSections, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	// No corresponding setter.  Value is driven by #setConsentSectionsAsString above
	@Nonnull
	public Map<String, Object> getConsentSections() {
		return this.consentSections;
	}

	public Map<String, Object> toDeviceSettingsRepresentation() {
		String studyBeiweConfigAsJson = getDeviceSettingsGson().toJson(this);

		// Device Settings model is available at https://github.com/onnela-lab/beiwe-backend/blob/main/database/study_models.py
		Map<String, Object> deviceSettings = getDeviceSettingsGson().fromJson(studyBeiweConfigAsJson, new TypeToken<Map<String, Object>>() {
		}.getType());

		// There are some internal fields unrelated to Beiwe on this record we don't need to expose
		deviceSettings.remove("study_beiwe_config_id");
		deviceSettings.remove("study_id");
		deviceSettings.remove("consent_sections_as_string");
		deviceSettings.remove("created");
		deviceSettings.remove("last_updated");

		return deviceSettings;
	}

	// Remaining accessors/mutators

	@Nullable
	public UUID getStudyBeiweConfigId() {
		return this.studyBeiweConfigId;
	}

	public void setStudyBeiweConfigId(@Nullable UUID studyBeiweConfigId) {
		this.studyBeiweConfigId = studyBeiweConfigId;
	}

	@Nullable
	public UUID getStudyId() {
		return this.studyId;
	}

	public void setStudyId(@Nullable UUID studyId) {
		this.studyId = studyId;
	}

	@Nullable
	public Boolean getAccelerometer() {
		return this.accelerometer;
	}

	public void setAccelerometer(@Nullable Boolean accelerometer) {
		this.accelerometer = accelerometer;
	}

	@Nullable
	public Boolean getGps() {
		return this.gps;
	}

	public void setGps(@Nullable Boolean gps) {
		this.gps = gps;
	}

	@Nullable
	public Boolean getCalls() {
		return this.calls;
	}

	public void setCalls(@Nullable Boolean calls) {
		this.calls = calls;
	}

	@Nullable
	public Boolean getTexts() {
		return this.texts;
	}

	public void setTexts(@Nullable Boolean texts) {
		this.texts = texts;
	}

	@Nullable
	public Boolean getWifi() {
		return this.wifi;
	}

	public void setWifi(@Nullable Boolean wifi) {
		this.wifi = wifi;
	}

	@Nullable
	public Boolean getBluetooth() {
		return this.bluetooth;
	}

	public void setBluetooth(@Nullable Boolean bluetooth) {
		this.bluetooth = bluetooth;
	}

	@Nullable
	public Boolean getPowerState() {
		return this.powerState;
	}

	public void setPowerState(@Nullable Boolean powerState) {
		this.powerState = powerState;
	}

	@Nullable
	public Boolean getUseAnonymizedHashing() {
		return this.useAnonymizedHashing;
	}

	public void setUseAnonymizedHashing(@Nullable Boolean useAnonymizedHashing) {
		this.useAnonymizedHashing = useAnonymizedHashing;
	}

	@Nullable
	public Boolean getUseGpsFuzzing() {
		return this.useGpsFuzzing;
	}

	public void setUseGpsFuzzing(@Nullable Boolean useGpsFuzzing) {
		this.useGpsFuzzing = useGpsFuzzing;
	}

	@Nullable
	public Boolean getCallClinicianButtonEnabled() {
		return this.callClinicianButtonEnabled;
	}

	public void setCallClinicianButtonEnabled(@Nullable Boolean callClinicianButtonEnabled) {
		this.callClinicianButtonEnabled = callClinicianButtonEnabled;
	}

	@Nullable
	public Boolean getCallResearchAssistantButtonEnabled() {
		return this.callResearchAssistantButtonEnabled;
	}

	public void setCallResearchAssistantButtonEnabled(@Nullable Boolean callResearchAssistantButtonEnabled) {
		this.callResearchAssistantButtonEnabled = callResearchAssistantButtonEnabled;
	}

	@Nullable
	public Boolean getAmbientAudio() {
		return this.ambientAudio;
	}

	public void setAmbientAudio(@Nullable Boolean ambientAudio) {
		this.ambientAudio = ambientAudio;
	}

	@Nullable
	public Boolean getProximity() {
		return this.proximity;
	}

	public void setProximity(@Nullable Boolean proximity) {
		this.proximity = proximity;
	}

	@Nullable
	public Boolean getGyro() {
		return this.gyro;
	}

	public void setGyro(@Nullable Boolean gyro) {
		this.gyro = gyro;
	}

	@Nullable
	public Boolean getMagnetometer() {
		return this.magnetometer;
	}

	public void setMagnetometer(@Nullable Boolean magnetometer) {
		this.magnetometer = magnetometer;
	}

	@Nullable
	public Boolean getDevicemotion() {
		return this.devicemotion;
	}

	public void setDevicemotion(@Nullable Boolean devicemotion) {
		this.devicemotion = devicemotion;
	}

	@Nullable
	public Boolean getReachability() {
		return this.reachability;
	}

	public void setReachability(@Nullable Boolean reachability) {
		this.reachability = reachability;
	}

	@Nullable
	public Boolean getAllowUploadOverCellularData() {
		return this.allowUploadOverCellularData;
	}

	public void setAllowUploadOverCellularData(@Nullable Boolean allowUploadOverCellularData) {
		this.allowUploadOverCellularData = allowUploadOverCellularData;
	}

	@Nullable
	public Integer getAccelerometerOffDurationSeconds() {
		return this.accelerometerOffDurationSeconds;
	}

	public void setAccelerometerOffDurationSeconds(@Nullable Integer accelerometerOffDurationSeconds) {
		this.accelerometerOffDurationSeconds = accelerometerOffDurationSeconds;
	}

	@Nullable
	public Integer getAccelerometerOnDurationSeconds() {
		return this.accelerometerOnDurationSeconds;
	}

	public void setAccelerometerOnDurationSeconds(@Nullable Integer accelerometerOnDurationSeconds) {
		this.accelerometerOnDurationSeconds = accelerometerOnDurationSeconds;
	}

	@Nullable
	public Integer getAccelerometerFrequency() {
		return this.accelerometerFrequency;
	}

	public void setAccelerometerFrequency(@Nullable Integer accelerometerFrequency) {
		this.accelerometerFrequency = accelerometerFrequency;
	}

	@Nullable
	public Integer getAmbientAudioOffDurationSeconds() {
		return this.ambientAudioOffDurationSeconds;
	}

	public void setAmbientAudioOffDurationSeconds(@Nullable Integer ambientAudioOffDurationSeconds) {
		this.ambientAudioOffDurationSeconds = ambientAudioOffDurationSeconds;
	}

	@Nullable
	public Integer getAmbientAudioOnDurationSeconds() {
		return this.ambientAudioOnDurationSeconds;
	}

	public void setAmbientAudioOnDurationSeconds(@Nullable Integer ambientAudioOnDurationSeconds) {
		this.ambientAudioOnDurationSeconds = ambientAudioOnDurationSeconds;
	}

	@Nullable
	public Integer getAmbientAudioBitrate() {
		return this.ambientAudioBitrate;
	}

	public void setAmbientAudioBitrate(@Nullable Integer ambientAudioBitrate) {
		this.ambientAudioBitrate = ambientAudioBitrate;
	}

	@Nullable
	public Integer getAmbientAudioSamplingRate() {
		return this.ambientAudioSamplingRate;
	}

	public void setAmbientAudioSamplingRate(@Nullable Integer ambientAudioSamplingRate) {
		this.ambientAudioSamplingRate = ambientAudioSamplingRate;
	}

	@Nullable
	public Integer getBluetoothOnDurationSeconds() {
		return this.bluetoothOnDurationSeconds;
	}

	public void setBluetoothOnDurationSeconds(@Nullable Integer bluetoothOnDurationSeconds) {
		this.bluetoothOnDurationSeconds = bluetoothOnDurationSeconds;
	}

	@Nullable
	public Integer getBluetoothTotalDurationSeconds() {
		return this.bluetoothTotalDurationSeconds;
	}

	public void setBluetoothTotalDurationSeconds(@Nullable Integer bluetoothTotalDurationSeconds) {
		this.bluetoothTotalDurationSeconds = bluetoothTotalDurationSeconds;
	}

	@Nullable
	public Integer getBluetoothGlobalOffsetSeconds() {
		return this.bluetoothGlobalOffsetSeconds;
	}

	public void setBluetoothGlobalOffsetSeconds(@Nullable Integer bluetoothGlobalOffsetSeconds) {
		this.bluetoothGlobalOffsetSeconds = bluetoothGlobalOffsetSeconds;
	}

	@Nullable
	public Integer getCheckForNewSurveysFrequencySeconds() {
		return this.checkForNewSurveysFrequencySeconds;
	}

	public void setCheckForNewSurveysFrequencySeconds(@Nullable Integer checkForNewSurveysFrequencySeconds) {
		this.checkForNewSurveysFrequencySeconds = checkForNewSurveysFrequencySeconds;
	}

	@Nullable
	public Integer getCreateNewDataFilesFrequencySeconds() {
		return this.createNewDataFilesFrequencySeconds;
	}

	public void setCreateNewDataFilesFrequencySeconds(@Nullable Integer createNewDataFilesFrequencySeconds) {
		this.createNewDataFilesFrequencySeconds = createNewDataFilesFrequencySeconds;
	}

	@Nullable
	public Integer getGpsOffDurationSeconds() {
		return this.gpsOffDurationSeconds;
	}

	public void setGpsOffDurationSeconds(@Nullable Integer gpsOffDurationSeconds) {
		this.gpsOffDurationSeconds = gpsOffDurationSeconds;
	}

	@Nullable
	public Integer getGpsOnDurationSeconds() {
		return this.gpsOnDurationSeconds;
	}

	public void setGpsOnDurationSeconds(@Nullable Integer gpsOnDurationSeconds) {
		this.gpsOnDurationSeconds = gpsOnDurationSeconds;
	}

	@Nullable
	public Integer getSecondsBeforeAutoLogout() {
		return this.secondsBeforeAutoLogout;
	}

	public void setSecondsBeforeAutoLogout(@Nullable Integer secondsBeforeAutoLogout) {
		this.secondsBeforeAutoLogout = secondsBeforeAutoLogout;
	}

	@Nullable
	public Integer getUploadDataFilesFrequencySeconds() {
		return this.uploadDataFilesFrequencySeconds;
	}

	public void setUploadDataFilesFrequencySeconds(@Nullable Integer uploadDataFilesFrequencySeconds) {
		this.uploadDataFilesFrequencySeconds = uploadDataFilesFrequencySeconds;
	}

	@Nullable
	public Integer getVoiceRecordingMaxTimeLengthSeconds() {
		return this.voiceRecordingMaxTimeLengthSeconds;
	}

	public void setVoiceRecordingMaxTimeLengthSeconds(@Nullable Integer voiceRecordingMaxTimeLengthSeconds) {
		this.voiceRecordingMaxTimeLengthSeconds = voiceRecordingMaxTimeLengthSeconds;
	}

	@Nullable
	public Integer getWifiLogFrequencySeconds() {
		return this.wifiLogFrequencySeconds;
	}

	public void setWifiLogFrequencySeconds(@Nullable Integer wifiLogFrequencySeconds) {
		this.wifiLogFrequencySeconds = wifiLogFrequencySeconds;
	}

	@Nullable
	public Integer getGyroOffDurationSeconds() {
		return this.gyroOffDurationSeconds;
	}

	public void setGyroOffDurationSeconds(@Nullable Integer gyroOffDurationSeconds) {
		this.gyroOffDurationSeconds = gyroOffDurationSeconds;
	}

	@Nullable
	public Integer getGyroOnDurationSeconds() {
		return this.gyroOnDurationSeconds;
	}

	public void setGyroOnDurationSeconds(@Nullable Integer gyroOnDurationSeconds) {
		this.gyroOnDurationSeconds = gyroOnDurationSeconds;
	}

	@Nullable
	public Integer getGyroFrequency() {
		return this.gyroFrequency;
	}

	public void setGyroFrequency(@Nullable Integer gyroFrequency) {
		this.gyroFrequency = gyroFrequency;
	}

	@Nullable
	public Integer getMagnetometerOffDurationSeconds() {
		return this.magnetometerOffDurationSeconds;
	}

	public void setMagnetometerOffDurationSeconds(@Nullable Integer magnetometerOffDurationSeconds) {
		this.magnetometerOffDurationSeconds = magnetometerOffDurationSeconds;
	}

	@Nullable
	public Integer getMagnetometerOnDurationSeconds() {
		return this.magnetometerOnDurationSeconds;
	}

	public void setMagnetometerOnDurationSeconds(@Nullable Integer magnetometerOnDurationSeconds) {
		this.magnetometerOnDurationSeconds = magnetometerOnDurationSeconds;
	}

	@Nullable
	public Integer getDevicemotionOffDurationSeconds() {
		return this.devicemotionOffDurationSeconds;
	}

	public void setDevicemotionOffDurationSeconds(@Nullable Integer devicemotionOffDurationSeconds) {
		this.devicemotionOffDurationSeconds = devicemotionOffDurationSeconds;
	}

	@Nullable
	public Integer getDevicemotionOnDurationSeconds() {
		return this.devicemotionOnDurationSeconds;
	}

	public void setDevicemotionOnDurationSeconds(@Nullable Integer devicemotionOnDurationSeconds) {
		this.devicemotionOnDurationSeconds = devicemotionOnDurationSeconds;
	}

	@Nullable
	public String getAboutPageText() {
		return this.aboutPageText;
	}

	public void setAboutPageText(@Nullable String aboutPageText) {
		this.aboutPageText = aboutPageText;
	}

	@Nullable
	public String getCallClinicianButtonText() {
		return this.callClinicianButtonText;
	}

	public void setCallClinicianButtonText(@Nullable String callClinicianButtonText) {
		this.callClinicianButtonText = callClinicianButtonText;
	}

	@Nullable
	public String getConsentFormText() {
		return this.consentFormText;
	}

	public void setConsentFormText(@Nullable String consentFormText) {
		this.consentFormText = consentFormText;
	}

	@Nullable
	public String getSurveySubmitSuccessToastText() {
		return this.surveySubmitSuccessToastText;
	}

	public void setSurveySubmitSuccessToastText(@Nullable String surveySubmitSuccessToastText) {
		this.surveySubmitSuccessToastText = surveySubmitSuccessToastText;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}