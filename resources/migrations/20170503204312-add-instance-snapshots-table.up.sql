CREATE TABLE instance_snapshots (
id SERIAL PRIMARY KEY,
snapshot_id int NOT NULL,
instance_type VARCHAR(255),
instance_id VARCHAR(255),
project VARCHAR(255),
name VARCHAR(255),
stages VARCHAR(255),
availability_zone VARCHAR(255) 
);
