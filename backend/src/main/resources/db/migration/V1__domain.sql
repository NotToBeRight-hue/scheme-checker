CREATE TABLE users (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

full_name VARCHAR(255),

email VARCHAR(255),

password_hash VARCHAR(255),

role VARCHAR(255),

status VARCHAR(255)

) ENGINE=InnoDB;

CREATE TABLE user_profiles (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

user_id BIGINT UNSIGNED,

date_of_birth VARCHAR(255),

state_code VARCHAR(255),

annual_income DECIMAL(18,2),

education_level VARCHAR(255),

occupation VARCHAR(255),

revision BIGINT UNSIGNED

) ENGINE=InnoDB;

CREATE TABLE profile_answers (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

user_id BIGINT UNSIGNED,

question_id BIGINT UNSIGNED,

value_json JSON

) ENGINE=InnoDB;

CREATE TABLE scheme_categories (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

name VARCHAR(255)

) ENGINE=InnoDB;

CREATE TABLE schemes (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

name VARCHAR(255),

category_id BIGINT UNSIGNED,

state_code VARCHAR(255),

description TEXT,

benefits TEXT,

guidance TEXT,

source_url VARCHAR(255),

verified_date VARCHAR(255),

synthetic BOOLEAN,

reviewer_approved BOOLEAN,

source_mapping JSON,

exceptions TEXT,

published_version_id BIGINT UNSIGNED,

revision BIGINT UNSIGNED

) ENGINE=InnoDB;

CREATE TABLE questions (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

label VARCHAR(255),

type VARCHAR(255),

options_json JSON,

active BOOLEAN,

revision BIGINT UNSIGNED

) ENGINE=InnoDB;

CREATE TABLE scheme_questions (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

scheme_id BIGINT UNSIGNED,

question_id BIGINT UNSIGNED

) ENGINE=InnoDB;

CREATE TABLE scheme_rule_versions (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

scheme_id BIGINT UNSIGNED,

status VARCHAR(255),

rule_json JSON,

revision BIGINT UNSIGNED,

created_at VARCHAR(255)

) ENGINE=InnoDB;

CREATE TABLE eligibility_evaluations (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

user_id BIGINT UNSIGNED,

scheme_id BIGINT UNSIGNED,

rule_version_id BIGINT UNSIGNED,

profile_revision BIGINT UNSIGNED,

evaluation_date VARCHAR(255),

verdict VARCHAR(255),

evaluated_at VARCHAR(255),

date_sensitive BOOLEAN,

snapshot_json JSON

) ENGINE=InnoDB;

CREATE TABLE admin_audit_logs (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

actor_id BIGINT UNSIGNED,

action VARCHAR(255),

target_id VARCHAR(255),

created_at VARCHAR(255)

) ENGINE=InnoDB;

CREATE TABLE scheme_suggestions (

id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,

user_id BIGINT UNSIGNED,

type VARCHAR(255),

scheme_id BIGINT UNSIGNED,

scheme_name VARCHAR(255),

description TEXT,

proposed_criterion TEXT,

source_url VARCHAR(255),

status VARCHAR(255),

review_notes TEXT,

revision BIGINT UNSIGNED,

created_at VARCHAR(255)

) ENGINE=InnoDB;

ALTER TABLE users MODIFY email VARCHAR(200) NOT NULL, MODIFY password_hash VARCHAR(255) NOT NULL, MODIFY role VARCHAR(20) NOT NULL, MODIFY status VARCHAR(20) NOT NULL, ADD CHECK(role IN ('ADMIN','CITIZEN')), ADD CHECK(status IN ('ACTIVE','DISABLED'));
ALTER TABLE user_profiles MODIFY user_id BIGINT UNSIGNED NOT NULL, MODIFY revision BIGINT UNSIGNED NOT NULL;
ALTER TABLE profile_answers MODIFY user_id BIGINT UNSIGNED NOT NULL, MODIFY question_id BIGINT UNSIGNED NOT NULL, MODIFY value_json JSON NOT NULL;
ALTER TABLE scheme_categories MODIFY name VARCHAR(255) NOT NULL, ADD UNIQUE(name);
ALTER TABLE scheme_rule_versions MODIFY scheme_id BIGINT UNSIGNED NOT NULL, MODIFY rule_json JSON NOT NULL, MODIFY revision BIGINT UNSIGNED NOT NULL, MODIFY status VARCHAR(20) NOT NULL, ADD CHECK(status IN ('DRAFT','PUBLISHED'));
ALTER TABLE eligibility_evaluations MODIFY user_id BIGINT UNSIGNED NOT NULL, MODIFY scheme_id BIGINT UNSIGNED NOT NULL, MODIFY rule_version_id BIGINT UNSIGNED NOT NULL, MODIFY snapshot_json JSON NOT NULL, MODIFY profile_revision BIGINT UNSIGNED NOT NULL, ADD CHECK(verdict IN ('ELIGIBLE','NOT_ELIGIBLE'));
ALTER TABLE scheme_suggestions MODIFY user_id BIGINT UNSIGNED NOT NULL, MODIFY revision BIGINT UNSIGNED NOT NULL, ADD CHECK(status IN ('PENDING','UNDER_REVIEW','NEEDS_INFO','ACCEPTED','REJECTED')), ADD CHECK(type IN ('SCHEME','CRITERION'));
ALTER TABLE users ADD CONSTRAINT uq_user_email UNIQUE(email);

ALTER TABLE user_profiles ADD CONSTRAINT uq_profile_user UNIQUE(user_id), ADD FOREIGN KEY(user_id) REFERENCES users(id);

ALTER TABLE profile_answers ADD CONSTRAINT uq_answer UNIQUE(user_id,question_id), ADD FOREIGN KEY(user_id) REFERENCES users(id), ADD FOREIGN KEY(question_id) REFERENCES questions(id);

ALTER TABLE schemes ADD FOREIGN KEY(category_id) REFERENCES scheme_categories(id);

ALTER TABLE scheme_questions ADD CONSTRAINT uq_link UNIQUE(scheme_id,question_id), ADD FOREIGN KEY(scheme_id) REFERENCES schemes(id), ADD FOREIGN KEY(question_id) REFERENCES questions(id);

ALTER TABLE scheme_rule_versions ADD FOREIGN KEY(scheme_id) REFERENCES schemes(id);

ALTER TABLE schemes ADD FOREIGN KEY(published_version_id) REFERENCES scheme_rule_versions(id);

ALTER TABLE eligibility_evaluations ADD FOREIGN KEY(user_id) REFERENCES users(id), ADD FOREIGN KEY(scheme_id) REFERENCES schemes(id), ADD FOREIGN KEY(rule_version_id) REFERENCES scheme_rule_versions(id), ADD INDEX ix_history(user_id,id);

ALTER TABLE admin_audit_logs ADD FOREIGN KEY(actor_id) REFERENCES users(id);

ALTER TABLE scheme_suggestions ADD FOREIGN KEY(user_id) REFERENCES users(id), ADD FOREIGN KEY(scheme_id) REFERENCES schemes(id), ADD INDEX ix_suggestions(user_id,id);

ALTER TABLE user_profiles ADD CONSTRAINT chk_income CHECK(annual_income IS NULL OR annual_income >= 0);








CREATE TRIGGER immutable_evaluation_update BEFORE UPDATE ON eligibility_evaluations FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Evaluation snapshots are immutable';

CREATE TRIGGER immutable_evaluation_delete BEFORE DELETE ON eligibility_evaluations FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Evaluation snapshots are immutable';

CREATE TRIGGER immutable_published_rule_update BEFORE UPDATE ON scheme_rule_versions FOR EACH ROW BEGIN IF OLD.status = 'PUBLISHED' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Published rules are immutable'; END IF; END;

CREATE TRIGGER immutable_published_rule_delete BEFORE DELETE ON scheme_rule_versions FOR EACH ROW BEGIN IF OLD.status = 'PUBLISHED' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Published rules are immutable'; END IF; END;

