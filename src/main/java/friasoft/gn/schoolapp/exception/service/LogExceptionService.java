package friasoft.gn.schoolapp.exception.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class LogExceptionService {
    public static final String ERROR_ID_MESSAGE = "error id";

    void logExceptionWithIssueId(String message, Throwable e, String uuid) {
        String msg;
        message = Optional.ofNullable(message).map(m -> m + " (" + ERROR_ID_MESSAGE + " " + uuid + ")").orElse(ERROR_ID_MESSAGE + " " + uuid);
        log.error(message, e);
    }

    void logException(String message, Throwable e) {
        log.error(message, e);
    }
}
