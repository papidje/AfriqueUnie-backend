package friasoft.gn.schoolapp.repository.messaging;

import friasoft.gn.schoolapp.entity.messaging.MessagingMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessagingMessageRepository extends JpaRepository<MessagingMessage, Long> {

    @Query("""
        SELECT m FROM MessagingMessage m
        WHERE m.conversation.id = :conversationId
        AND (:afterId IS NULL OR m.id > :afterId)
        AND (:beforeId IS NULL OR m.id < :beforeId)
        ORDER BY m.id ASC
        """)
    List<MessagingMessage> findPageAscending(
        @Param("conversationId") Long conversationId,
        @Param("afterId") Long afterId,
        @Param("beforeId") Long beforeId,
        Pageable pageable
    );

    @Query("""
        SELECT m FROM MessagingMessage m
        WHERE m.conversation.id = :conversationId
        AND (:beforeId IS NULL OR m.id < :beforeId)
        ORDER BY m.id DESC
        """)
    List<MessagingMessage> findNewestBefore(
        @Param("conversationId") Long conversationId,
        @Param("beforeId") Long beforeId,
        Pageable pageable
    );
}
