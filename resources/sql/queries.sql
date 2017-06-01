-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc update an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieve a user given the id.
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc delete a user given the id
DELETE FROM users
WHERE id = :id

-- :name create-cost-snapshot! :! :n
-- :doc creates a new cost snapshot record
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
total_cost
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
:total_cost
)

-- :name create-instance-snapshot :! :n
-- :doc creates a new instance snapshot record
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

-- :name find-instance-snapshot :? :1
-- :doc finds a single instance snapshot
SELECT instance_snapshots.*
FROM instance_snapshots
WHERE id = :id
ORDER by id DESC
LIMIT 1

-- :name find-cost-snapshot :? :1
-- :doc finds a single cost snapshot
SELECT cost_snapshots.*
FROM cost_snapshots
WHERE id = :id
ORDER by id DESC
LIMIT 1

-- :name create-snapshot :n
-- :doc creates a new snapshot record with a returning clause
INSERT INTO snapshots (table_name, snapshot_id, title)
VALUES (:table_name, :snapshot_id, :title) returning id

-- :name find-snapshot :? :n
-- :doc creates a new cost snapshot record
SELECT snapshots.*
FROM snapshots.*
WHERE id = :id
ORDER by id DESC
LIMIT 1

-- :name create-instance-snapshots :! :n
-- Insert multiple characters with :tuple* parameter type
INSERT INTO instance_snapshots (
snapshot_id,
instance_type,
instance_id,
project,
name,
stages,
availability_zone
)
VALUES :tuple*:instance_snapshots

-- :name most-recent-instance-snapshot :? :1
-- :doc finds the last instance snapshot
SELECT COUNT(*) as count, project
FROM instance_snapshots
WHERE snapshot_id =
(
SELECT id
FROM snapshots
WHERE table_name = 'instance_snapshots'
ORDER by created_at DESC limit 1
)
GROUP BY project
ORDER by project ASC;
