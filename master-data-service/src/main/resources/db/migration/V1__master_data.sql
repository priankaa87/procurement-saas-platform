-- Master data schema: units, currencies, item categories/items, geography.
-- Applied per provisioned tenant schema (schema-per-tenant).

CREATE TABLE measurement_unit (
    id      BIGSERIAL PRIMARY KEY,
    code    VARCHAR(30)  NOT NULL UNIQUE,
    name    VARCHAR(100) NOT NULL,
    symbol  VARCHAR(20)
);

CREATE TABLE currency (
    id      BIGSERIAL PRIMARY KEY,
    code    VARCHAR(3)   NOT NULL UNIQUE,
    name    VARCHAR(100) NOT NULL,
    symbol  VARCHAR(10)
);

CREATE TABLE item_category (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE item (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    category_id BIGINT NOT NULL REFERENCES item_category(id),
    unit_id     BIGINT NOT NULL REFERENCES measurement_unit(id),
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE country (
    id      BIGSERIAL PRIMARY KEY,
    iso2    VARCHAR(2)   NOT NULL UNIQUE,
    name    VARCHAR(100) NOT NULL
);

CREATE TABLE city (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    country_id  BIGINT NOT NULL REFERENCES country(id)
);

-- Seed common reference data.
INSERT INTO measurement_unit (code, name, symbol) VALUES
    ('PCS', 'Pieces', 'pcs'),
    ('KG',  'Kilogram', 'kg'),
    ('L',   'Litre', 'L'),
    ('M',   'Metre', 'm'),
    ('BOX', 'Box', 'box');

INSERT INTO currency (code, name, symbol) VALUES
    ('USD', 'US Dollar', '$'),
    ('EUR', 'Euro', '€'),
    ('GBP', 'Pound Sterling', '£'),
    ('BDT', 'Bangladeshi Taka', '৳');

INSERT INTO item_category (code, name, description) VALUES
    ('IT-HARDWARE', 'IT Hardware', 'Computers, servers, networking'),
    ('OFFICE',      'Office Supplies', 'Stationery and consumables'),
    ('SERVICES',    'Services', 'Professional and managed services');

INSERT INTO country (iso2, name) VALUES
    ('US', 'United States'),
    ('GB', 'United Kingdom'),
    ('BD', 'Bangladesh');
