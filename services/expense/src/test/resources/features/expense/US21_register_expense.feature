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
    Cuando el conductor registra un gasto de tipo "FUEL" por el monto "180.50" en fecha "HOY+5" con evidencia "recibo1.jpg" y clave "KEY-EXP-001" en el viaje "viaje"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "REGISTERED"
    Y la respuesta incluye el campo "category" con el valor "FUEL"
    Y la respuesta incluye el campo "amount" con el valor "180.5"
    Y la respuesta incluye el campo anidado "evidence.fileName" con el valor "recibo1.jpg"
    Y la respuesta incluye el campo anidado "evidence.contentType" con el valor "image/jpeg"

  Escenario: Gasto sin evidencia queda pendiente de sustento
    Cuando el conductor registra un gasto de tipo "TOLL" por el monto "25.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-002" en el viaje "viaje"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "PENDING_SUPPORT"

  Escenario: Subir el comprobante a un gasto ya registrado
    Cuando el conductor registra un gasto de tipo "TOLL" por el monto "25.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-SUBIR" en el viaje "viaje"
    Entonces la respuesta incluye el campo "status" con el valor "PENDING_SUPPORT"
    Cuando el conductor sube el comprobante "peaje.pdf" al último gasto registrado
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "REGISTERED"
    Y la respuesta incluye el campo anidado "evidence.contentType" con el valor "application/pdf"

  Escenario: Reemplazar el comprobante de un gasto
    Cuando el conductor registra un gasto de tipo "FUEL" por el monto "90.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-REEMPLAZO" en el viaje "viaje"
    Y el conductor sube el comprobante "borroso.jpg" al último gasto registrado
    Y el conductor sube el comprobante "legible.png" al último gasto registrado
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo anidado "evidence.fileName" con el valor "legible.png"
    Y la respuesta incluye el campo anidado "evidence.contentType" con el valor "image/png"

  Escenario: Rechazar un comprobante con formato no permitido
    Cuando el conductor registra un gasto de tipo "FOOD" por el monto "20.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-FORMATO" en el viaje "viaje"
    Y el conductor sube el comprobante "nota.txt" al último gasto registrado
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "Formato no permitido. Adjunte una foto en JPEG, PNG o WEBP, o un PDF."

  Escenario: Rechazar la subida sin archivo
    Cuando el conductor registra un gasto de tipo "FOOD" por el monto "20.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-SIN-ARCHIVO" en el viaje "viaje"
    Y el conductor sube sin archivo al último gasto registrado
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "Adjunte el comprobante en el campo file."

  Escenario: Rechazar un comprobante que supera el tamaño máximo
    Cuando el conductor registra un gasto de tipo "FOOD" por el monto "20.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-GRANDE" en el viaje "viaje"
    Y el conductor sube el comprobante "grande.jpg" de 5242881 bytes al último gasto registrado
    Entonces la respuesta tiene código 413
    Y el mensaje de error es "El comprobante supera el tamaño máximo permitido de 5 MB."

  Escenario: El conductor que registró el gasto lee su comprobante
    Cuando el conductor registra un gasto de tipo "FUEL" por el monto "180.50" en fecha "HOY+5" con evidencia "recibo-leer.jpg" y clave "KEY-EXP-LEER" en el viaje "viaje"
    Y el conductor "luis@andes.pe" abre el comprobante del último gasto registrado
    Entonces la respuesta tiene código 200
    Y la respuesta tiene el tipo de contenido "image/jpeg"
    Y el contenido recibido coincide con el archivo "recibo-leer.jpg"

  Escenario: El administrador de la empresa lee el comprobante
    Cuando el conductor registra un gasto de tipo "FUEL" por el monto "60.00" en fecha "HOY+5" con evidencia "recibo2.png" y clave "KEY-EXP-ADMIN" en el viaje "viaje"
    Y el administrador "ana@andes.pe" abre el comprobante del último gasto registrado
    Entonces la respuesta tiene código 200
    Y la respuesta tiene el tipo de contenido "image/png"
    Y el contenido recibido coincide con el archivo "recibo2.png"

  Escenario: Otro conductor de la empresa no puede leer el comprobante
    Dado que el administrador invitó a "Marta Ruiz" con el correo "marta@andes.pe" y el rol "DRIVER"
    Y que "marta@andes.pe" definió la contraseña "Clave-De-Marta-2026" con su enlace de invitación
    Y que el conductor "marta@andes.pe" inició sesión
    Cuando el conductor registra un gasto de tipo "FUEL" por el monto "45.00" en fecha "HOY+5" con evidencia "recibo3.jpg" y clave "KEY-EXP-AJENO-LEER" en el viaje "viaje"
    Y el conductor "marta@andes.pe" abre el comprobante del último gasto registrado
    Entonces la respuesta tiene código 403
    Y el mensaje de error es "No tiene permiso para ver este comprobante."

  Escenario: Una empresa ajena no puede leer el comprobante
    Dado que existe una empresa verificada con RUC "20456789012" y administrador "beto@beta.pe"
    Y que el administrador "beto@beta.pe" inició sesión
    Cuando el conductor registra un gasto de tipo "FUEL" por el monto "70.00" en fecha "HOY+5" con evidencia "recibo4.jpg" y clave "KEY-EXP-AJENA-LEER" en el viaje "viaje"
    Y el administrador "beto@beta.pe" abre el comprobante del último gasto registrado
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El gasto no existe."

  Escenario: Otro conductor de la empresa no puede adjuntar el comprobante
    Dado que el administrador invitó a "Marta Ruiz" con el correo "marta@andes.pe" y el rol "DRIVER"
    Y que "marta@andes.pe" definió la contraseña "Clave-De-Marta-2026" con su enlace de invitación
    Y que el conductor "marta@andes.pe" inició sesión
    Cuando el conductor registra un gasto de tipo "TOLL" por el monto "25.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-AJENO-SUBE" en el viaje "viaje"
    Y el conductor "marta@andes.pe" intenta subir el comprobante "otro.jpg" al último gasto registrado
    Entonces la respuesta tiene código 403
    Y el mensaje de error es "Solo el conductor que registró el gasto puede adjuntar su comprobante."

  Escenario: El administrador no puede adjuntar comprobantes a un gasto
    Cuando el conductor registra un gasto de tipo "TOLL" por el monto "25.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-ADMIN-SUBE" en el viaje "viaje"
    Y el administrador "ana@andes.pe" intenta subir el comprobante "admin.jpg" al último gasto registrado
    Entonces la respuesta tiene código 403
    Y el mensaje de error es "No tiene permisos para realizar esta operación."

  Escenario: Subir un comprobante requiere autenticación
    Cuando el conductor registra un gasto de tipo "FOOD" por el monto "20.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-SIN-TOKEN" en el viaje "viaje"
    Y una persona sin token sube el comprobante "anonimo.jpg" al último gasto registrado
    Entonces la respuesta tiene código 401

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
    Cuando el conductor registra un gasto de tipo "FUEL" por el monto "95.00" en fecha "HOY+5" sin evidencia y clave "KEY-EXP-RESTRINGIDA" en el viaje "viaje"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "PENDING_SUPPORT"

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
