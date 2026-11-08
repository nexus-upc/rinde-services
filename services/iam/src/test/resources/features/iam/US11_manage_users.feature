# language: es
Característica: US11 Gestionar los usuarios de la empresa
  Como administrador de una empresa
  Quiero invitar usuarios, cambiar su rol y deshabilitarlos
  Para controlar quién accede a los datos de mi empresa

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión

  Escenario: Invitar a un conductor
    Cuando el administrador invita a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Entonces la respuesta tiene código 201
    Y el usuario queda con el rol "DRIVER" y el estado "INVITED"
    Y se publica el evento "UserInvited" para "luis@andes.pe"

  Escenario: El usuario invitado define su contraseña y queda activo
    Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Cuando "luis@andes.pe" define la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Entonces la respuesta tiene código 204
    Y "luis@andes.pe" puede iniciar sesión con la contraseña "Clave-De-Luis-2026"

  Escenario: No se puede invitar un correo ya registrado
    Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Cuando el administrador invita a "Otro Luis" con el correo "luis@andes.pe" y el rol "DRIVER"
    Entonces la respuesta tiene código 409

  Escenario: Cambiar el rol de un usuario
    Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Cuando el administrador cambia el rol del usuario "luis@andes.pe" a "OPERATIONS_MANAGER"
    Entonces la respuesta tiene código 200
    Y el usuario queda con el rol "OPERATIONS_MANAGER" y el estado "INVITED"

  Escenario: Deshabilitar a un usuario
    Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Cuando el administrador deshabilita al usuario "luis@andes.pe"
    Entonces la respuesta tiene código 200
    Y el usuario queda con el rol "DRIVER" y el estado "DISABLED"

  Escenario: El listado solo muestra los usuarios de la empresa del administrador
    Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que existe una empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
    Cuando el administrador lista los usuarios
    Entonces la respuesta tiene código 200
    Y la lista contiene 2 usuarios
    Y la lista incluye al usuario "luis@andes.pe"
    Y la lista no incluye al usuario "otro@otra.pe"

  Escenario: Un usuario de otra empresa no se puede modificar y no se revela que existe
    Dado que existe una empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
    Y que el administrador "otro@otra.pe" inició sesión
    Y que el administrador invitó a "Chofer Otra" con el correo "chofer@otra.pe" y el rol "DRIVER"
    Y que el administrador "ana@andes.pe" inició sesión
    Cuando el administrador deshabilita al usuario "chofer@otra.pe"
    Entonces la respuesta tiene código 404

  Escenario: Un conductor no puede gestionar usuarios
    Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Cuando "luis@andes.pe" intenta listar los usuarios
    Entonces la respuesta tiene código 403

  Escenario: Cambiar el rol de un usuario que no existe responde 404
    Cuando el administrador cambia el rol de un usuario inexistente a "OPERATIONS_MANAGER"
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El usuario no existe."
