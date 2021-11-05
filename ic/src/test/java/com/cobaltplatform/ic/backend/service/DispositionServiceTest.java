package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.model.DispositionFlag;
import com.cobaltplatform.ic.model.DispositionOutcome;
import com.cobaltplatform.ic.model.DispositionOutcomeDiagnosis;
import io.ebean.test.ForTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DispositionServiceTest {
    private ForTests.RollbackAll rollbackAll;

    @BeforeEach
    public void before() {
        rollbackAll = ForTests.createRollbackAll();
    }

    @AfterEach
    public void after() {
        rollbackAll.close();
    }

    @org.junit.jupiter.api.Test
    void updateDispositionFlag_success() {
        DPatientDisposition disposition = new DPatientDisposition().setFlag(DispositionFlag.CONNECTED_TO_CARE);
        disposition.save();
        DispositionService.updateDispositionFlag(disposition.getId(), DispositionFlag.GRADUATED);
        disposition.refresh();
        Assertions.assertEquals(disposition.getFlag(), DispositionFlag.GRADUATED);
    }

    @org.junit.jupiter.api.Test
    void updateDispositionFlag_notFound() {
        Exception exception = assertThrows(NoSuchElementException.class, () -> {
            DispositionService.updateDispositionFlag(UUID.randomUUID(), DispositionFlag.GRADUATED);
        });
    }

    @org.junit.jupiter.api.Test
    void updateDispositionOutcome_success() {
        DPatientDisposition disposition = new DPatientDisposition();

        disposition
                .setFlag(DispositionFlag.CONNECTED_TO_CARE)
                .setCrisis(true)
                .setDiagnosis(DispositionOutcomeDiagnosis.CRISIS_CARE);

        disposition.save();

        var newOutcome  = new DispositionOutcome()
                .setDiagnosis(DispositionOutcomeDiagnosis.ADHD)
                .setCrisis(false)
                .setCare(DispositionOutcomeDiagnosis.ADHD.getCare());

        DispositionService.updateDispositionOutcome(disposition.getId(), newOutcome.getDiagnosis());
        disposition.refresh();
        Assertions.assertEquals(disposition.getFlag(), newOutcome.getDiagnosis().getFlag());
        assertEquals(disposition.isCrisis(), newOutcome.isCrisis());
        Assertions.assertEquals(disposition.getDiagnosis(), newOutcome.getDiagnosis());
    }

    @org.junit.jupiter.api.Test
    void updateDispositionOutcome_notFound() {
        Exception exception = assertThrows(NoSuchElementException.class, () -> {
            DispositionService.updateDispositionOutcome(UUID.randomUUID(), DispositionOutcomeDiagnosis.ADHD);
        });
    }

    @Test
    void getDispositionsForAllPatients() {
        DPatient patient1 = new DPatient();
        patient1.save();
        DPatientDisposition disposition11 = new DPatientDisposition()
                .setFlag(DispositionFlag.CONNECTED_TO_CARE)
                .setPatient(patient1);
        disposition11.save();
        DPatientDisposition disposition12 = new DPatientDisposition()
                .setFlag(DispositionFlag.CONNECTED_TO_CARE)
                .setPatient(patient1);
        disposition12.save();
        DPatient patient2 = new DPatient();
        patient2.save();
        DPatientDisposition disposition21 = new DPatientDisposition()
                .setFlag(DispositionFlag.CONNECTED_TO_CARE)
                .setPatient(patient2);
        disposition21.save();
//        List<DPatientDisposition> responses = DispositionService.getDispositionsForAllPatients();
//        assertAll("dispositions",
//                () -> assertEquals(0, responses.size()),
//                () -> assertTrue(
//                        responses.stream()
//                                .map(r -> r.getPatient().getId())
//                                .collect(Collectors.toSet())
//                                .containsAll(List.of(patient1.getId(), patient2.getId()))
//                )
//        );
    }

    @Test
    void getDispositionsForAllPatients_ignoresGraduated() {
        DPatient patient1 = new DPatient();
        patient1.save();
        DPatientDisposition disposition11 = new DPatientDisposition()
                .setFlag(DispositionFlag.CONNECTED_TO_CARE)
                .setPatient(patient1);
        disposition11.save();
        DPatient patient2 = new DPatient();
        patient1.save();
        DPatientDisposition disposition21 = new DPatientDisposition()
                .setFlag(DispositionFlag.GRADUATED)
                .setPatient(patient2);
        disposition21.save();
//        List<DPatientDisposition> responses = DispositionService.getDispositionsForAllPatients();
//        assertAll("dispositions",
//                () -> assertEquals(0, responses.size()),
//                () -> assertEquals(disposition11.getId(), responses.get(0).getId())
//        );
    }

    @Test
    void getLatestDispositionForPatient() {
        DPatient patient = new DPatient();
        patient.save();
        DPatientDisposition disposition1 = new DPatientDisposition()
                .setFlag(DispositionFlag.CONNECTED_TO_CARE)
                .setPatient(patient);
        disposition1.save();
        DPatientDisposition disposition2 = new DPatientDisposition()
                .setFlag(DispositionFlag.CONNECTED_TO_CARE)
                .setPatient(patient);
        disposition2.save();
        //Optional<DPatientDisposition> disposition = DispositionService.getLatestDispositionForPatient(patient.getId());
        //assertEquals(disposition2.getId(), disposition.get().getId());
    }

    @Test
    void getLatestDispositionForPatient_ignoresGraduated() {
        DPatient patient = new DPatient();
        patient.save();
        DPatientDisposition disposition1 = new DPatientDisposition()
                .setFlag(DispositionFlag.GRADUATED)
                .setPatient(patient);
        disposition1.save();
        Optional<DPatientDisposition> disposition = DispositionService.getLatestDispositionForPatient(patient.getId());
        assertTrue(disposition.isEmpty());
    }

    @Test
    void testGetDispositionsForAllPatients() {
        DPatient patient1 = new DPatient();
        patient1.save();
        DPatientDisposition disposition11 = new DPatientDisposition()
                .setFlag(DispositionFlag.GRADUATED)
                .setPatient(patient1);
        disposition11.save();
        DPatientDisposition disposition12 = new DPatientDisposition()
                .setFlag(DispositionFlag.CONNECTED_TO_CARE)
                .setPatient(patient1);
        disposition12.save();
        DPatient patient2 = new DPatient();
        patient2.save();
        DPatientDisposition disposition21 = new DPatientDisposition()
                .setFlag(DispositionFlag.CONNECTED_TO_CARE)
                .setPatient(patient2);
        disposition21.save();
        DPatient patient3 = new DPatient();
        patient2.save();
        DPatientDisposition disposition31 = new DPatientDisposition()
                .setFlag(DispositionFlag.GRADUATED)
                .setPatient(patient2);
        disposition31.save();
//        Set<UUID> dispositionIds = DispositionService.getDispositionsForAllPatients().stream().map(DBaseModel::getId).collect(Collectors.toSet());
//        assertEquals(Set.of(disposition12.getId(), disposition21.getId()), dispositionIds);
    }
}