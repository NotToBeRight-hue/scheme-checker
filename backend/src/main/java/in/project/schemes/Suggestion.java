package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="scheme_suggestions") class Suggestion {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public Long userId;
public String type;
public Long schemeId;
public String schemeName;
@Column(columnDefinition="text") public String description;
@Column(columnDefinition="text") public String proposedCriterion;
public String sourceUrl;
public String status;
@Column(columnDefinition="text") public String reviewNotes;
public Long revision;
public String createdAt;
}
