package in.project.schemes;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import jakarta.persistence.*;
import java.time.Instant;
@Component
class SecurityEvents {
 @PersistenceContext EntityManager em;
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void rejected(String action){Audit a=new Audit();a.action=action;a.targetId="AUTH";a.createdAt=Instant.now().toString();em.persist(a);}
}
