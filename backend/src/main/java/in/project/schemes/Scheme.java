package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="schemes") class Scheme {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public String name;
public Long categoryId;
public String stateCode;
@Column(columnDefinition="text") public String description;
@Column(columnDefinition="text") public String benefits;
@Column(columnDefinition="text") public String guidance;
public String sourceUrl;
public String verifiedDate;
public Boolean synthetic;
public Boolean reviewerApproved;
@Column(columnDefinition="json") public String sourceMapping;
@Column(columnDefinition="text") public String exceptions;
public Long publishedVersionId;
public Long revision;
}
