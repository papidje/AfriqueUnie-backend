package friasoft.gn.schoolapp.dto;

/**
 * Corps {@code POST /auth/switch-school} — établissement cible pour régénérer le JWT.
 */
public record SwitchSchoolRequest(Long schoolId) {}
