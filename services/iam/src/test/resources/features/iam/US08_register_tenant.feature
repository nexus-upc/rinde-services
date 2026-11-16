# language: es
Característica: US08 Registrar la empresa y confirmar su correo
  Como administrador de una pyme de transporte
  Quiero registrar mi empresa en RINDE y confirmar mi correo
  Para empezar a gestionar mi operación

  Escenario: Registrar una empresa nueva
    Cuando registro la empresa "Transportes Andes SAC" con RUC "20123456789" y administrador "ana@andes.pe"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "PENDING_VERIFICATION"
    Y la respuesta incluye el campo "ruc" con el valor "20123456789"
    Y se publica el evento "TenantRegistered" para "ana@andes.pe"

  Escenario: No se puede registrar un RUC ya registrado
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Cuando registro la empresa "Otra Empresa SAC" con RUC "20123456789" y administrador "luis@otra.pe"
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "Ya existe una empresa registrada con ese RUC."

  Escenario: No se puede registrar un correo de administrador ya registrado
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Cuando registro la empresa "Otra Empresa SAC" con RUC "20987654321" y administrador "ana@andes.pe"
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "Ya existe un usuario registrado con ese correo."

  Escenario: Un RUC con menos de 11 dígitos es rechazado
    Cuando registro la empresa "Empresa Chica SAC" con RUC "123" y administrador "ana@andes.pe"
    Entonces la respuesta tiene código 400

  Escenario: Confirmar el correo activa la empresa
    Dado que registré la empresa con RUC "20123456789" y administrador "ana@andes.pe" sin confirmar el correo
    Cuando confirmo el correo de la empresa con RUC "20123456789" usando el enlace recibido
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "status" con el valor "ACTIVE"

  Escenario: Un enlace de verificación no se puede usar dos veces
    Dado que registré la empresa con RUC "20123456789" y administrador "ana@andes.pe" sin confirmar el correo
    Y confirmo el correo de la empresa con RUC "20123456789" usando el enlace recibido
    Cuando confirmo el correo de la empresa con RUC "20123456789" usando el enlace recibido
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "El enlace no es válido, venció o ya fue utilizado."

  Escenario: Un enlace de verificación incorrecto es rechazado
    Dado que registré la empresa con RUC "20123456789" y administrador "ana@andes.pe" sin confirmar el correo
    Cuando confirmo el correo de la empresa con RUC "20123456789" usando el enlace "enlace-inventado"
    Entonces la respuesta tiene código 400

  Escenario: Confirmar el correo de una empresa que no existe
    Cuando confirmo el correo de una empresa que no existe usando el enlace "cualquiera"
    Entonces la respuesta tiene código 404
