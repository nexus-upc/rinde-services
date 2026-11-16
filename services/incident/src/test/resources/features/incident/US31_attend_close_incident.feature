# language: es
Característica: US31 Atender y cerrar una incidencia
  Como responsable de operaciones
  Quiero registrar la acción tomada frente a una incidencia y cerrarla
  Para dejar documentada su resolución en el historial del vehículo

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
    Y que el conductor "luis@andes.pe" reportó la incidencia "averia" de tipo "BREAKDOWN" con la descripción "Falla en el freno neumático" sin evidencia en el viaje "viaje"

  Escenario: Cerrar la incidencia con la acción tomada
    Cuando el administrador cierra la incidencia "averia" con la acción "Se cambió la llanta de repuesto"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "status" con el valor "CLOSED"
    Y la respuesta incluye la resolución con la acción "Se cambió la llanta de repuesto" y cerrada por "ana@andes.pe"
    Y se publica el evento "IncidentClosed" para "ana@andes.pe"
    Y se publica el evento "IncidentClosed" del vehículo "camión"

  Escenario: Pasar la incidencia a atención sin cerrarla
    Cuando el administrador pasa la incidencia "averia" a atención
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "status" con el valor "IN_PROGRESS"
    Y no se publica el evento "IncidentClosed"

  Escenario: Rechazar un cambio de estado que no sea pasar a atención
    Cuando el administrador intenta pasar la incidencia "averia" al estado "CLOSED"
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "Este cambio solo pasa la incidencia a en atención. Para cerrarla use la acción de cierre."

  Escenario: No se cierra dos veces la misma incidencia
    Dado que la incidencia "averia" fue cerrada con la acción "Revisada en taller"
    Cuando el administrador cierra la incidencia "averia" con la acción "Otra acción"
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "La incidencia ya está cerrada."

  Escenario: Una incidencia cerrada no pasa a atención
    Dado que la incidencia "averia" fue cerrada con la acción "Revisada en taller"
    Cuando el administrador intenta pasar la incidencia "averia" al estado "IN_PROGRESS"
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "Solo una incidencia abierta puede pasar a atención."

  Escenario: Rechazar el cierre sin acción tomada
    Cuando el administrador intenta cerrar la incidencia "averia" sin acción
    Entonces la respuesta tiene código 400

  Escenario: El conductor no puede cerrar una incidencia
    Cuando el conductor "luis@andes.pe" intenta cerrar la incidencia "averia" con la acción "Intento del conductor"
    Entonces la respuesta tiene código 403

  Escenario: Una empresa ajena no puede cerrar la incidencia
    Dado que existe otra empresa verificada con RUC "20456789012" y administrador "beto@beta.pe"
    Y que el administrador de la otra empresa "beto@beta.pe" inició sesión
    Cuando el administrador de la otra empresa "beto@beta.pe" intenta cerrar la incidencia "averia" con la acción "Intento ajeno"
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "La incidencia no existe."
