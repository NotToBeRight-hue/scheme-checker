package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="questions") class Question {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public String label;
public String type;
@Column(columnDefinition="json") public String optionsJson;
public Boolean active;
public Long revision;
}
