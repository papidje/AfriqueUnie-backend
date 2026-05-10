package friasoft.gn.schoolapp.service.communication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockSmsService {

    public void sendSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.debug("[SMS MOCK] Ignoré — numéro vide.");
            return;
        }
        String preview = message == null ? "" : message.substring(0, Math.min(message.length(), 120));
        log.info("[SMS MOCK] → {} | {}", phoneNumber.trim(), preview);
    }
}
