# language: es
Característica: US22 Sincronizar gastos registrados sin conexión (Offline Sync)
  Como conductor
  Quiero enviar un lote de gastos registrados durante el viaje sin conexión
  Para sincronizarlos con el sistema en cuanto recupere conectividad a internet

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

  Escenario: Sincronizar con éxito un lote de gastos capturados offline
    Cuando el conductor sincroniza un lote con 2 gastos para el viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "totalReceived" con el valor "2"
    Y la respuesta incluye el campo "synchronizedCount" con el valor "2"

  Escenario: Sincronización idempotente de lote con gastos ya existentes
    Dado que el conductor registró un gasto "combustible" de tipo "FUEL" por el monto "100.00" con clave "KEY-OFFLINE-01" en el viaje "viaje"
    Cuando el conductor sincroniza un lote que incluye el gasto con clave "KEY-OFFLINE-01" y uno nuevo con clave "KEY-OFFLINE-02" para el viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "totalReceived" con el valor "2"
    Y la respuesta incluye el campo "synchronizedCount" con el valor "2"

  Escenario: Lote con un gasto de un viaje que no existe guarda solo el gasto válido
    Cuando el conductor sincroniza un lote con un gasto para el viaje "viaje" y otro para un viaje que no existe
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "synchronizedCount" con el valor "1"
    Y la respuesta incluye el campo "rejectedCount" con el valor "1"
    Y el elemento 0 de rechazados tiene el campo "code" con el valor "TRIP_NOT_FOUND"
    Y el elemento 0 de rechazados tiene el campo "reason" con el valor "El viaje no existe."
    Cuando el conductor consulta los gastos del viaje "viaje"
    Entonces la lista de gastos contiene 1 elementos

  Escenario: Lote en el que todos los gastos son inválidos responde 200 con todos rechazados
    Dado que el administrador programa el viaje "programado" de "Lima" a "Cusco" con carga "Papel" y salida "HOY+10"
    Cuando el conductor sincroniza un lote con un gasto para el viaje "programado" y otro para el viaje "inexistente"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "synchronizedCount" con el valor "0"
    Y la respuesta incluye el campo "rejectedCount" con el valor "2"
    Y el elemento 0 de rechazados tiene el campo "code" con el valor "TRIP_NOT_STARTED"
    Y el elemento 1 de rechazados tiene el campo "code" con el valor "TRIP_NOT_FOUND"

  Escenario: Reenviar el mismo lote devuelve los mismos gastos sin duplicarlos
    Cuando el conductor sincroniza el lote con las claves "KEY-REP-1" y "KEY-REP-2" para el viaje "viaje"
    Entonces la respuesta tiene código 200
    Cuando el conductor sincroniza el lote con las claves "KEY-REP-1" y "KEY-REP-2" para el viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "synchronizedCount" con el valor "2"
    Y la respuesta incluye el campo "rejectedCount" con el valor "0"
    Cuando el conductor consulta los gastos del viaje "viaje"
    Entonces la lista de gastos contiene 2 elementos

  Escenario: Clave de idempotencia usada por otra empresa se rechaza sin revelar su gasto
    Dado que existe una empresa verificada con RUC "20456789012" y administrador "beto@beta.pe"
    Y que el administrador "beto@beta.pe" inició sesión
    Y que el administrador invitó a "Marta Ruiz" con el correo "marta@beta.pe" y el rol "DRIVER"
    Y que "marta@beta.pe" definió la contraseña "Clave-De-Marta-2026" con su enlace de invitación
    Y que existe el vehículo "camión beta"
    Y que el administrador programa el viaje "viaje ajeno" de "Lima" a "Trujillo" con carga "Papel" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje ajeno" al vehículo "camión beta" y al conductor "marta@beta.pe"
    Y que el conductor "marta@beta.pe" inició sesión
    Y que el conductor "marta@beta.pe" inicia el viaje "viaje ajeno"
    Y que el conductor "marta@beta.pe" registró el gasto "gasto ajeno" de tipo "FUEL" por el monto "300.00" con clave "KEY-AJENA-01" en el viaje "viaje ajeno"
    Cuando el conductor sincroniza el lote con las claves "KEY-AJENA-01" y "KEY-PROPIA-01" para el viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "synchronizedCount" con el valor "1"
    Y el elemento 0 de rechazados tiene el campo "code" con el valor "DUPLICATE_KEY"
    Y el elemento 0 de rechazados tiene el campo "reason" con el valor "La clave de idempotencia ya fue utilizada."
    Y la respuesta no incluye el gasto "gasto ajeno"

  Escenario: Sincronizar sin autenticación es rechazado
    Cuando una persona sin token sincroniza un lote de gastos para el viaje "viaje"
    Entonces la respuesta tiene código 401

  Escenario: Sincronizar un gasto sin comprobante y subir su foto después
    Cuando el conductor sincroniza un lote con 1 gastos para el viaje "viaje"
    Y el conductor sube el comprobante "foto-offline.jpg" al primer gasto sincronizado
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "REGISTERED"

  Escenario: Un campo evidence heredado no bloquea la sincronización del gasto
    Cuando el conductor sincroniza un gasto con una URL de comprobante heredada para el viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "synchronizedCount" con el valor "1"
    Y el primer gasto sincronizado tiene el estado "PENDING_SUPPORT" y no tiene comprobante
