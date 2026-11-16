# language: es
Característica: US14 Programar viajes
  Como administrador u operador de una empresa
  Quiero programar un viaje con su ruta, carga y fecha de salida
  Para organizar el transporte de cada carga

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión

  Escenario: Programar un viaje para una fecha futura
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Entonces la respuesta tiene código 201
    Y el viaje queda en estado "SCHEDULED"
    Y la respuesta incluye el campo "code" con el valor "TRP-000001"

  Escenario: Programar un viaje con peso opcional
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Cusco" con carga "Muebles", peso "850.25" y salida "HOY+5"
    Entonces la respuesta tiene código 201
    Y el peso de la carga es "850.25"

  Escenario: El código correlativo avanza por empresa
    Cuando el administrador programa el viaje "primero" de "Lima" a "Ica" con carga "Cajas" y salida "HOY+5"
    Y el administrador programa el viaje "segundo" de "Lima" a "Ica" con carga "Cajas" y salida "HOY+6"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "code" con el valor "TRP-000002"

  Escenario: Rechazar datos obligatorios incompletos
    Cuando el administrador intenta programar un viaje sin destino
    Entonces la respuesta tiene código 400

  Escenario: Rechazar un peso que no sea mayor que cero
    Cuando el administrador intenta programar un viaje con peso "0"
    Entonces la respuesta tiene código 400

  Escenario: Una fecha pasada requiere confirmación
    Cuando el administrador programa el viaje "viaje" con salida pasada "HOY-10"
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "La fecha de salida es anterior a hoy. Confirme que se trata de un viaje ya ejecutado."

  Escenario: Programar un viaje ejecutado tras confirmar la fecha pasada
    Cuando el administrador confirma la fecha pasada y programa el viaje "viaje" para "HOY-10"
    Entonces la respuesta tiene código 201
    Y el viaje queda en estado "SCHEDULED"

  Escenario: Un conductor no puede programar viajes
    Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Cuando el conductor "luis@andes.pe" intenta programar un viaje
    Entonces la respuesta tiene código 403

  Escenario: Rechazar programar un viaje cuando la suscripción de la empresa está restringida
    Dado que la suscripción de la empresa con RUC "20123456789" está restringida
    Cuando el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Entonces la respuesta tiene código 403
    Y el mensaje de error contiene "La suscripción está vencida"

  Escenario: Programar requiere autenticación
    Cuando una persona sin token programa un viaje
    Entonces la respuesta tiene código 401
