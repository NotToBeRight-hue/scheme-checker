package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="users") class User {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public String fullName;
public String email;
public String passwordHash;
public String role;
public String status;
}
