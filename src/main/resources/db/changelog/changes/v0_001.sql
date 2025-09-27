CREATE SCHEMA IF NOT EXISTS schools;

create table schools.roles (
                               id BIGSERIAL PRIMARY KEY,
                               name varchar(255)
);

create table schools.schools (
                                 id BIGSERIAL PRIMARY KEY,
                                 name varchar,
                                 adress varchar,
                                 contact varchar,
                                 open_date date,
                                 logo varchar,
                                 created_at timestamp,
                                 updated_at timestamp
);

create table schools.users (
                               id BIGSERIAL PRIMARY KEY,
                               fullname varchar(255),
                               username varchar(255),
                               email varchar(255),
                               password varchar(255),
                               is_active boolean not null,
                               created_at timestamp,
                               updated_at timestamp,
                               school_id smallint references schools.schools,
                               last_login_at timestamp
);

CREATE TABLE schools.users_roles (
                                     user_id BIGINT NOT NULL,
                                     role_id BIGINT NOT NULL,
                                     PRIMARY KEY (user_id, role_id),
                                     CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES schools.users(id),
                                     CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES schools.roles(id)
);

create table schools.activations (
                                     id BIGSERIAL PRIMARY KEY,
                                     user_id smallint references schools.users,
                                     registration_date timestamp,
                                     code varchar(6) not null,
                                     expiration timestamp
);

create table schools.refresh_token (
                                       id BIGSERIAL PRIMARY KEY,
                                       expired boolean not null,
                                       value varchar(255),
                                       creation timestamp not null,
                                       expiration timestamp not null
);

create table schools.jwts (
                              id BIGSERIAL PRIMARY KEY,
                              jwt varchar,
                              is_active boolean not null,
                              is_expired boolean not null,
                              created_at timestamp,
                              last_login_at timestamp,
                              user_id smallint references schools.users,
                              refresh_token_id smallint references schools.refresh_token
);

INSERT INTO schools.roles (id, name)
values  (1, 'SUPER_ADMIN'),
        (2, 'ADMINISTRATOR'),
        (3, 'ACCOUNTER'),
        (4, 'TEACHER');