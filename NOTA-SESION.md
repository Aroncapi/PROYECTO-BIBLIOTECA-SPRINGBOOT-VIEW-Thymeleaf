# NOTA DE SESIÓN — Leer al INICIAR la próxima sesión

> **Última actualización:** 01/10/2026 — **FASE 5 COMPLETADA** (fin de sesión por límite de tokens)
> **Proyecto:** `C:\JAVA-SPRIN BOOT\Proyecto APLICACIONES WEB II\biblioteca-escolar`
> **Objetivo:** Cumplir las 4 competencias de la matriz CIBERTEC + lo prometido en el informe **AVANCE 1**.

---

## 0. REQUISITOS EXACTOS (la imagen ya no se podrá ver — texto literal de la matriz)

Los 4 requisitos de la matriz CIBERTEC (Computación e Informática, pie de página 7):

1. *"Implementa aplicaciones distribuidas complejas bajo los principios SaaS (Software as a Service)."*
2. *"Configura la interoperabilidad de los componentes del servicio web, de acuerdo a la arquitectura de servicios web, estándares de Internet y seguridad de la información."*
3. *"Diseña y realiza pruebas a los componentes de software desarrollados."*
4. *"Diseña, desarrolla e implementa una aplicación web y/o móvil desarrollada y servicios en la nube para un proceso de una unidad de negocio aplicando metodologías o buenas prácticas para garantizar la persistencia, la visualización, el versionamiento y la seguridad transaccional aplicando pruebas funcionales y de rendimiento, así como generación de reportes exportables a diferentes formatos."*

**Cobertura planificada** (el req. 1 no se cumple literalmente sin SaaS/microservicios — decisión aceptada: se argumenta con API REST desacoplada + Docker + despliegue cloud posterior, sin re-abrir el tema):
- Req. 2 → Swagger (hecho), CORS restringido (hecho), JWT+BCrypt (ya existía), secretos por env ✅ (Fase 5).
- Req. 3 → Fase 4 (pruebas + JaCoCo).
- Req. 4 → persistencia JPA ✅, visualización Angular (final), versionamiento/Flyway ✅ (Fase 5), seguridad transaccional `@Transactional` ✅ + `@Version` ✅ (Fase 5), pruebas funcionales (Fase 4) y rendimiento (Fase 4b Gatling), reportes exportables (Fase 3 ✅).

**Promesas del AVANCE 1 que el código debe cumplir** (informe en carpeta padre `..\Informe de Proyecto'AVANCE 1.docx`):
- Frontend **Angular** + Bootstrap 5 + Chart.js (SPA), API REST Spring Boot, Java (informe dice 25, pom dice 21 — pendiente de alinear).
- **Pruebas unitarias y de integración** (§Alcance "Pruebas Unitarias y Técnicas").
- **Control de concurrencia optimista en préstamos** (§Niveles de Seguridad → `@Version` en `Prestamo`).
- **Diagramas UML + modelo de datos** (§Alcance "Diseño de Arquitectura y Topología").
- Despliegue "servidor local o nube (Railway/Render)" — solo preparado, no desplegado.
- Módulos: Auth/Usuarios con JWT+roles, Inventario (CRUD libros), Préstamos/Devoluciones, Reportes con visualización gráfica.

**Contexto del usuario:** estudiante CIBERTEC, hispanohablante, nivel intermedio; pide respuestas **paso a paso** y claras (evitar jerga innecesaria). Trabaja en IntelliJ (no usar mvn global, no está instalado → usar `mvnw.cmd`). NO mencionar microservicios ni React de nuevo (temas cerrados).

---

## 1. DECISIONES DE ARQUITECTURA (ya acordadas con el usuario — NO re-abrir estos temas)

- **Monolito en capas MVC, SIN microservicios** — los informes piden "arquitectura en capas MVC" y "cliente-servidor API REST"; ningún informe menciona microservicios.
- **Frontend en ANGULAR** (no React): el AVANCE 1 dice literalmente "el cliente en Angular" + Bootstrap 5 + Chart.js. El usuario descartó React.
- **Angular se crea AL FINAL** (fase 1-2 del plan): primero todo el backend.
- **React NO se usa.** Se eliminará Thymeleaf cuando exista Angular (fase 6).
- **Nube NO se despliega aún** (después lo harán ellos); solo preparar Docker + README con pasos Railway/Render.
- IDEs: backend en **IntelliJ** (el usuario), frontend en **VS Code**.
- Docente: Adrián Socrates Cañari de la Cruz — Grupo 8 (Capillo, Castro, Egusquiza, Ramón).

## 2. ESTADO ACTUAL: FASE 0 Y FASE 3 COMPLETADAS (verificadas con app corriendo)

### Hecho y probado ✅
1. **CORS global** en `config/SecurityConfig.java` → permite solo `http://localhost:4200` (Angular dev). Eliminado `@CrossOrigin("*")` de los 6 controladores.
2. **Swagger/OpenAPI**: dependencia `springdoc-openapi-starter-webmvc-ui 2.5.0` en pom + propiedades en `application.properties` + permitAll en SecurityConfig. Probado: `http://localhost:8080/swagger-ui/index.html` → 200.
3. **FIX crítico compilación**: `pom.xml` → plugin `maven-compiler-plugin` con `<proc>full</proc>` + annotationProcessorPaths Lombok 1.18.44. Sin esto, Maven+JDK 25 NO compila (JDK 23+ desactiva annotation processors por defecto).
4. **Nuevo endpoint gráfico**: `GET /api/admin/reportes/prestamos-por-mes?meses=6` → `{labels:[...], data:[...]}` (query en `PrestamoRepository.contarPrestamosPorMes` + `ReporteService.getPrestamosPorMes`). El dashboard Thymeleaf usaba datos hardcodeados; Angular usará este endpoint.
5. **FASE 3 — Exportación real de reportes** (competencia matriz "reportes exportables a diferentes formatos"):
   - Dependencias pom: `com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10` (PDF), `org.apache.poi:poi-ooxml:5.2.5` (Excel), `org.apache.commons:commons-csv:1.10.0` (CSV).
   - `service/ExportService.java` → `exportarPdf()`, `exportarExcel()`, `exportarCsv()`, `nombreArchivo()`.
   - `controller/ReporteExportController.java` → **`GET /api/admin/reportes/{tipo}/export/{formato}`**
     - tipos: `prestamos-activos`, `prestamos-vencidos`, `historial` (req. `fechaInicio`/`fechaFin` yyyy-MM-dd), `top-libros`, `usuarios-activos` (req. `limite`), `estadisticas-categorias`
     - formatos: `pdf`, `xlsx`, `csv` → descarga con Content-Disposition.
   - `ReporteService` += `getPrestamosPorMes(int)`, `getTopUsuariosActivos(int)`, `getEstadisticasCategorias()`.
6. **FIX crítico JWT** (el más importante): `security/JwtAuthenticationFilter.java` — `shouldNotFilter` usaba `startsWith("/")` en TODA la lista → **el filtro JWT NUNCA corría** y el API devolvía la página de login. Corregido a `rutasExcluidas` (exactas: `/`,`/login`,`/registro`) + `prefijosExcluidos` (`/admin/`,`/alumno/`,`/css/`,`/js/`,`/images/`). ⚠️ Si se reintegra Thymeleaf o se toque este archivo, recordar: la raíz `/` JAMÁS con startsWith.
   - Nota: antes "funcionaba" porque las vistas Thymeleaf usaban sesión (JSESSIONID), no JWT.

### Prueba final exitosa (01/10/2026)
- Login `admin@biblioteca.com`/`admin123` → 200 + token.
- `/api/admin/health` con Bearer → JSON.
- PDF → `application/pdf` inicia `%PDF-`; XLSX → attachment con nombre; CSV → 200; prestamos-por-mes → labels OK (data=0: la BD no tiene préstamos sembrados aún — `DataInitializer` solo crea roles, admin, alumnos y libros; **no** crea préstamos, por eso el gráfico sale en 0, no es bug).

### Verificación de endpoints para Angular (Fase 0, tercer punto — CERRADO)
Todas las vistas Thymeleaf quedan cubiertas por endpoints REST existentes; NO falta ninguno:
- dashboard → `/api/admin/dashboard` (incluye stats, topLibros, ultimosPrestamos) + `/reportes/prestamos-por-mes` (gráfico, antes hardcodeado).
- catálogo → `GET /api/libros` + contador de "préstamos activos/3" se calcula en el front con `GET /api/alumno/mis-prestamos/activos`.
- perfil alumno (totales/historial) → se calcula con `GET /api/alumno/mis-prestamos` en el front.
- registro alumno exige `nivel` (PRIMARIA|SECUNDARIA), `grado`, `seccion` (validación en `AuthController.registerAlumno`) — el form Angular debe enviarlos.

## 3. PLAN PENDIENTE (en este orden)

- [x] Fase 0 — CORS + Swagger + endpoints
- [x] Fase 3 — Reportes exportables PDF/XLSX/CSV (backend)
- [x] **Fase 5 — Versionamiento y seguridad de configuración (COMPLETADA 01/10/2026)** — ver detalle en §8.
- [ ] **Fase 4 — Pruebas** (AVANCE 1 las exige): unitarias Mockito en `LibroService/PrestamoService/UsuarioService/ReporteService/ExportService`; `@WebMvcTest`+MockMvc; tests de seguridad (rol ALUMNO no entra a `/api/admin/**`); `@DataJpaTest`+H2 (agregar dependencia H2 test); plugin **JaCoCo** en pom → reporte de cobertura. *Idea: test del 409 de concurrencia optimista (dos escrituras con versiones cruzadas en `Prestamo`).*
- [ ] **Fase 4b — Rendimiento**: simulación **Gatling** (`gatling-maven-plugin`) contra login+catálogo → evidencia reporte HTML. (La matriz pide "pruebas funcionales y de rendimiento").
- [ ] **Fase 6 — Quitar Thymeleaf**: eliminar templates/dependencias, crear controller forward a `index.html` para rutas Angular, build de Angular a `src/main/resources/static`.
- [ ] **Fase 7 — Evidencias**: `Dockerfile` multi-stage + `docker-compose.yml` (app+MySQL, sin desplegar), `README.md`, **diagramas UML** (Mermaid — el alcance del AVANCE 1 los exige), ejecutar `mvnw verify` y guardar reportes JaCoCo/Gatling.
- [ ] **FASES 1-2 AL FINAL — Angular** en carpeta `frontend/`: `ng new frontend` (última versión estable del CLI, standalone components, routing), proxy `proxy.conf.json` → `localhost:8080`, `AuthService`+`interceptor JWT`+`guards` (AdminGuard/AlumnoGuard), Bootstrap 5 + Chart.js (como dice el informe; NO Tailwind/MUI).

**Mapa template Thymeleaf → ruta Angular** (los templates viven en `src/main/resources/templates/`):

| Template actual | Ruta Angular / componente |
|---|---|
| `auth/login.html` | `/login` |
| `auth/register.html` | `/registro` |
| `admin/dashboard.html` | `/admin/dashboard` |
| `admin/libros.html` | `/admin/libros` |
| `admin/usuarios.html` | `/admin/usuarios` |
| `admin/prestamos.html` | `/admin/prestamos` |
| `admin/reportes.html` | `/admin/reportes` (botones → `/export/pdf|xlsx|csv`) |
| `admin/perfil.html` | `/admin/perfil` |
| `alumno/catalogo.html` | `/catalogo` |
| `alumno/mis-prestamos.html` | `/mis-prestamos` |
| `alumno/perfil.html` | `/perfil` |
| `error/403.html`, `error/500.html`, 404 | `/403`, `/500`, `**` catch-all |
| `layout/header.html`, `layout/footer.html` | componentes `<app-navbar>`, `<app-footer>` |

Al terminar Angular → Fase 6 (borrar `templates/`, dependencias Thymeleaf, `ViewController`, `DashboardRedirectController`; crear forward a `index.html`; quitar formLogin de SecurityConfig y dejar solo JWT+API).

## 4. CONTRATO DE API REST (lo que Angular consumirá)

- `POST /api/auth/login` `{email,password}` → `{token,type,id,email,rol}` | `POST /api/auth/register/alumno` | `GET /api/auth/check-email?email=`
- `GET /api/admin/dashboard` → stats+topLibros+ultimosPrestamos | `GET /api/admin/reportes/prestamos-por-mes?meses=6`
- `GET|POST|PUT|DELETE /api/admin/usuarios[...]`, `/api/admin/libros[...]`, `/api/admin/prestamos[...]` (devolver, actualizar-vencidos, etc.)
- 6 reportes JSON: `/api/admin/reportes/{prestamos-activos|prestamos-vencidos|historial|top-libros|usuarios-activos|estadisticas-categorias}`
- **Export**: `/api/admin/reportes/{tipo}/export/{pdf|xlsx|csv}`
- `GET /api/libros`, `/api/libros/{id}`, `/buscar`, `/categoria/{c}` (autenticado)
- `GET /api/alumno/mis-prestamos[/activos]`, `POST /api/alumno/prestamos/solicitar/{libroId}`, `PUT /api/alumno/prestamos/devolver/{id}`
- `GET|PUT /api/usuarios/perfil`, `POST /api/usuarios/cambiar-password`
- Auth: header `Authorization: Bearer <token>` (HS256, exp 24h). Roles `ROLE_ADMIN`/`ROLE_ALUMNO`.

## 5. ENTORNO Y COMANDOS

- **MySQL = XAMPP** (`C:\xampp\mysql\bin\mysqld.exe --defaults-file=C:\xampp\mysql\bin\my.ini`), MariaDB 10.4.32, user `root` pass `mysql` (coincide con `application.properties`), BD `biblioteca_escolar` creada ✅. Si el puerto 3306 no responde: matar procesos `mysqld`/`xampp-control` duplicados y relanzar mysqld (Start-Process desprendido). NO usar servicio `MySQL92` (está deshabilitado).
- **App**: `.\mvnw.cmd spring-boot:run` → :8080 (para detener: matar proceso del puerto 8080). Logs temporal: `$env:TEMP\biblioteca-app.log`.
- **Compilar**: `.\mvnw.cmd -q compile` (debe pasar SIN errores).
- Node 24 + npm 11 instalados ✅ (para Angular después).
- Java del sistema = 25; **pom = `release 21`** (DECIDIDO: NO subir a 25 — Spring Boot 3.2.3 no lee class major 69 y la app muere al arrancar; el informe AVANCE 1 hay que ajustarlo a "Java 21 (JDK 25)").
- **Variables de entorno (opcionales, con valor por defecto para dev)**: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `REMEMBER_ME_KEY`, `SPRING_PROFILES_ACTIVE=prod`.

### Protocolo de reanudación (ejecutar al INICIO de la próxima sesión)
1. Leer esta nota completa.
2. MySQL: si 3306 no responde → relanzar mysqld (ver arriba).
3. `.\mvnw.cmd -q compile` → debe decir OK (si falla, revisar el fix de Lombok/proc=full).
4. Arrancar app (`.\mvnw.cmd spring-boot:run`) y smoke test con PowerShell:
   ```powershell
   $l = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body '{"email":"admin@biblioteca.com","password":"admin123"}'
   $h = @{ Authorization = 'Bearer ' + $l.token }
   Invoke-RestMethod -Uri 'http://localhost:8080/api/admin/health' -Headers $h        # debe dar JSON status OK
   (Invoke-WebRequest -Uri 'http://localhost:8080/api/admin/reportes/top-libros/export/pdf' -Headers $h).Headers['Content-Type']  # debe dar application/pdf
   ```
   En el log debe verse Flyway: `Successfully validated 1 migration` + `Successfully baselined schema with version: 1` (la BD real ya estaba baselined).
5. Continuar con la **Fase 4 — Pruebas** (siguiente pendiente).

## 6. ARCHIVOS CREADOS/MODIFICADOS EN ESTA SESIÓN

**Sesión anterior (Fases 0 y 3):**
- `pom.xml` — springdoc, openhtmltopdf, poi-ooxml, commons-csv, maven-compiler-plugin(proc=full+lombok)
- `config/SecurityConfig.java` — CORS global + swagger permitAll
- `security/JwtAuthenticationFilter.java` — FIX shouldNotFilter
- `application.properties` — propiedades springdoc
- `repository/PrestamoRepository.java` — contarPrestamosPorMes
- `service/ReporteService.java` — getPrestamosPorMes, getTopUsuariosActivos, getEstadisticasCategorias
- `service/ExportService.java` — NUEVO (PDF/XLSX/CSV)
- `controller/ReporteExportController.java` — NUEVO (endpoints export)
- `controller/AdminController.java` — endpoint prestamos-por-mes
- 6 controladores — eliminado @CrossOrigin("*")

**Sesión 01/10/2026 (Fase 5):** ver §8.

## 7. PENDIENTES MENORES CONOCIDOS

- Enlace roto `dashboard.html:265` → `/admin/reportes/generar` (no existe; se resuelve con el dashboard Angular).
- `reportes.html` y `libros.html` siguen teniendo botones "próximamente" (solo CSV client-side) — se reemplazan al migrar a Angular con los nuevos endpoints.
- Libros/usuarios creados ANTES de la Fase 5 tienen `created_by = NULL` (esperado; la auditoría solo aplica desde ahora).
- La plantilla de los informes está en la carpeta padre: `Informe de Proyecto_Sección_4910_Lunes.docx` y `Informe de Proyecto'AVANCE 1.docx` (leer con PowerShell+ZipFile si hace falta).

## 8. FASE 5 — COMPLETADA (01/10/2026)

### 8.1 Concurrencia optimista
- `model/Prestamo.java` → **`@Version private Integer version`** (columna `version int NOT NULL`; el AVANCE 1 lo prometía, antes solo la tenía `Libro`).
- `exception/GlobalExceptionHandler.java` → handler `OptimisticLockingFailureException` → **HTTP 409** con mensaje "Conflicto de concurrencia…". (Ya probado en Fase 4: `LibroControllerTest.postLibros_conflictoOptimista_devuelve409` → 409.)

### 8.2 Auditoría JPA
- **`config/JpaAuditingConfig.java` (NUEVO)** → `@EnableJpaAuditing(auditorAwareRef="auditorAware")` + bean `AuditorAware<String>` que devuelve el email del usuario autenticado o `"sistema"` (startup/scheduled).
- `Libro` y `Prestamo` → campos `@CreatedBy/@CreatedDate/@LastModifiedBy/@LastModifiedDate` (columnas `created_by`, `created_date`, `last_modified_by`, `last_modified_date`).
- ⚠️ **OJO**: hay que poner **`@EntityListeners(AuditingEntityListener.class)`** en la entidad; sin eso `@EnableJpaAuditing` solo no dispara los callbacks (lo descubrimos probando: `version` subía pero `last_modified_*` quedaba NULL).
- Probado: PUT libro → `last_modified_by=admin@biblioteca.com`; POST libro → `created_by=admin@biblioteca.com`; préstamo alumno → `created_by=alumno.prueba@…` y al devolver `last_modified_by=alumno.prueba@…` + `version` 0→1.

### 8.3 Flyway (versionamiento del esquema)
- `pom.xml` → `flyway-core` + `flyway-mysql` (Boot 3.2.3 trae Flyway 9.22.3).
- **`src/main/resources/db/migration/V1__init.sql` (NUEVO)** → generado con `mysqldump --no-data`, limpiado (orden roles→usuarios→libros→prestamos, sin `DROP TABLE`, sin AUTO_INCREMENT=N). **No tiene datos** (el seed es `DataInitializer`).
- `application.properties` → `spring.jpa.hibernate.ddl-auto` = **`validate`** (antes `update`), `spring.flyway.baseline-on-migrate=true` + `baseline-version=1`.
- Probado en 2 escenarios:
  - **BD real existente** → `Successfully baselined schema with version: 1` (no intenta re-crear tablas).
  - **BD vacía nueva** (`biblioteca_escolar_flyway_test`) → `Migrating schema … to version "1 - init"` + `Successfully applied 1 migration` + las 4 tablas creadas. (BD de prueba ya borrada.)

### 8.4 Secretos por variables de entorno + perfil prod
- `application.properties` → `DB_URL/DB_USERNAME/DB_PASSWORD`, `JWT_SECRET/JWT_EXPIRATION`, `REMEMBER_ME_KEY` **con valor por defecto** (dev sigue funcionando igual).
- `config/SecurityConfig.java` → `@Value("${remember.me.key}")` (antes string literal `"uniqueAndSecret"` — pendiente de §7 resuelto).
- **`application-prod.properties` (NUEVO)** → `SPRING_PROFILES_ACTIVE=prod`: sin DEBUG/TRACE, `show-sql=false`, `format_sql=false`, `swagger-ui.enabled=false`, `logging.level.root=INFO`. Verificado en corrida de prueba: **0 líneas DEBUG/TRACE y 0 SQL en el log**.

### 8.5 Datos tocados durante las pruebas (ya restaurados)
- Libro 1: stock 5→7→**5** restaurado; nombre restaurado a `Cien años de soledad`.
- Préstamo de prueba id=1 → **borrado**; stock del libro 2 descuadrado → cuadrado (3/3).
- BD temporal `biblioteca_escolar_flyway_test` → **borrada**.

## 9. FASE 4 — COMPLETADA (01/10/2026)

### 9.1 Infraestructura
- `pom.xml` → `com.h2database:h2` (scope test) + `jacoco-maven-plugin` **0.8.15** (prepare-agent, report en `verify`, check **LINE COVEREDRATIO ≥ 0.60**) + propiedades `mockito.version=5.24.0` y `byte-buddy.version=1.18.14` (compatibilidad Java 25; 0.8.11 fallaba con "Unsupported class file major version 69").
- **`src/test/resources/application.properties` (NUEVO)** → H2 en memoria (`MODE=MySQL`), `ddl-auto=create-drop`, `spring.flyway.enabled=false`, JWT de prueba. **Los tests NO tocan MySQL de XAMPP.**

### 9.2 Resultado final (`.\mvnw.cmd verify`)
- **121 pruebas, 0 fallos → BUILD SUCCESS** y "All coverage checks have been met" (120 de la Fase 4 + 1 del fix 404 → §12).
- **Cobertura de líneas: 66.6 %** (947/1421) → umbral 60 % **CUMPLIDO**. Ramas: 59 % (no exigido por el check).
- Reporte HTML: `target/site/jacoco/index.html`.

### 9.3 Distribución (11 clases de test en `src/test`)
- **Unitarias Mockito (40)**: `LibroServiceTest` 9, `PrestamoServiceTest` 12, `UsuarioServiceTest` 9, `ReporteServiceTest` 5, `ExportServiceTest` 5.
- **Web `@WebMvcTest` (60)**: `AuthControllerTest` 8, `LibroControllerTest` 6, `SeguridadApiTest` 8 (7 de Fase 4 + `rutaInexistente_devuelve404` → §12), `UsuarioControllerTest` 9, `ReporteExportControllerTest` 11, `ViewControllerTest` 18.
- **Repositorio `@DataJpaTest` (15)**: `LibroRepositoryTest` 7 (incluye conflicto `@Version` → `OptimisticLockingFailureException`), `PrestamoRepositoryTest` 8 (JPQL `YEAR()/MONTH()` funciona en H2, `@Modifying` vencidos, auditoría con `@Import(JpaAuditingConfig.class)`).
- **Seguridad**: `JwtUtilsTest` 5 (token, malformado, vacío, expirado). + `contextLoads` 1.

### 9.4 Bugs encontrados y corregidos (únicos cambios en `src/main` de la Fase 4)
1. **`exception/GlobalExceptionHandler.java`** → nuevo handler `AccessDeniedException` → **HTTP 403** con mensaje "No tiene permisos…". Sin él, un ALUMNO intentando `POST /api/libros` recibía **400** (el catch-all `RuntimeException` se lo comía). Bugfix justificado, aprobado por el usuario.
2. **`templates/error/404.html` (NUEVO)** → la plantilla no existía; `GET /error/404` habría fallado con `TemplateInputException` (500) en producción. Clonado del estilo de `error/403.html`.

### 9.5 Trampas técnicas descubiertas (no volver a tropezar)
- `@WebMvcTest` con `@ContextConfiguration(classes=...)` explícito **no carga el `@ControllerAdvice`** si no lo listas → faltaba `GlobalExceptionHandler` y el login inválido daba 302 y el 404 salía sin cuerpo (`ResourceNotFoundException` tiene `@ResponseStatus`).
- `@WithMockUser` crea principal `String` → `ViewController` hace cast a `UserDetailsImpl` y redirige 302. Solución: `.with(user(UserDetailsImpl.build(usuario)))`.
- El conflicto `@Version` se lanza en `save()` (merge), **no** en `flush()` → el `assertThrows` debe envolver ambas llamadas.
- `Prestamo.fechaPrestamo` usa `@CreationTimestamp` → los tests "por mes" usan `now()` como referencia.
- Auditoría en `@DataJpaTest` requiere `@Import(JpaAuditingConfig.class)`.

### 9.6 Huecos de cobertura restantes (mejora futura, no bloquea)
- `AdminController` 12/274 líneas y `PrestamoService` 56/101 son los mayores. No afectan el umbral.

## 10. VERIFICACIÓN VS. INFORME "AVANCE 1" (01/10/2026)

Fuente: `C:\JAVA-SPRIN BOOT\Proyecto APLICACIONES WEB II\Informe de Proyecto'AVANCE 1.docx`
(extraído con PowerShell+ZipFile → `word/document.xml`; el **cuerpo llega hasta 2.2 Alcance** — el índice promete 2.3 "Ventaja comparativa" a 3.2 "Evaluación tecnológica" + Conclusiones, pero **esas secciones están vacías** en el documento).

### 10.1 Stack declarado vs. backend REAL
| Lo que promete el informe | Estado | Evidencia |
|---|---|---|
| Java 21 (JDK 25) | ✅ | `pom.xml` con `<java.version>21</java.version>`. **OJO**: subirlo a 25 **rompe el arranque** (Spring Boot 3.2.3/ASM no lee class major 69 → "Unsupported class file major version 69" → la app muere al escanear `BibliotecaEscolarApplication`). **Informe CORREGIDO (01/10/2026)**: decía "Java 25" → ahora dice **"Java 21"** (backup del docx en `%TEMP%\Informe-AVANCE1-backup-20261001.docx`) |
| Spring Boot (API REST) | ✅ | 3.2.3, 8 controllers |
| Spring Data JPA (Hibernate) | ✅ | 4 entidades, 4 repositorios |
| Spring Security | ✅ | `SecurityConfig` + `JwtAuthenticationFilter` |
| JWT sin estado | ✅ | `JwtUtils` (jjwt 0.11.5): generate/parse/validate + expiración |
| MySQL | ✅ | XAMPP + Flyway `V1__init.sql`, `ddl-auto=validate` |
| API REST desacoplada + CORS Angular | ✅ | CORS `localhost:4200` ya configurado en `SecurityConfig` |
| **Frontend Angular + Bootstrap 5 + Chart.js** | ❌ | No existe — es la **Fase 1-2** (siguiente) |
| Deploy Railway/Render | ❌ | Pendiente (Fase 7); el CORS solo acepta localhost |

### 10.2 Los 6 "Niveles de Seguridad" del informe
1. Autenticación Spring Security + JWT → ✅ (probado: `AuthControllerTest`, `JwtUtilsTest`)
2. BCrypt → ✅ (`BCryptPasswordEncoder`)
3. Roles ADMIN/ALUMNO → ✅ (`@EnableMethodSecurity`, probado: `SeguridadApiTest`)
4. Rutas/endpoints restringidos → ✅ (`/api/admin/**` ADMIN, `/api/alumno/**` ALUMNO; 403 verificados)
5. Validación de formularios + manejo global de excepciones → ✅ (`@Valid` + `GlobalExceptionHandler` 400/401/403/404/409/500)
6. **Concurrencia optimista en préstamos** → ✅ (`@Version` en `Prestamo` y `Libro`, handler 409 con test)

### 10.3 Los 4 módulos del alcance (§2.2)
| Módulo | Estado | Reglas de negocio verificadas |
|---|---|---|
| Autenticación y Gestión de Usuarios con JWT por roles | ✅ | login, registro alumno, check-email, perfil, cambiar password, CRUD usuarios (crear admin/alumno, editar, eliminar) |
| Inventario (alta, baja, actualización, categorización) | ✅ | código único, no eliminar libro con préstamos activos, stock no negativo, búsqueda por nombre/categoría |
| Préstamos/Devoluciones con cálculo automático de plazos y restricciones | ✅ | `@Scheduled(cron="0 0 0 * * *")` marca vencidos **cada medianoche** (ya existía, `PrestamoService:207` + `@EnableScheduling`), solo alumnos, sin vencidos previos, límite 3 activos, devolución solo propia/activa, cálculo días restantes/retraso |
| Reportes Estadísticos con visualización gráfica | ⚠️ 50% | **Backend ✅** (dashboard, top libros, historial rango, por mes, categorías + export PDF/Excel/CSV); **gráfica Chart.js ❌** = frontend |

### 10.4 Las 4 fases del proyecto (§2.2)
1. Diagnóstico y Análisis → ✅ (Cap. 1 del informe)
2. Diseño de Arquitectura y Topología → ⚠️ BD modelada ✅ (Flyway); **UML ❌** (Fase 7)
3. Configuración del Core y Desarrollo → ⚠️ **API Spring Boot ✅**; **cliente Angular ❌** (Fase 1-2)
4. **Pruebas Unitarias y Técnicas → ✅** (Fase 4: 120 pruebas, 66.6% JaCoCo; **Gatling ⏳** = Fase 4b)

### 10.5 Objetivos SMART
- **Obj. 4 (100% accesos autenticados/autorizados)** → ✅ garantizado por la cadena de seguridad + tests; medible en operación.
- **Obj. 3 (reportes automatizados de stock)** → ✅ endpoints listos; medible en operación.
- Objetivos 1 y 2 (tiempos/errores de registro) → se miden cuando el sistema esté en uso con frontend.

### 10.6 Exclusiones del informe → correctas ✅
Sin pasarelas de pago, sin sistemas contables, sin nube de pago (no implementados, como corresponde).

### 10.7 QUÉ FALTA (según AVANCE 1) — resumen ejecutivo
1. **Frontend Angular + Bootstrap 5 + Chart.js** (Fase 1-2) — incluye la gráfica de reportes.
2. **Gatling** (Fase 4b) — pruebas de carga.
3. **Evidencias Fase 7**: Dockerfile, README, diagramas UML Mermaid, deploy (Railway/Render) + ampliar CORS.
4. **Informe**: completar secciones 2.3-3.2 y Conclusiones (el cuerpo actual llega solo hasta 2.2). ~~Corregir "Java 25"~~ → **hecho (01/10/2026)**, ahora dice "Java 21".
5. *Opcional no exigido*: subir cobertura JaCoCo de `AdminController` (12/274 líneas).

### 10.8 EXTRAS ya implementados (no pedidos en el AVANCE 1)
Flyway (versionamiento), auditoría JPA (quién creó/modificó), perfil `prod` sin DEBUG, secretos por variables de entorno, exportación PDF/Excel/CSV/DOC, Swagger UI (springdoc), 121 pruebas con JaCoCo 66.6%.

### 10.9 RE-VERIFICACIÓN FINAL vs. AVANCE 1 (01/10/2026 — post-fix 404)
Fuente re-leída: `Informe de Proyecto'AVANCE 1.docx` (modificado 21/09/2026 → extracción vigente). Claims verificados **contra el código real** con grep:

**A. Los 6 "Niveles de Seguridad" (§2) — TODOS ✅:**
| # | Lo que promete | Estado | Evidencia en código |
|---|---|---|---|
| 1 | Autenticación Spring Security + JWT | ✅ | `SecurityConfig` + `JwtAuthenticationFilter` + `JwtUtils` (jjwt 0.11.5) |
| 2 | Encriptación BCrypt | ✅ | `SecurityConfig.java:99` → `new BCryptPasswordEncoder()` |
| 3 | Roles ADMIN/ALUMNO | ✅ | 7× `@PreAuthorize` (`AlumnoController:17`, `AdminController:32`, `LibroController:61/72/83`, `ReporteExportController:34`) + matchers en `SecurityConfig:48-69` |
| 4 | Protección de rutas/endpoints | ✅ | permitAll/hasRole/authenticated + tests 401/403 (`SeguridadApiTest`) |
| 5 | Validación de formularios + manejo global de excepciones | ✅ | `@Valid` (Auth, Admin, Libro controllers) + `GlobalExceptionHandler` con **7 handlers** (404 nuevo, 409, 403, 401, 400, 500) |
| 6 | Concurrencia optimista en préstamos | ✅ | `@Version` en `Libro.java:44` y `Prestamo.java:53` + handler 409 + test `postLibros_conflictoOptimista_devuelve409` |

**B. Los 4 Módulos (§2.2):**
| Módulo | Estado | Evidencia |
|---|---|---|
| Auth + gestión de usuarios con roles/JWT | ✅ | `AuthController` (login/registro), `UsuarioController`, `AdminController` |
| Inventario (alta, baja, update, categorización) | ✅ | `LibroController` CRUD + categorías + búsqueda |
| Préstamos/devoluciones con **cálculo automático de plazos** y restricciones | ✅ | `PrestamoService.java:72` → `plusDays(7)`; control de stock; cron vencidos diario |
| Reportes estadísticos con **visualización gráfica** | ⚠️ | API + dashboard + exports ✅; la gráfica (Chart.js) llega con Angular |

**C. Stack (§2) y Fases (§2.2):**
- Backend (Java/Spring Boot/JPA/Security/JWT/MySQL) → ✅ todo real. ~~"Java 25"~~ → **CORREGIDO (01/10/2026)**: el informe ahora dice "Java 21" (editado directo en el `word/document.xml` del docx; el pom nunca cambió).
- API REST desacoplada + CORS Angular → ✅ (CORS 4200 listo).
- Frontend Angular/Bootstrap 5/Chart.js → ❌ pendiente (Fases 1-2).
- Deploy Railway/Render + diagramas UML → ❌ pendiente (Fase 7).
- Pruebas unitarias/técnicas (fase 4) → ✅ **121 pruebas, 0 fallos, JaCoCo ≥60%**.
- Objetivos SMART Obj.4 → ✅ garantizado; Obj.3 → ✅ endpoints listos; Objs.1-2 → se miden con frontend en uso.
- Exclusiones (sin pagos/contabilidad/nube de pago) → ✅ correctas.

**VEREDICTO**: el backend **cumple el 100% de lo que le corresponde** (todo lo verificable en código: stack backend, 6 niveles de seguridad, 3.5 módulos, fases 1/3-backend/4). Lo faltante es exclusivamente lo declarado en §10.7 (Angular, Gatling, evidencias, informe 2.3-3.2) + la corrección "Java 21". Sin promesas rotas.

## 11. QUÉ SIGUE (orden de la nota)

1. **Fase 4b — Gatling** (rendimiento).
2. **Fase 7 — Evidencias** (Dockerfile, README, UML Mermaid).
3. **Fases 1-2 — Angular** (`frontend/`, Bootstrap 5 + Chart.js) y luego **Fase 6** (quitar Thymeleaf).

## 12. FIX 404 PARA RUTAS INEXISTENTES (01/10/2026)

### 12.1 Problema
Cualquier URL inexistente devolvía **500** ("Error interno del servidor: No static resource …") en vez de 404. Causa vista en Postman: `POST http://localhost:8080/api/auth/login<espacio>` → el espacio (`%20`) hacía que el path no matcheara ningún controller. `SecurityConfig` tiene `/api/auth/**` en `permitAll` → la petición pasaba la seguridad, llegaba a `ResourceHttpRequestHandler` (que lanza `NoResourceFoundException`) y el catch-all `Exception` de `GlobalExceptionHandler` la respondía como 500.

### 12.2 Cambio
- **`exception/GlobalExceptionHandler.java`** → nuevo `@ExceptionHandler(NoResourceFoundException.class)` → **HTTP 404** con `{"status":404,"message":"Recurso no encontrado: <ruta>","timestamp":…}`. Spring elige el handler más específico, así que ya no cae en el catch de `Exception`.
- **`SeguridadApiTest.rutaInexistente_devuelve404`** → `GET /api/ruta-que-no-existe` con usuario mockeado (sin sesión, la seguridad responde 302/401 antes del servlet) → 404 + `$.status=404`.
- Sin conflictos: `ResourceNotFoundException` tiene su propio handler y `NoResourceFoundException` no es `RuntimeException` (no lo come el catch de la línea 72). `ReporteExportControllerTest` que esperaba 500 (export roto) no se ve afectado.

### 12.3 Verificación
- `.\mvnw.cmd verify` → **121 pruebas, 0 fallos, BUILD SUCCESS**, "All coverage checks have been met" (cobertura ≥60%).
- **Prueba manual en vivo (app corriendo, 01/10/2026)**:
  - `POST /api/auth/login%20` (espacio final, sin token) → **404** `{"status":404,"message":"Recurso no encontrado: api/auth/login%20"}` ✅ (antes: 500).
  - `GET /api/invento` **con** token → **404** JSON ✅.
  - `GET /api/invento` **sin** token → **302 → /login** (seguridad sin cambios, responde antes del servlet) ✅.

## 13. ESTADO DEL PROYECTO: LO IMPLEMENTADO vs. LO QUE FALTA (01/10/2026)

### 13.1 TODO LO IMPLEMENTADO ✅
**Stack**: Spring Boot 3.2.3 · Java 21 (JDK 25) · MySQL XAMPP + Flyway · JPA con auditoría · Spring Security + JWT · Thymeleaf · springdoc/Swagger.

**Backend — API REST completa (8 controllers, todo operativo y probado)**:
1. **Auth** — `POST /api/auth/login` (JWT Bearer), `POST /api/auth/registro`; form login de sesión para vistas.
2. **Libros** — CRUD completo, búsqueda por nombre/categoría, control de stock con `@Version` (conflicto optimista → 409).
3. **Usuarios/Alumnos** — gestión de usuarios, perfil, cambio de contraseña.
4. **Préstamos** — solicitar (control de stock: no presta si no hay disponibilidad), devolver, mis-préstamos, **`@Scheduled(cron "0 0 0 * * *")` marca vencidos a medianoche** + `@EnableScheduling`.
5. **Reportes** — dashboard admin (totales, top libros, últimos préstamos) + gráficas de reportes.
6. **Exports** — PDF, Excel (XLSX), CSV, DOC desde `/api/admin/reportes/.../export/...`.
7. **Admin** — health, dashboard, usuarios, libros, préstamos.

**Seguridad (los 6 niveles del informe, verificados en §10.2)**: JWT sin estado · roles ADMIN/ALUMNO (`@EnableMethodSecurity` + `@PreAuthorize` → 403) · cadena de filtros (cualquier request sin sesión → 302/401) · secrets por variables de entorno · perfil `prod` sin DEBUG · CORS `localhost:4200` listo para Angular.

**UI Thymeleaf (16 templates, en uso hasta la Fase 6)**: `/login`, `/registro`, `/` (redirige por rol) · admin: dashboard, libros, usuarios, prestamos, reportes, perfil · alumno: catalogo, mis-prestamos, perfil · layout + errores 403/404/500. Rutas en `ViewController.java`; credenciales en `RESUMEN-SESION.txt`.

**Documentación**: Swagger UI en `/swagger-ui.html` (springdoc-openapi).

**Calidad — Fase 4**: **121 pruebas, 0 fallos**, JaCoCo **líneas 66.6% ≥ 60%** ("All coverage checks have been met") — 11 clases + JwtUtilsTest + contextLoads, H2 en memoria (no tocan MySQL).

**Bugs corregidos**: 403 para `AccessDeniedException` (§9.4.1) · `templates/error/404.html` (§9.4.2) · **404 para rutas inexistentes en vez de 500 (§12)**.

**Extras no pedidos**: Flyway, auditoría JPA, devtools/LiveReload, exports múltiples, Swagger.

### 13.2 LO QUE FALTA ❌ (orden §11)
1. **Fase 4b — Gatling**: pruebas de carga/rendimiento.
2. **Fase 7 — Evidencias**: Dockerfile, README.md, diagramas UML Mermaid, deploy (Railway/Render) + ampliar CORS allí.
3. **Fases 1-2 — Frontend Angular + Bootstrap 5 + Chart.js** (`frontend/`) → luego **Fase 6**: quitar Thymeleaf.
4. **Informe AVANCE 1**: completar secciones 2.3-3.2 + Conclusiones (el cuerpo llega solo hasta 2.2). ~~Corregir "Java 25"~~ → **hecho (01/10/2026): ahora dice "Java 21"**.
5. *Opcional no exigido*: subir cobertura de `AdminController` (12/274 líneas).

> Credenciales y cuentas: ver **`RESUMEN-SESION.txt`** en la raíz del proyecto.
