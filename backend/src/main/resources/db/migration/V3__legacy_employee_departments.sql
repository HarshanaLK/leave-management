-- An existing employee record may predate mandatory department selection.
-- Do not guess its actual department. Mark it explicitly as Unassigned for admins to correct.
INSERT INTO departments(name)
SELECT 'Unassigned' WHERE NOT EXISTS (SELECT 1 FROM departments WHERE name = 'Unassigned');
UPDATE users SET department_id=(SELECT id FROM departments WHERE name='Unassigned'),updated_at=NOW(6)
WHERE role='EMPLOYEE' AND department_id IS NULL;
