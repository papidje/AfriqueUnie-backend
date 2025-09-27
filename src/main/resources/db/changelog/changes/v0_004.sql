

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

create table schools.students (
                                  id BIGSERIAL PRIMARY KEY,
                                  birth_date date,
                                  adress varchar(255),
                                  civility varchar(255),
                                  first_name varchar(255),
                                  last_name varchar(255),
                                  matricule varchar(255)
);