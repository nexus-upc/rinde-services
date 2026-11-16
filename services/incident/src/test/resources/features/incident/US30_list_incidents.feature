# language: es
Característica: US30 Consultar las incidencias reportadas
  Como responsable de operaciones
  Quiero consultar en un listado las incidencias reportadas por los conductores
  Para atenderlas oportunamente y darles seguimiento

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que el administrador invitó a "Marta Ruiz" con el correo "marta@andes.pe" y el rol "DRIVER"
    Y que "marta@andes.pe" definió la contraseña "Clave-De-Marta-2026" con su enlace de invitación
    Y que existe el vehículo "camión"
    Y que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Y que el conductor "luis@andes.pe" inició sesión
    Y que el conductor "marta@andes.pe" inició sesión
    Y que el conductor inicia el viaje "viaje"
    Y que el conductor "luis@andes.pe" reportó la incidencia "averia" de tipo "BREAKDOWN" con la descripción "Falla en el freno neumático" sin evidencia en el viaje "viaje"
    Y que el conductor "luis@andes.pe" reportó la incidencia "retraso" de tipo "DELAY" con la descripción "Retraso por cierre de vía" sin evidencia en el viaje "viaje"
    Y que el conductor "luis@andes.pe" reportó la incidencia "otra" de tipo "OTHER" con la descripción "Cambio de ruta" sin evidencia en el viaje "viaje"

  Escenario: Listado de incidencias ordenado por fecha con sus datos
    Cuando el administrador consulta las incidencias
    Entonces la respuesta tiene código 200
    Y la lista tiene un total de 3
    Y las incidencias de la lista están ordenadas de la más reciente a la más antigua
    Y la lista muestra el tipo, el viaje, el vehículo, el conductor y el estado de cada incidencia

  Escenario: Filtrado del listado por estado de atención
    Dado que la incidencia "averia" está en atención
    Cuando el administrador consulta las incidencias en estado "IN_PROGRESS"
    Entonces la respuesta tiene código 200
    Y la lista tiene un total de 1
    Y la lista incluye la incidencia "averia"
    Y todas las incidencias de la lista tienen el estado "IN_PROGRESS"

  Escenario: Consultar el detalle de una incidencia
    Cuando el administrador consulta el detalle de la incidencia "retraso"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye la incidencia "retraso"
    Y la respuesta incluye el campo "status" con el valor "OPEN"

  Escenario: El conductor solo consulta el detalle de sus propias incidencias
    Cuando el conductor "marta@andes.pe" consulta el detalle de la incidencia "averia"
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "La incidencia no existe."

  Escenario: El conductor no puede consultar el listado de la empresa
    Cuando el conductor "luis@andes.pe" consulta las incidencias
    Entonces la respuesta tiene código 403

  Escenario: La incidencia de otra empresa no es visible
    Dado que existe otra empresa verificada con RUC "20456789012" y administrador "beto@beta.pe"
    Y que el administrador de la otra empresa "beto@beta.pe" inició sesión
    Cuando el administrador de la otra empresa "beto@beta.pe" consulta el detalle de la incidencia "averia"
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "La incidencia no existe."
