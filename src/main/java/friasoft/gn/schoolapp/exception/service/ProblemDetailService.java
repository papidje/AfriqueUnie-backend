package friasoft.gn.schoolapp.exception.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProblemDetailService {
    private static final String ERROR_GLOBAL = "error.";
    private final MessageSource messageSource;

    ProblemDetailService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public ProblemDetail createProblemDetail(String problemId) {
        return null;
    }
}
