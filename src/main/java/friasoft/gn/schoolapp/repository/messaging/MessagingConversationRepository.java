package friasoft.gn.schoolapp.repository.messaging;

import friasoft.gn.schoolapp.entity.messaging.MessagingConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessagingConversationRepository extends JpaRepository<MessagingConversation, Long> {

    @Query("""
        SELECT c FROM MessagingConversation c
        JOIN MessagingParticipant p1 ON p1.conversation = c AND p1.user.id = :userA
        JOIN MessagingParticipant p2 ON p2.conversation = c AND p2.user.id = :userB
        WHERE (
            SELECT COUNT(p) FROM MessagingParticipant p WHERE p.conversation = c
        ) = 2
        """)
    Optional<MessagingConversation> findOneToOneBetween(
        @Param("userA") Long userA,
        @Param("userB") Long userB
    );

    @Query("""
        SELECT DISTINCT c FROM MessagingConversation c
        JOIN MessagingParticipant me ON me.conversation = c AND me.user.id = :userId
        JOIN MessagingParticipant other ON other.conversation = c AND other.user.id <> :userId
        WHERE (:q IS NULL OR :q = '' OR LOWER(other.user.fullname) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(COALESCE(c.lastMessagePreview, '')) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.id DESC
        """)
    List<MessagingConversation> findForUserFiltered(
        @Param("userId") Long userId,
        @Param("q") String q
    );
}
