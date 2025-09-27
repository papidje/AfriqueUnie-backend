package friasoft.gn.schoolapp.exception.handler;

import friasoft.gn.schoolapp.exception.service.ExceptionHandlerService;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GlobalEcxceptionHandler {
    private final ExceptionHandlerService exceptionHandlerService;

    GlobalEcxceptionHandler(ExceptionHandlerService exceptionHandlerService) {
        this.exceptionHandlerService = exceptionHandlerService;
    }
}
