# language: es
Característica: US32 Ficha e historial consolidado del vehículo
  Como administrador o responsable de operaciones
  Quiero consultar el historial unificado de mantenimientos, viajes e incidencias de una unidad
  Para evaluar el uso y estado operativo del vehículo en una sola consulta

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador registró el vehículo "camion" con placa "ABC-123", marca "Volvo", modelo "FH540", año 2022 y capacidad "15000.00"

  Escenario: Consultar el historial unificado con mantenimientos, viajes e incidencias ordenados por fecha
    Dado que se registra un mantenimiento "PREVENTIVE" para el vehículo "camion" con ejecución "HOY-10", costo "450.00" y próximo mantenimiento "HOY+60"
    Y que Trip responde el viaje "TRP-000014" para el vehículo "camion" en fecha "HOY-5"
    Y que Incident responde la incidencia "BREAKDOWN" para el vehículo "camion" en fecha "HOY-2"
    Cuando el administrador consulta el historial del vehículo "camion"
    Entonces la respuesta tiene código 200
    Y el historial tiene 3 eventos ordenados de más reciente a más antiguo
    Y el historial no está marcado como parcial

  Escenario: Consultar el historial cuando Trip no responde devuelve respuesta parcial
    Dado que se registra un mantenimiento "PREVENTIVE" para el vehículo "camion" con ejecución "HOY-10", costo "450.00" y próximo mantenimiento "HOY+60"
    Y que Trip no responde para la empresa de "20123456789"
    Y que Incident no tiene incidencias para el vehículo "camion"
    Cuando el administrador consulta el historial del vehículo "camion"
    Entonces la respuesta tiene código 200
    Y el historial está marcado como parcial con la fuente "TRIP" no disponible
    Y el historial contiene 1 evento de tipo "MAINTENANCE" sin estado

  Escenario: Rechazar consulta de historial con rango de fechas invertido
    Cuando el administrador consulta el historial del vehículo "camion" desde "HOY-1" hasta "HOY-10"
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "La fecha final no puede ser anterior a la inicial."

  Escenario: Un usuario de otra empresa no puede consultar el historial de una unidad ajena
    Dado que existe una empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
    Y que el administrador "otro@otra.pe" inició sesión
    Cuando el administrador "otro@otra.pe" consulta el historial del vehículo "camion"
    Entonces la respuesta tiene código 404
    Y el mensaje de error es "El vehículo no existe."
