CREATE TABLE departments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE
);
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  gender VARCHAR(24) NOT NULL,
  role VARCHAR(20) NOT NULL,
  department_id BIGINT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id),
  INDEX idx_user_role (role),
  INDEX idx_user_department (department_id)
);
CREATE TABLE leave_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_id BIGINT NOT NULL,
  leave_type VARCHAR(20) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  rejection_reason VARCHAR(1000) NULL,
  reviewed_by BIGINT NULL,
  reviewed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_leave_employee FOREIGN KEY (employee_id) REFERENCES users(id),
  CONSTRAINT fk_leave_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
  CONSTRAINT ck_leave_dates CHECK (end_date >= start_date),
  INDEX idx_leave_status_created (status, created_at),
  INDEX idx_leave_employee_created (employee_id, created_at),
  INDEX idx_leave_start_date (start_date)
);
CREATE TABLE leave_attachments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  leave_id BIGINT NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(100) NOT NULL UNIQUE,
  content_type VARCHAR(70) NOT NULL,
  size_bytes BIGINT NOT NULL,
  uploaded_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_attachment_leave FOREIGN KEY (leave_id) REFERENCES leave_requests(id) ON DELETE CASCADE,
  INDEX idx_attachment_leave (leave_id)
);
CREATE TABLE password_reset_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_reset_user (user_id)
);
INSERT INTO departments (name) VALUES ('Engineering'), ('Human Resources'), ('Finance'), ('Marketing'), ('Operations'), ('Sales'), ('IT Support');
