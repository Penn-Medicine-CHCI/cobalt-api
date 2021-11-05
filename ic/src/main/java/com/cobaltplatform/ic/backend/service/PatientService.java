package com.cobaltplatform.ic.backend.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.cobaltplatform.ic.backend.config.EpicConfig;
import com.cobaltplatform.ic.backend.model.auth.FhirTokenResponse;
import com.cobaltplatform.ic.backend.model.db.DAssessment;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.backend.model.db.query.QDPatient;
import com.cobaltplatform.ic.model.DispositionFlag;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.NotFoundResponse;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class PatientService {
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);

    @Nonnull
    public Boolean isFakePatient(@Nonnull Patient patient) {
        if(!EpicConfig.isFakeSsoSupported())
            throw new UnsupportedOperationException();

        requireNonNull(patient);

        List<Identifier> identifiers = patient.getIdentifier();

        for(Identifier identifier : identifiers) {
            boolean isUid = identifier.getType() != null && Objects.equals("UID", identifier.getType().getText());
            String value = identifier.getValue() == null ? "" : identifier.getValue();

            if (isUid && value.startsWith("fake-"))
                return true;
        }

        return false;
    }

    @Nonnull
    public Boolean isFakePatient(@Nonnull DPatient patient) {
        if (!EpicConfig.isFakeSsoSupported())
            throw new UnsupportedOperationException();

        requireNonNull(patient);

        return patient.getUid() != null && patient.getUid().startsWith("fake-");
    }

    @Nonnull
    protected String fakeUidFromEmailAddress(@Nonnull String emailAddress) {
        if(!EpicConfig.isFakeSsoSupported())
            throw new UnsupportedOperationException();

        requireNonNull(emailAddress);
        return format("fake-%s", emailAddress);
    }

    @Nonnull
    public Optional<HumanName> humanNameFromFakeEmailAddress(@Nonnull String emailAddress) {
        if(!EpicConfig.isFakeSsoSupported())
            throw new UnsupportedOperationException();

        requireNonNull(emailAddress);

        emailAddress = emailAddress.trim();

        // Must be of the form "firstname.lastname@anything.com"
        String[] components = emailAddress.split("@");

        if(components.length != 2)
            return Optional.empty();

        String[] usernameComponents = components[0].split("\\.");

        if(usernameComponents.length != 2)
            return Optional.empty();

        String firstName = StringUtils.capitalize(usernameComponents[0]);
        String lastName = StringUtils.capitalize(usernameComponents[1]);

        StringType givenName = new StringType();
        givenName.setValue(firstName);

        HumanName humanName = new HumanName();
        humanName.setText(format("%s %s", firstName, lastName));
        humanName.setFamily(lastName);
        humanName.setGiven(List.of(givenName));

        return Optional.of(humanName);
    }

    @Nonnull
    public Patient fakePatient(@Nonnull String emailAddress) {
        if(!EpicConfig.isFakeSsoSupported())
            throw new UnsupportedOperationException();

        requireNonNull(emailAddress);

        CodeableConcept type = new CodeableConcept();
        type.setText("UID");

        Identifier identifier = new Identifier();
        identifier.setValue(fakeUidFromEmailAddress(emailAddress));
        identifier.setType(type);

        List<Identifier> identifiers = List.of(identifier);

        HumanName humanName = humanNameFromFakeEmailAddress(emailAddress).orElse(null);

        if(humanName == null)
            throw new IllegalStateException(format("Email address '%s' is not in the right format for extracting a name", emailAddress));

        List<HumanName> names = List.of(humanName);

        ContactPoint phoneNumber = new ContactPoint();
        phoneNumber.setValue("555-111-2222");
        phoneNumber.setSystem(ContactPoint.ContactPointSystem.PHONE);
        phoneNumber.setUse(ContactPoint.ContactPointUse.HOME);

        ContactPoint contactEmailAddress = new ContactPoint();
        contactEmailAddress.setValue(emailAddress);
        contactEmailAddress.setSystem(ContactPoint.ContactPointSystem.EMAIL);

        List<ContactPoint> contactPoints = List.of(phoneNumber, contactEmailAddress);

        StringType line = new StringType();
        line.setValue("112 Fayette St");

        Address address = new Address();
        address.setCity("CONSHOHOCKEN");
        address.setDistrict("FAKE");
        address.setState("PA");
        address.setPostalCode("19428");
        address.setCountry("USA");
        address.setLine(List.of(line));
        address.setUse(Address.AddressUse.HOME);

        List<Address> addresses = List.of(address);

        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());
        patient.setIdentifier(identifiers);
        patient.setName(names);
        patient.setTelecom(contactPoints);
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        patient.setAddress(addresses);

        return patient;
    }

    public Patient getPatientByToken(FhirTokenResponse fhirPatientToken) {
        logger.info(StringUtils.join("Retrieving patient for patientId=", fhirPatientToken.getPatient(), " scope=",
            fhirPatientToken.getScope()));

        if(EpicConfig.isFakeSsoSupported()) {
            String emailAddress = fhirPatientToken.getPatient();
            DPatient patientRecord = getPatientByUid(fakeUidFromEmailAddress(emailAddress)).orElse(null);

            if(patientRecord != null)
                return fakePatient(emailAddress);
        }

        var url =
            StringUtils.join(EpicConfig.getBaseUrl(), EpicConfig.getEnvironmentPath(), EpicConfig.getStu3ApiPath(),
                "/patient/",
                fhirPatientToken.getPatient());
        logger.trace("Using patientUrl={} to retrieve patient", url);
        var getRequest = new Request.Builder().get().url(url)
            .addHeader(HttpHeaders.AUTHORIZATION,
                StringUtils.join(StringUtils.capitalize(fhirPatientToken.getTokenType()), " ",
                    fhirPatientToken.getAccessToken()))
            .addHeader(HttpHeaders.ACCEPT, MediaType.XML_UTF_8.toString())
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.XML_UTF_8.toString())
            .build();

        try (var fhirResponse = httpClient.newCall(getRequest).execute()) {
            try {
                Validate.isTrue(fhirResponse.isSuccessful());
            } catch (Exception e) {
                logger.error("Failed to retrieve patientId={} with epicResponseCode={}", fhirPatientToken.getPatient(),
                    fhirResponse.code(), e);
                return null;
            }
            var responseByteStream = requireNonNull(fhirResponse.body()).string();
            logger.trace(responseByteStream);
            var fhirParser = fhirContext.newXmlParser();
            Patient parsedPatient = fhirParser.parseResource(Patient.class, responseByteStream);
            return parsedPatient;
        } catch (Exception e) {
            logger.error("Failed to reach Epic to retrieve patientId={}", fhirPatientToken.getPatient(), e);
            return null;
        }
    }

    public Patient getPatientByFhirIdAndAccessToken(String fhirId, String accessToken) {
        if(EpicConfig.isFakeSsoSupported()) {
            DPatient patient = getPatientByFhirId(fhirId).orElse(null);

            if(isFakePatient(patient))
                return fakePatient(patient.getPreferredEmail());
        }

        logger.trace("access token = {}", accessToken);
        var url =
            StringUtils.join(EpicConfig.getBaseUrl(), EpicConfig.getEnvironmentPath(), EpicConfig.getStu3ApiPath(),
                "/patient/",
                fhirId);
        logger.trace("Using patientUrl={} to retrieve patient", url);
        var getRequest = new Request.Builder().get().url(url)
            .addHeader(HttpHeaders.AUTHORIZATION,
                StringUtils.join(StringUtils.capitalize("Bearer "), accessToken))
            .addHeader(HttpHeaders.ACCEPT, MediaType.XML_UTF_8.toString())
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.XML_UTF_8.toString())
            .build();

        try (var fhirResponse = httpClient.newCall(getRequest).execute()) {
            try {
                Validate.isTrue(fhirResponse.isSuccessful());
            } catch (Exception e) {
                logger.error("Failed to retrieve patientId={} with epicResponseCode={}", fhirId,
                    fhirResponse.code(), e);
                return null;
            }
            var responseByteStream = requireNonNull(fhirResponse.body()).string();
            logger.trace(responseByteStream);
            var fhirParser = fhirContext.newXmlParser();
            return fhirParser.parseResource(Patient.class, responseByteStream);
        } catch (Exception e) {
            logger.error("Failed to reach Epic to retrieve patientId={}", fhirId, e);
            return null;
        }
    }

    public Optional<String> getUidFromPatientObject(Patient patient) {
        if(isFakePatient(patient)) {
            List<Identifier> identifiers = patient.getIdentifier();

            for(Identifier identifier : identifiers) {
                boolean isUid = identifier.getType() != null && Objects.equals("UID", identifier.getType().getText());
                String value = identifier.getValue() == null ? "" : identifier.getValue();

                if (isUid && value.startsWith("fake-"))
                    return Optional.of(value);
            }

            return Optional.empty();
        }

        return patient.getIdentifier().stream().filter(identifier -> {
            return StringUtils
                .equals(identifier.getSystem(), "urn:oid:1.3.6.1.4.1.22812.19.44324.0");
        }).map(Identifier::getValue).findFirst();
    }

    public static DPatient getPatientById(UUID patientId) {
        return new QDPatient().id.eq(patientId).findOneOrEmpty().orElseThrow(NotFoundResponse::new);
    }

    @Nonnull
    public static Optional<DPatient> getPatientByUid(@Nullable String uid) {
        return new QDPatient().uid.eq(uid).findOneOrEmpty();
    }

    @Nonnull
    public static Optional<DPatient> getPatientByFhirId(@Nullable String fhirId) {
        return new QDPatient().fhirId.eq(fhirId).findOneOrEmpty();
    }

    public Patient getPatientObject(String patientId) {
        try {
            File file = new File(
                requireNonNull(getClass().getClassLoader().getResource(patientId + ".json")).getFile());
            var patientString = FileUtils.readFileToString(file, Charset.defaultCharset());
            return fhirContext.newJsonParser().parseResource(Patient.class, patientString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public  static DPatient updatePatientDemographics(UUID patientId, String email, String phone){
        DPatient patient = new QDPatient().id.equalTo(patientId).findOneOrEmpty().orElseThrow();

        patient
                .setPreferredPhoneNumber(phone)
                .setPreferredEmail(email)
                .setPreferredEmailHasBeenUpdated(true)
                .setPreferredPhoneHasBeenUpdated(true)
                .save();

        return patient;
    }

    @Nonnull
    public List<DPatient> searchPatients(@Nullable String query) {
        String normalizedQuery = trimToNull(query);

        if (query != null)
            normalizedQuery = query.toLowerCase(Locale.US);

        QDPatient patientQuery = new QDPatient()
            .alias("patient")
            .where()
            .deleted.isFalse();

        if (normalizedQuery != null) {
            // For a patient firstname/lastname "Avocado Zzzpoc", we would match these inputs:
            //
            // avo
            // avocado
            // zz
            // zzzpoc
            // avocado zz
            // avocado zzzpoc
            String namePrefix = normalizedQuery + "%";
            patientQuery.raw("((LOWER(preferred_first_name) LIKE ? OR LOWER(preferred_last_name) LIKE ?) " +
                "OR LOWER(preferred_first_name) || ' ' || LOWER(preferred_last_name) LIKE ?)", namePrefix, namePrefix, namePrefix);
        }

        return patientQuery
            .raw("not exists (select 'x' from patient_disposition where patient_id=patient.id and flag IN (?,?,?))",
                DispositionFlag.GRADUATED, DispositionFlag.LOST_CONTACT_WITH_PATIENT, DispositionFlag.CONNECTED_TO_CARE)
            .setMaxRows(100)
            .orderBy()
            .preferredFirstName.asc()
            .preferredLastName.asc()
            .findList();
    }

    public Optional<DAssessment> getOrCreateDispositionAndAssessmentForPatient(UUID patientId) {
        DPatient patient;
        try {
            patient = PatientService.getPatientById(patientId);
        } catch (Exception e) {
            throw new NotFoundResponse();
        }

        DPatientDisposition disposition;
        try {
            disposition = DispositionService.getLatestDispositionForPatient(patient.getId())
                .orElseGet(() -> DispositionService.createDisposition(patient));
        } catch (Exception e) {
            logger.warn("Failed to get disposition");
            throw new InternalServerErrorResponse();
        }

        return AssessmentService.getOrCreateAssessment(disposition);
    }
}
