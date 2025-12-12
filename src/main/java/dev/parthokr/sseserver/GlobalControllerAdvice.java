package dev.parthokr.sseserver;


import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(exception = BadCredentialsException.class)
    public Response handleBadCredentialsException(BadCredentialsException ex) {
        return new Response("Invalid credentials: " + ex.getMessage());
    }

    static class Response {
        private String message;

        public Response(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

}
