# language: es
Característica: US29 Reportar una incidencia desde el móvil
  Como conductor
  Quiero reportar una avería, un retraso u otro acontecimiento desde la aplicación
  Para que quede constancia asociada a mi viaje y al vehículo

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

  Escenario: Registro de la incidencia vinculada al viaje y al vehículo
    Cuando el conductor "luis@andes.pe" reporta la incidencia "averia" de tipo "BREAKDOWN" con la descripción "Falla en el freno neumático" y la evidencia "https://storage.rinde.pe/evidences/incidencia-1.jpg" en el viaje "viaje"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "status" con el valor "OPEN"
    Y la respuesta incluye el campo "type" con el valor "BREAKDOWN"
    Y la respuesta incluye el vehículo "camión" y el viaje "viaje"
    Y se publica el evento "IncidentReported" para "luis@andes.pe"
    Y el administrador consulta las incidencias
    Y la lista incluye la incidencia "averia"

  Escenario: Reporte sin conexión sincronizado al recuperar la señal
    Cuando el conductor "luis@andes.pe" sincroniza la incidencia "retraso" de tipo "DELAY" con la descripción "Retraso por cierre de vía" y la clave "INC-OFFLINE-0001" en el viaje "viaje"
    Entonces la respuesta tiene código 201
    Cuando el conductor "luis@andes.pe" sincroniza la incidencia "retraso" de tipo "DELAY" con la descripción "Retraso por cierre de vía" y la clave "INC-OFFLINE-0001" en el viaje "viaje"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye la incidencia "retraso"
    Y se publica una sola vez el evento "IncidentReported"
    Y el administrador consulta las incidencias
    Y la lista tiene un total de 1

  Escenario: Rechazar el reporte de un conductor que no está asignado al viaje
    Cuando el conductor "marta@andes.pe" reporta la incidencia "intruso" de tipo "OTHER" con la descripción "Reporte ajeno" sin evidencia en el viaje "viaje"
    Entonces la respuesta tiene código 403
    Y el mensaje de error es "Solo el conductor asignado al viaje puede reportar incidencias."

  Escenario: Rechazar el reporte cuando el viaje todavía no está en ruta
    Dado que el administrador programa el viaje "asignado" de "Lima" a "Cusco" con carga "Cemento" y salida "HOY+6"
    Y que el administrador asigna el viaje "asignado" al vehículo "camión" y al conductor "luis@andes.pe"
    Cuando el conductor "luis@andes.pe" reporta la incidencia "temprana" de tipo "BREAKDOWN" con la descripción "Falla antes de salir" sin evidencia en el viaje "asignado"
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "Solo se puede reportar una incidencia cuando el viaje está en ruta."

  Escenario: Rechazar el reporte de un viaje que no existe en la empresa
    Cuando el conductor "luis@andes.pe" reporta la incidencia "fantasma" de tipo "OTHER" con la descripción "Viaje inexistente" sin evidencia en un viaje que no existe
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El viaje no existe."

  Escenario: Rechazar el reporte de un viaje de otra empresa
    Dado que existe otra empresa verificada con RUC "20456789012" y administrador "beto@beta.pe"
    Y que existe otro viaje "ajeno" en la otra empresa
    Cuando el conductor "luis@andes.pe" reporta la incidencia "ajena" de tipo "OTHER" con la descripción "Viaje de otra empresa" sin evidencia en el viaje "ajeno"
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El viaje no existe."

  Escenario: Reportar una incidencia requiere iniciar sesión
    Cuando una persona sin token reporta una incidencia en el viaje "viaje"
    Entonces la respuesta tiene código 401
