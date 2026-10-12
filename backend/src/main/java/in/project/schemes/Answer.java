package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="profile_answers") class Answer {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public Long userId;
public Long questionId;
@Column(columnDefinition="json") public String valueJson;
}
