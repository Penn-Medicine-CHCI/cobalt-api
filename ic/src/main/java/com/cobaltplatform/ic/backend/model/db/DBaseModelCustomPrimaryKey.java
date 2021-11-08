package com.cobaltplatform.ic.backend.model.db;

import java.util.UUID;
import javax.persistence.MappedSuperclass;

import io.ebean.Model;
import io.ebean.annotation.SoftDelete;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import org.joda.time.DateTime;

@MappedSuperclass
public class DBaseModelCustomPrimaryKey extends Model {

    UUID id;
    @WhenCreated
    DateTime createdDt;
    @WhenModified
    DateTime updatedDt;
    @SoftDelete
    boolean deleted;

    public UUID getId() {
        return id;
    }

    public DBaseModelCustomPrimaryKey setId(final UUID id) {
        this.id = id;
        return this;
    }

    public DateTime getCreatedDt() {
        return createdDt;
    }

    public DBaseModelCustomPrimaryKey setCreatedDt(final DateTime createdDt) {
        this.createdDt = createdDt;
        return this;
    }

    public DateTime getUpdatedDt() {
        return updatedDt;
    }

    public DBaseModelCustomPrimaryKey setUpdatedDt(final DateTime updatedDt) {
        this.updatedDt = updatedDt;
        return this;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public DBaseModelCustomPrimaryKey setDeleted(final Boolean deleted) {
        this.deleted = deleted;
        return this;
    }
}

