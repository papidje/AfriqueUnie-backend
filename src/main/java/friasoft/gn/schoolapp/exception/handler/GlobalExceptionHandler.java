package friasoft.gn.schoolapp.exception.handler;

import friasoft.gn.schoolapp.exception.service.ExceptionHandlerService;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GlobalExceptionHandler {
    private final ExceptionHandlerService exceptionHandlerService;

    GlobalExceptionHandler(ExceptionHandlerService exceptionHandlerService) {
        this.exceptionHandlerService = exceptionHandlerService;
    }
}
