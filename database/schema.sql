-- usuarios que registran y atienden incidencias
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    correo VARCHAR(120) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'Residente',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT chk_usuario_rol
        CHECK (rol IN ('Residente', 'Administrador'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- catalogo utilizado para clasificar las incidencias
CREATE TABLE IF NOT EXISTS tipo_incidencia (
    id_tipo_incidencia INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_tipo_incidencia_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- registro principal de incidencias
CREATE TABLE IF NOT EXISTS incidencia (
    id_incidencia INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(15) NOT NULL,
    fecha_registro DATE NOT NULL,
    hora_registro TIME NOT NULL,
    descripcion VARCHAR(250) NOT NULL,
    ubicacion VARCHAR(60) NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'Pendiente',
    foto_url VARCHAR(500) NULL,
    fecha_atencion DATETIME NULL,
    observacion_atencion VARCHAR(250) NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    id_tipo_incidencia INT UNSIGNED NOT NULL,
    id_usuario_registro INT UNSIGNED NOT NULL,
    CONSTRAINT uq_incidencia_codigo UNIQUE (codigo),
    CONSTRAINT chk_incidencia_descripcion
        CHECK (CHAR_LENGTH(descripcion) BETWEEN 10 AND 250),
    CONSTRAINT chk_incidencia_estado
        CHECK (estado IN ('Pendiente', 'En proceso', 'Atendido', 'Rechazado')),
    CONSTRAINT fk_incidencia_tipo
        FOREIGN KEY (id_tipo_incidencia)
        REFERENCES tipo_incidencia (id_tipo_incidencia)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_incidencia_usuario
        FOREIGN KEY (id_usuario_registro)
        REFERENCES usuario (id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX idx_incidencia_fecha (fecha_registro),
    INDEX idx_incidencia_estado (estado),
    INDEX idx_incidencia_eliminado (eliminado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- tipos iniciales definidos para el proyecto
INSERT IGNORE INTO tipo_incidencia (nombre) VALUES
    ('Fuga de agua'),
    ('Ascensor'),
    ('Ruido'),
    ('Seguridad'),
    ('Limpieza'),
    ('Otro');
