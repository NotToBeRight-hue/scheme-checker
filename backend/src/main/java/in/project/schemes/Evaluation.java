package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="eligibility_evaluations") class Evaluation {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public Long userId;
public Long schemeId;
public Long ruleVersionId;
public Long profileRevision;
public String evaluationDate;
public String verdict;
public String evaluatedAt;
public Boolean dateSensitive;
@Column(columnDefinition="json") public String snapshotJson;
}
