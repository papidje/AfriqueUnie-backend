CREATE SCHEMA IF NOT EXISTS schools;

create table schools.class_groups (
  id BIGSERIAL PRIMARY KEY,
  name varchar(255)
);

create table schools.contacts (
  id BIGSERIAL PRIMARY KEY,
  backup_mail varchar(255),
  backup_phone varchar(255),
  prime_mail varchar(255),
  prime_phone varchar(255)
);

create table schools.grades (
  id BIGSERIAL PRIMARY KEY,
  class_group_id integer references schools.class_groups,
  name varchar(255)
);

create table schools.payments (
  id BIGSERIAL PRIMARY KEY,
  amount float(53) not null,
  rest_to_pay float(53) not null,
  type varchar(255)
);

create table schools.promotions (
  id BIGSERIAL PRIMARY KEY,
  grade_id integer references schools.grades,
  name varchar(255)
);

create table schools.responsibles (
  id BIGSERIAL PRIMARY KEY,
  contact_id smallint unique references schools.contacts,
  adress varchar(255),
  civility varchar(255),
  first_name varchar(255),
  last_name varchar(255)
);

create table schools.roles (
  id BIGSERIAL PRIMARY KEY,
  name varchar(255)
);

create table schools.schools (
  id BIGSERIAL PRIMARY KEY,
  open_date date,
  adress varchar(255),
  logo varchar(255),
  name varchar(255)
);

create table schools.users (
  id BIGSERIAL PRIMARY KEY,
  is_active boolean not null,
  school_id smallint references schools.schools,
  email varchar(255),
  name varchar(255),
  username varchar(255),
  password varchar(255)
);

create table schools.students (
  id BIGSERIAL PRIMARY KEY,
  birth_date date,
  adress varchar(255),
  civility varchar(255),
  first_name varchar(255),
  last_name varchar(255),
  matricule varchar(255)
);

create table schools.activations (
  id BIGSERIAL PRIMARY KEY,
  school_id smallint references schools.schools,
  user_id smallint references schools.users,
  registration_date date,
  code varchar(6) not null
);

CREATE TABLE schools.users_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES schools.users(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES schools.roles(id)
);
