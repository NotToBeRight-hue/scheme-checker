package in.project.schemes;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

class ApiFailure extends RuntimeException {final int status;final String code;final Object details;ApiFailure(int s,String c,String m){this(s,c,m,Map.of());}ApiFailure(int s,String c,String m,Object d){super(m);status=s;code=c;details=d;}}
@RestControllerAdvice
class Errors {
 @ExceptionHandler(ApiFailure.class) ResponseEntity<?> fail(ApiFailure e){return response(e.status,e.code,e.getMessage(),e.details);}
 @ExceptionHandler({java.time.DateTimeException.class,IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class}) ResponseEntity<?> bad(Exception e){return response(400,"VALIDATION_ERROR",e instanceof IllegalArgumentException?e.getMessage():"Invalid request body",Map.of());}
 @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,jakarta.persistence.OptimisticLockException.class}) ResponseEntity<?> conflict(Exception e){return response(409,"CONFLICT","Resource changed or already exists",Map.of());}
 @ExceptionHandler(Exception.class) ResponseEntity<?> unknown(Exception e){org.slf4j.LoggerFactory.getLogger(Errors.class).error("Request failed: {}",e.getClass().getSimpleName());return response(500,"INTERNAL_ERROR","Unable to complete request",Map.of());}
 static ResponseEntity<?> response(int status,String code,String message,Object details){return ResponseEntity.status(status).body(Map.of("error",Map.of("code",code,"message",message,"details",details),"requestId",UUID.randomUUID().toString()));}
}

