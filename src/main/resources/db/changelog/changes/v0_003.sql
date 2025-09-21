create table schools.jwts (
  id BIGSERIAL PRIMARY KEY,
  jwt varchar(255),
  is_active boolean not null,
  is_expired boolean not null,
  user_id smallint references schools.users
);