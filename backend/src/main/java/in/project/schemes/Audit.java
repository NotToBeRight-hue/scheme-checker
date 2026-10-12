package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="admin_audit_logs") class Audit {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public Long actorId;
public String action;
public String targetId;
public String createdAt;
}
