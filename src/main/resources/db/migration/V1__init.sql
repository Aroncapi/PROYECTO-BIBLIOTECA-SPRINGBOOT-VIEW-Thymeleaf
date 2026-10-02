-- =====================================================================
-- V1__init.sql — Migración inicial del esquema de la biblioteca escolar
-- Generado con: mysqldump -uroot -pmysql --no-data biblioteca_escolar
-- OJO: este script crea SOLO las tablas. Los datos iniciales
-- (roles, admin, alumnos y libros) los crea DataInitializer.java.
-- =====================================================================

-- Tabla: roles
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` enum('ADMIN','ALUMNO') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ldv0v52e0udsh2h1rs0r0gw1n` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Tabla: usuarios
CREATE TABLE `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `apellido` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `grado` int DEFAULT NULL,
  `nivel` enum('PRIMARIA','SECUNDARIA') DEFAULT NULL,
  `nombre` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `seccion` varchar(255) DEFAULT NULL,
  `rol_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_kfsp0s1tflm1cwlj8idhqsad0` (`email`),
  KEY `FKqf5elo4jcq7qrt83oi0qmenjo` (`rol_id`),
  CONSTRAINT `FKqf5elo4jcq7qrt83oi0qmenjo` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Tabla: libros
CREATE TABLE `libros` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `categoria` varchar(255) NOT NULL,
  `codigo_libro` varchar(255) NOT NULL,
  `nombre` varchar(255) NOT NULL,
  `stock_disponible` int NOT NULL,
  `stock_total` int NOT NULL,
  `version` int DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_date` datetime(6) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_7792ap603is11v01wkbpw2dbo` (`codigo_libro`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Tabla: prestamos
CREATE TABLE `prestamos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `estado` enum('ACTIVO','DEVUELTO','VENCIDO') NOT NULL,
  `fecha_devolucion_estimada` datetime(6) NOT NULL,
  `fecha_devolucion_real` datetime(6) DEFAULT NULL,
  `fecha_prestamo` datetime(6) NOT NULL,
  `libro_id` bigint NOT NULL,
  `usuario_id` bigint NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_date` datetime(6) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `last_modified_date` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqwwlmlxm19mwwnpng9945iy3` (`libro_id`),
  KEY `FKeqd1t799y0x5ck9mdeltepy1w` (`usuario_id`),
  CONSTRAINT `FKeqd1t799y0x5ck9mdeltepy1w` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `FKqwwlmlxm19mwwnpng9945iy3` FOREIGN KEY (`libro_id`) REFERENCES `libros` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
