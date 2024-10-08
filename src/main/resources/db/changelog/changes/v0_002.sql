INSERT INTO schools.role (id, name)
values  (1, 'ADMINISTRATOR'),
        (2, 'DIRECTOR'),
        (3, 'ACCOUNTER'),
        (4, 'INSTRUCTOR');

INSERT INTO schools.class_group (id, name)
values  (1, 'Maternel'),
        (2, 'Primaire'),
        (3, 'College'),
        (4, 'Lycée'),
        (5, 'Université');


INSERT INTO schools.grade (id, name, class_group_id)
values  (1, 'Petite Section', 1),
        (2, 'Moyenne Section', 1),
        (3, 'Grande Section', 1),
        (4, 'CP1', 2),
        (5, 'CP2', 2),
        (6, 'CE1', 2),
        (7, 'CE2', 2),
        (8, 'CM1', 2),
        (9, 'CM2', 2),
        (10, '7 eme', 3),
        (11, '8 eme', 3),
        (12, '9 eme', 3),
        (13, '10 eme', 3),
        (14, '11 eme', 4),
        (15, '12 eme', 4),
        (16, 'Terminal', 4),
        (17, 'L1', 5),
        (18, 'L2', 5),
        (19, 'L3', 5),
        (20, 'M1', 5),
        (21, 'M2', 5);
        