--liquibase formatted sql
--changeset friasoft:003-audit-function splitStatements:false stripComments:false runOnChange:true
CREATE OR REPLACE FUNCTION schools.audit_trigger_fn()
RETURNS TRIGGER AS $func$
DECLARE
    v_old JSONB;
    v_new JSONB;
    v_action VARCHAR(10);
    v_id_column TEXT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_old := to_jsonb(OLD);
        v_new := NULL;
        v_action := 'DELETE';
    ELSIF TG_OP = 'UPDATE' THEN
        v_old := to_jsonb(OLD);
        v_new := to_jsonb(NEW);
        v_action := 'UPDATE';
    ELSIF TG_OP = 'INSERT' THEN
        v_old := NULL;
        v_new := to_jsonb(NEW);
        v_action := 'CREATE';
    END IF;

    IF TG_TABLE_NAME = 'fees' THEN
        v_id_column := 'fee_id';
    ELSIF TG_TABLE_NAME = 'enrollments' THEN
        v_id_column := 'enrollment_id';
    ELSIF TG_TABLE_NAME = 'payments' THEN
        v_id_column := 'payment_id';
    ELSE
        RAISE EXCEPTION 'Table % non supportée pour audit', TG_TABLE_NAME;
    END IF;

    EXECUTE format($exec$
        INSERT INTO schools.%I_audit (%I, action, old_data, new_data, user_id)
        VALUES ($1, $2, $3, $4, current_setting('app.current_user_id', true)::BIGINT)
    $exec$, TG_TABLE_NAME, v_id_column)
    USING COALESCE(NEW.id, OLD.id), v_action, v_old, v_new;

    RETURN NEW;
END;
$func$ LANGUAGE plpgsql;

--changeset friasoft:003-audit-trigger-fees runOnChange:true
DROP TRIGGER IF EXISTS trg_audit_fees ON schools.fees;
CREATE TRIGGER trg_audit_fees
    AFTER INSERT OR UPDATE OR DELETE ON schools.fees
    FOR EACH ROW EXECUTE FUNCTION schools.audit_trigger_fn();

--changeset friasoft:003-audit-trigger-enrollments runOnChange:true
DROP TRIGGER IF EXISTS trg_audit_enrollments ON schools.enrollments;
CREATE TRIGGER trg_audit_enrollments
    AFTER INSERT OR UPDATE OR DELETE ON schools.enrollments
    FOR EACH ROW EXECUTE FUNCTION schools.audit_trigger_fn();

--changeset friasoft:003-audit-trigger-payments runOnChange:true
DROP TRIGGER IF EXISTS trg_audit_payments ON schools.payments;
CREATE TRIGGER trg_audit_payments
    AFTER INSERT OR UPDATE OR DELETE ON schools.payments
    FOR EACH ROW EXECUTE FUNCTION schools.audit_trigger_fn();
