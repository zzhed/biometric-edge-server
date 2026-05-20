CREATE TABLE IF NOT EXISTS user_info (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(256) NOT NULL COMMENT 'BCrypt hash',
    email       VARCHAR(128) NOT NULL UNIQUE,
    status      TINYINT      DEFAULT 1 COMMENT '1=active 0=disabled',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS biometric_model (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    modality    VARCHAR(32)  NOT NULL COMMENT 'face/fingerprint/palm/iris/voice',
    version     VARCHAR(32)  NOT NULL,
    status      VARCHAR(16)  DEFAULT 'active' COMMENT 'active/inactive/deprecated',
    description VARCHAR(512),
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dataset_info (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(128) NOT NULL,
    modality     VARCHAR(32)  NOT NULL COMMENT 'face/fingerprint/palm/iris/voice',
    sample_count INT          DEFAULT 0,
    description  VARCHAR(512),
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS workflow (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(256) NOT NULL,
    nodes       MEDIUMTEXT   NOT NULL COMMENT 'VueFlow nodes JSON array',
    edges       MEDIUMTEXT   NOT NULL COMMENT 'VueFlow edges JSON array',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS protection_method (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    level       VARCHAR(16)  NOT NULL COMMENT 'image/template',
    type        VARCHAR(32)  NOT NULL COMMENT 'anonymization/perturbation/masking/encryption/homomorphic/...',
    description VARCHAR(512),
    parameters  JSON         COMMENT 'Array of {key,label,type,default,options,min,max}',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
