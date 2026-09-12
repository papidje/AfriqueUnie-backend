package friasoft.gn.schoolapp.dto.request;

import java.time.LocalDate;

/**
 * Corps optionnel pour {@code PATCH /super-admin/tenants/{id}/active/{active}}.
 * {@code subscriptionEndsOn} est obligatoire à l’activation si le tenant a plus de 100 élèves.
 */
public record TenantActiveUpdateRequest(LocalDate subscriptionEndsOn) {
}
