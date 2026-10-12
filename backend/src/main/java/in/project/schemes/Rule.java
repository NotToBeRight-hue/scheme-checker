package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="scheme_rule_versions") class Rule {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public Long schemeId;
public String status;
@Column(columnDefinition="json") public String ruleJson;
public Long revision;
public String createdAt;
}
