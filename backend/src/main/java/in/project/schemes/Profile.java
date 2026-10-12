package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="user_profiles") class Profile {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public Long userId;
public String dateOfBirth;
public String stateCode;
public java.math.BigDecimal annualIncome;
public String educationLevel;
public String occupation;
public Long revision;
}
