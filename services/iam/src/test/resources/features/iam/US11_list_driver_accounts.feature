# language: es
Característica: US11 Listar las cuentas DRIVER de la empresa para vincularlas a un conductor
  Como responsable de operaciones o administrador
  Quiero elegir la cuenta DRIVER de mi empresa al registrar un conductor
  Para vincularla sin escribir identificadores a mano

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión

  Regla: El administrador puede listar los usuarios de un rol

    Escenario: El administrador lista solo las cuentas DRIVER
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que el administrador invitó a "Rosa Mendez" con el correo "rosa@andes.pe" y el rol "OPERATIONS_MANAGER"
      Cuando el administrador lista los usuarios con el rol "DRIVER"
      Entonces la respuesta tiene código 200
      Y la lista contiene 1 usuarios
      Y la lista incluye al usuario "luis@andes.pe"
      Y la lista no incluye al usuario "rosa@andes.pe"

    Escenario: Pedir un rol que no existe responde 400
      Cuando el administrador lista los usuarios con el rol "SUPERUSER"
      Entonces la respuesta tiene código 400
      Y el mensaje de error es "El rol indicado no existe."

  Regla: El responsable de operaciones solo ve las cuentas DRIVER de su empresa

    Escenario: El responsable lista las cuentas DRIVER de su empresa
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que el administrador invitó a "Rosa Mendez" con el correo "rosa@andes.pe" y el rol "OPERATIONS_MANAGER"
      Y que "rosa@andes.pe" definió la contraseña "Clave-De-Rosa-2026" con su enlace de invitación
      Cuando "rosa@andes.pe" lista los usuarios con el rol "DRIVER"
      Entonces la respuesta tiene código 200
      Y la lista contiene 1 usuarios
      Y la lista incluye al usuario "luis@andes.pe"

    Escenario: Las cuentas DRIVER de otra empresa no aparecen para el responsable
      Dado que el administrador invitó a "Rosa Mendez" con el correo "rosa@andes.pe" y el rol "OPERATIONS_MANAGER"
      Y que "rosa@andes.pe" definió la contraseña "Clave-De-Rosa-2026" con su enlace de invitación
      Y que existe una empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
      Y que el administrador "otro@otra.pe" inició sesión
      Y que el administrador invitó a "Chofer Otra" con el correo "chofer@otra.pe" y el rol "DRIVER"
      Y que el administrador "ana@andes.pe" inició sesión
      Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Cuando "rosa@andes.pe" lista los usuarios con el rol "DRIVER"
      Entonces la respuesta tiene código 200
      Y la lista contiene 1 usuarios
      Y la lista incluye al usuario "luis@andes.pe"
      Y la lista no incluye al usuario "chofer@otra.pe"

    Escenario: El responsable no puede listar los usuarios sin filtrar por rol
      Dado que el administrador invitó a "Rosa Mendez" con el correo "rosa@andes.pe" y el rol "OPERATIONS_MANAGER"
      Y que "rosa@andes.pe" definió la contraseña "Clave-De-Rosa-2026" con su enlace de invitación
      Cuando "rosa@andes.pe" lista los usuarios
      Entonces la respuesta tiene código 403

    Escenario: El responsable no puede listar cuentas de un rol distinto de DRIVER
      Dado que el administrador invitó a "Rosa Mendez" con el correo "rosa@andes.pe" y el rol "OPERATIONS_MANAGER"
      Y que "rosa@andes.pe" definió la contraseña "Clave-De-Rosa-2026" con su enlace de invitación
      Cuando "rosa@andes.pe" lista los usuarios con el rol "ADMINISTRATOR"
      Entonces la respuesta tiene código 403

  Regla: Un conductor no lista cuentas de la empresa

    Escenario: El conductor no puede listar las cuentas DRIVER
      Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
      Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
      Cuando "luis@andes.pe" lista los usuarios con el rol "DRIVER"
      Entonces la respuesta tiene código 403
