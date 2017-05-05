-- name: create-user!
-- creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- name: update-user!
-- update an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- name: get-user
-- retrieve a user given the id.
SELECT * FROM users
WHERE id = :id

-- name: delete-user!
-- delete a user given the id
DELETE FROM users
WHERE id = :id

-- name: create-cost-snapshot!
-- creates a new cost snapshot record
INSERT INTO cost_snapshots
(
snapshot_id,
record_type,
record_id,
billing_period_start_date,
billing_period_end_date,
invoice_date,
product_code,
product_name,
usage_type,
operation,
rate_id,
item_description,
usage_start_date,
usage_end_date,
usage_quantity,
blended_rate,
cost_before_tax,
credits,
total_cost,
created_at
)
VALUES
(
:snapshot_id,
:record_type,
:record_id,
:billing_period_start_date,
:billing_period_end_date,
:invoice_date,
:product_code,
:product_name,
:usage_type,
:operation,
:rate_id,
:item_description,
:usage_start_date,
:usage_end_date,
:usage_quantity,
:blended_rate,
:cost_before_tax,
:credits,
:total_cost,
:created_at
)

-- name: create-instance-snapshot!
-- creates a new instance snapshot record
INSERT INTO instance_snapshots (
snapshot_id,
instance_type,
instance_id,
project,
name,
stages,
availability_zone
)
VALUES
(
:snapshot_id,
:instance_type,
:instance_id,
:project,
:name,
:stages,
:availability_zone
)

-- name: find-instance-snapshot
-- finds a single snapshot
SELECT instance_snapshots.*
FROM instance_snapshots
WHERE id = :id
ORDER by id DESC
LIMIT 1
