-- Local integration database only. The source .env uses schemes_app as MYSQL_USER.
CREATE DATABASE IF NOT EXISTS schemes_test;
GRANT ALL PRIVILEGES ON schemes_test.* TO 'schemes_app'@'%';
