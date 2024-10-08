CREATE SCHEMA IF NOT EXISTS schools;

create table schools.class_group (
  id integer not null,
  name varchar(255),
  primary key (id)
);

create table schools.contact (
  id smallint not null,
  backup_mail varchar(255),
  backup_phone varchar(255),
  prime_mail varchar(255),
  prime_phone varchar(255),
  primary key (id)
);

create table schools.grade (
  class_group_id integer references schools.class_group,
  id integer not null,
  name varchar(255),
  primary key (id)
);

create table schools.payment (
  amount float(53) not null,
  id integer not null,
  rest_to_pay float(53) not null,
  type varchar(255),
  primary key (id)
);

create table schools.promotion (
  grade_id integer references schools.grade,
  id smallint not null,
  name varchar(255),
  primary key (id)
);

create table schools.responsible (
  contact_id smallint unique references schools.contact,
  id smallint not null,
  adress varchar(255),
  civility varchar(255),
  first_name varchar(255),
  last_name varchar(255),
  primary key (id)
);

create table schools.role (
  id smallint not null,
  name varchar(255),
  primary key (id)
);

create table schools.school (
  id smallint not null,
  open_date date,
  adress varchar(255),
  logo varchar(255),
  name varchar(255),
  primary key (id)
);

create table schools.staff (
  id smallint not null,
  is_active boolean not null,
  role_id smallint references schools.role,
  school_id smallint references schools.school,
  email varchar(255),
  name varchar(255),
  password varchar(255),
  primary key (id)
);

create table schools.student (
  birth_date date,
  id integer not null,
  adress varchar(255),
  civility varchar(255),
  first_name varchar(255),
  last_name varchar(255),
  matricule varchar(255),
  primary key (id)
);

create table schools.activation (
  id integer not null,
  school_id smallint references schools.school,
  user_id smallint references schools.staff,
  registration_date date,
  code varchar(6) not null,
  primary key (id)
);