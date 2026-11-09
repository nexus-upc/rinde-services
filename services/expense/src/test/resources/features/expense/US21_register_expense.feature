# language: es
Característica: US21 Registrar un gasto con su evidencia desde el móvil
  Como conductor
  Quiero registrar un gasto indicando su tipo, monto y evidencia fotográfica
  Para dejar sustentado el gasto en ruta y no depender del papel

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que existe el vehículo "camión"
    Y que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Y que el conductor "luis@andes.pe" inició sesión
    Y que el conductor inicia el viaje "viaje"

  Escenario: Registro correcto del gasto con evidencia fotográfica
    Cuando el conductor registra un gasto de tipo "FUEL" por el monto "180.50" en fecha "HOY+5" con evidencia "https://storage.rinde.pe/evidences/recibo1.jpg" y clave "KEY-EXP-001" en el viaje "viaje"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "REGISTERED"
    Y la respuesta incluye el campo "category" con el valor "FUEL"
    Y la respuesta incluye el campo "amount" con el valor "180.5"

  Escenario: Gasto sin evidencia queda pendiente de sustento
    Cuando el conductor registra un gasto de tipo "TOLL" por el monto "25.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-002" en el viaje "viaje"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "PENDING_SUPPORT"

  Escenario: Rechazar monto inválido menor o igual a cero
    Cuando el conductor intenta registrar un gasto con monto "0" en el viaje "viaje"
    Entonces la respuesta tiene código 400

  Escenario: Rechazar clave de idempotencia duplicada
    Dado que el conductor registró un gasto de tipo "FOOD" por el monto "35.00" con clave "KEY-EXP-DUPLICADA" en el viaje "viaje"
    Cuando el conductor intenta registrar un gasto de tipo "FOOD" por el monto "35.00" con clave "KEY-EXP-DUPLICADA" en el viaje "viaje"
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "La clave de idempotencia ya fue utilizada."

  Escenario: Registrar un gasto aunque la suscripción de la empresa esté restringida
    Dado que la suscripción de la empresa con RUC "20123456789" está restringida
    Cuando el conductor registra un gasto de tipo "FUEL" por el monto "95.00" en fecha "HOY+5" con evidencia "https://storage.rinde.pe/evidences/recibo2.jpg" y clave "KEY-EXP-RESTRINGIDA" en el viaje "viaje"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "REGISTERED"

  Escenario: Obtener enlace firmado para subida directa de comprobante
    Cuando el conductor solicita un enlace firmado para subir su comprobante
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "expiresInSeconds" con el valor "900"

  Escenario: Registrar gasto requiere autenticación
    Cuando una persona sin token registra un gasto en el viaje "viaje"
    Entonces la respuesta tiene código 401

  Escenario: Rechazar gasto en un viaje que no existe
    Cuando el conductor intenta registrar un gasto de tipo "FUEL" por el monto "40.00" en un viaje que no existe
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El viaje no existe."

  Escenario: Rechazar gasto en un viaje de otra empresa
    Dado que existe una empresa verificada con RUC "20456789012" y administrador "beto@beta.pe"
    Y que el administrador "beto@beta.pe" inició sesión
    Y que el administrador invitó a "Marta Ruiz" con el correo "marta@beta.pe" y el rol "DRIVER"
    Y que "marta@beta.pe" definió la contraseña "Clave-De-Marta-2026" con su enlace de invitación
    Cuando el conductor "marta@beta.pe" intenta registrar un gasto de tipo "FUEL" por el monto "70.00" con clave "KEY-AJENO-2026" en el viaje "viaje"
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El viaje no existe."

  Escenario: Rechazar gasto en un viaje que todavía no ha iniciado
    Dado que el administrador programa el viaje "programado" de "Lima" a "Cusco" con carga "Papel" y salida "HOY+10"
    Cuando el conductor registra un gasto de tipo "FOOD" por el monto "20.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-PROGRAMADO" en el viaje "programado"
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "El viaje todavía no ha iniciado."

  Escenario: Registrar gasto en un viaje ya finalizado
    Dado que el conductor "luis@andes.pe" finaliza el viaje "viaje"
    Cuando el conductor registra un gasto de tipo "TOLL" por el monto "12.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-FINALIZADO" en el viaje "viaje"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "PENDING_SUPPORT"
