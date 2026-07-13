-- Identity & Access schema: users, roles, and feature-level RBAC.
-- Applied per provisioned tenant schema (schema-per-tenant).

CREATE TABLE feature (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    module      VARCHAR(100),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE role_feature (
    role_id     BIGINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    feature_id  BIGINT NOT NULL REFERENCES feature(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, feature_id)
);

CREATE TABLE app_user (
    id          BIGSERIAL PRIMARY KEY,
    keycloak_id VARCHAR(100),
    username    VARCHAR(100) NOT NULL UNIQUE,
    email       VARCHAR(150),
    full_name   VARCHAR(200),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE app_user_role (
    user_id     BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id     BIGINT NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Seed a baseline set of features and an ADMIN role.
INSERT INTO feature (code, name, module) VALUES
    ('FEATURE_USER_MANAGE',    'Manage users',    'identity'),
    ('FEATURE_USER_VIEW',      'View users',      'identity'),
    ('FEATURE_ROLE_MANAGE',    'Manage roles',    'identity'),
    ('FEATURE_ROLE_VIEW',      'View roles',      'identity'),
    ('FEATURE_FEATURE_VIEW',   'View features',   'identity');

INSERT INTO role (code, name, description) VALUES
    ('ADMIN', 'Administrator', 'Full access to identity administration');

INSERT INTO role_feature (role_id, feature_id)
SELECT r.id, f.id FROM role r CROSS JOIN feature f WHERE r.code = 'ADMIN';
