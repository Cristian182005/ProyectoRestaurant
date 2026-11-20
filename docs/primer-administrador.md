# Procedimiento seguro para crear el primer administrador en producción

> Verificado el 2026-11-20 contra el esquema real de PostgreSQL (solo lectura) y contra
> `spring-security-crypto` 7.1.2 (la versión que resuelve Spring Boot 4.1.2).

## Contexto

- No existe registro público: el endpoint `POST /api/auth/registro` fue eliminado;
  el único acceso público es `POST /api/auth/login` (`config/SecurityConfig.java`).
- Todo el alta de usuarios vive en `/api/usuarios` y exige el rol `ADMINISTRADOR`.
- Consecuencia: una instalación nueva de producción **no tiene ningún usuario con el
  que iniciar sesión**. El primer administrador debe crearse directamente en la base
  de datos, una sola vez.

## Reglas que este procedimiento respeta

- **No altera el esquema**: no hay `CREATE`, `ALTER` ni `DROP`; solo dos `INSERT` sobre
  las tablas `persona` y `usuario` que ya existen.
- **No usa credenciales predeterminadas**: la contraseña la elige quien ejecuta el
  procedimiento y solo se guarda como hash BCrypt.
- **No usa el perfil `dev`**: `config/DevDataSeeder.java` (anotado `@Profile("dev")`)
  crea usuarios de desarrollo con contraseñas fijas. Nunca se debe activar el perfil
  `dev` apuntando a la base de producción.

## Paso 1 — Elegir las credenciales

1. Elija un correo administrativo definitivo (ej.: el suyo corporativo) y una contraseña
   **fuerte y única**, distinta de cualquier credencial de desarrollo.
2. No reutilice los correos ni las contraseñas del seeder de desarrollo
   (`config/DevDataSeeder.java`): son exclusivas del entorno local.

## Paso 2 — Generar el hash BCrypt de la contraseña

La contraseña nunca se escribe en texto plano en SQL; solo su hash. Se ha verificado
que el backend acepta los tres prefijos estándar (`$2a$`, `$2b$`, `$2y$`) y rechaza
contraseñas incorrectas. Cualquiera de las opciones sirve:

**Opción A — `htpasswd` (Apache HTTP Server):**

```bash
htpasswd -bnBC 12 "" "su-contraseña-fuerte" | tr -d ':\n'
```

**Opción B — Python 3 (módulo `bcrypt`):**

```bash
python -c "import bcrypt, getpass; print(bcrypt.hashpw(getpass.getpass().encode(), bcrypt.gensalt(rounds=12)).decode())"
```

**Opción C — Java, con las propias dependencias del proyecto** (usa la versión de
`spring-security-crypto` que resuelva Maven en su `.m2`):

```bash
java -cp "spring-security-crypto.jar;spring-core.jar" HashGenerator.java
```

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}
```

El resultado es una cadena `$2a$...` / `$2b$...` / `$2y$...`. Guárdela en un archivo
temporal que **no se versionará**.

## Paso 3 — Insertar las dos filas

Conéctese a la base de datos de producción (el nombre real está en
`spring.datasource.url` de la configuración de producción) y ejecute, sustituyendo
`<correo>`, `<nombre>` y `<hash>`:

```sql
-- 1) Persona: solo estas 4 columnas; el resto de la tabla admite NULL
INSERT INTO persona (email, nombre, rol)
VALUES ('<correo>', '<nombre>', 'ADMINISTRADOR');

-- 2) Usuario: contraseña siempre como hash BCrypt
INSERT INTO usuario (password, persona_id)
VALUES ('<hash>', (SELECT id FROM persona WHERE email = '<correo>'));
```

Restricciones del esquema (verificadas en PostgreSQL):

- `persona.email` tiene la restricción única `uq_persona_email` (no puede repetirse).
- `usuario.password` y `usuario.persona_id` son `NOT NULL`;
  `usuario.persona_id` tiene la clave foránea `fk_usuario_persona`.
- El valor del rol debe ser exactamente `ADMINISTRADOR` (los otros valores válidos son
  `MOZO` y `CAJERO`; cualquier otro produce 400/403).

No se tocan tablas ni datos de productos, pedidos, ventas, inventario ni clientes.

## Paso 4 — Verificar el acceso

1. Inicie sesión en el frontend con `<correo>` y la contraseña elegida.
2. Compruebe con esta consulta de **solo lectura** quiénes son administradores:

```sql
SELECT p.email, p.rol
FROM persona p
JOIN usuario u ON u.persona_id = p.id
WHERE p.rol = 'ADMINISTRADOR';
```

3. Si el hash fue generado en una máquina compartida o el archivo temporal ya no es
   necesario, **rote la contraseña** de inmediato desde
   *Usuarios y roles* → acción de contraseña (endpoint `PUT /api/usuarios/{id}/password`),
   que vuelve a guardarla con BCrypt.

## Paso 5 — Blindaje posterior

- Elimine los archivos temporales (script SQL y archivo con el hash); nunca los
  suba al repositorio.
- Confirme que en producción **no** esté activo el perfil `dev`
  (`SPRING_PROFILES_ACTIVE` o `--spring.profiles.active`): con ese perfil activo,
  `DevDataSeeder` crearía usuarios semilla con contraseñas fijas.
- A partir de aquí, todos los usuarios adicionales (mozo, cajero y más administradores)
  se crean desde *Usuarios y roles* con `POST /api/usuarios`; este procedimiento de
  arranque ya no es necesario.

Usuario: admin@hornero.com
Contraseña: administrador123