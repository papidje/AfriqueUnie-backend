package friasoft.gn.schoolapp.dto;

/**
 * Parents ayant au moins un enfant inscrit dans une classe de l’année scolaire active de l’établissement.
 * DTO de premier niveau pour {@code select new} en JPQL (les classes internes ne sont pas résolues par Hibernate).
 */
public record ParentSchoolListRow(
    Long id,
    String lastName,
    String firstName,
    String phone,
    String email,
    Long enrolledChildrenCount
) {}
