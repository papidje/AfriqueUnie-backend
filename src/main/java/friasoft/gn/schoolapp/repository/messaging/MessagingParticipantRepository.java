package friasoft.gn.schoolapp.repository.messaging;

import friasoft.gn.schoolapp.entity.messaging.MessagingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessagingParticipantRepository extends JpaRepository<MessagingParticipant, Long> {

    List<MessagingParticipant> findByConversation_Id(Long conversationId);

    Optional<MessagingParticipant> findByConversation_IdAndUser_Id(Long conversationId, Long userId);

    boolean existsByConversation_IdAndUser_Id(Long conversationId, Long userId);

    @Query("""
        SELECT COUNT(DISTINCT me.conversation.id) FROM MessagingParticipant me
        JOIN MessagingMessage m ON m.conversation = me.conversation
        WHERE me.user.id = :userId
        AND m.sender.id <> :userId
        AND (me.lastReadAt IS NULL OR m.createdAt > me.lastReadAt)
        """)
    long countConversationsWithUnread(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(m) FROM MessagingMessage m
        JOIN MessagingParticipant me ON me.conversation = m.conversation AND me.user.id = :userId
        WHERE m.conversation.id = :conversationId
        AND m.sender.id <> :userId
        AND (me.lastReadAt IS NULL OR m.createdAt > me.lastReadAt)
        """)
    long countUnreadInConversation(
        @Param("userId") Long userId,
        @Param("conversationId") Long conversationId
    );
}
