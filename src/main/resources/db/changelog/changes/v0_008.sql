--liquibase formatted sql
--changeset friasoft:008-class-timetable-slots
--comment: Créneaux d’emploi du temps par classe (jour + index de période).

CREATE TABLE schools.class_timetable_slots (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT REFERENCES schools.tenants(id),
    class_id BIGINT NOT NULL REFERENCES schools.school_classes(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL,
    slot_index SMALLINT NOT NULL,
    class_subject_id BIGINT NOT NULL REFERENCES schools.class_subjects(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_timetable_cell UNIQUE (class_id, day_of_week, slot_index),
    CONSTRAINT chk_timetable_day CHECK (day_of_week >= 1 AND day_of_week <= 5),
    CONSTRAINT chk_timetable_slot CHECK (slot_index >= 0 AND slot_index <= 7)
);

CREATE INDEX idx_timetable_slots_class_id ON schools.class_timetable_slots(class_id);
CREATE INDEX idx_timetable_slots_tenant_id ON schools.class_timetable_slots(tenant_id);
