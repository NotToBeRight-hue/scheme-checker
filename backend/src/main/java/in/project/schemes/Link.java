package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="scheme_questions") class Link {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public Long schemeId;
public Long questionId;
}
