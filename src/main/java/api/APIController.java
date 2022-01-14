package api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.ResponseEntity;

public class APIController {
    public static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    public static final String apiURL = "http://localhost:8080/";
    public static final ResponseEntity<String> BAD_REQUEST = ResponseEntity.badRequest().build(),
            OK_REQUEST = ResponseEntity.ok().build();
}
