package in.project.schemes;
import jakarta.persistence.*;
@Entity @Table(name="scheme_categories") class Category {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
public String name;
}
