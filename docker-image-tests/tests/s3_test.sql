DROP TABLE IF EXISTS hive.default.users;

CREATE TABLE hive.default.users (
  id INT,
  name VARCHAR
)
WITH (
  external_location = 's3a://warehouse/users/',
  format = 'PARQUET'
);

INSERT INTO hive.default.users VALUES
  (1, 'Alice'),
  (2, 'Bob'),
  (3, 'Charlie');

SELECT * FROM hive.default.users ORDER BY id;
