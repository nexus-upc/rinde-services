# language: es
Característica: US37 Consultar estado de flota y métricas operativas
  Como administrador u operador de la empresa
  Quiero ver el estado de mi flota y las métricas de operación
  Para tomar decisiones informadas desde el dashboard

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión
    Y que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Y que el administrador registró el vehículo "camión" con placa "XYZ-001", marca "Volvo", modelo "FH16", año 2023 y capacidad "20000"
    Y que el administrador registró el conductor "Luis Quispe" con documento "DNI" "12345678", licencia "Q12345678" categoría "AIIIB" y vencimiento "2027-12-31"

  Escenario: Estado de flota con un vehículo y un conductor
    Cuando el administrador consulta el estado de la flota
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "totalVehicles" con el valor "1"
    Y la respuesta incluye el campo "totalDrivers" con el valor "1"

  Escenario: Métricas operativas sin viajes
    Cuando el administrador consulta las métricas operativas
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "totalTrips" con el valor "0"
    Y la respuesta incluye el campo "totalExpenses" con el valor "0"

  Escenario: Métricas operativas con un viaje programado
    Dado que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Cuando el administrador consulta las métricas operativas
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "totalTrips" con el valor "1"
    Y la respuesta incluye el campo "scheduledTrips" con el valor "1"

  Escenario: Las métricas del periodo suman solo los gastos aprobados dentro del rango
    Dado que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que existe un gasto aprobado de "120.00" soles con fecha "HOY-3" en el viaje "viaje"
    Y que existe un gasto aprobado de "80.00" soles con fecha "HOY-40" en el viaje "viaje"
    Y que el conductor registró un gasto "pendiente" de tipo "TOLL" por el monto "30.00" con clave "KEY-PERIODO-2026" en el viaje "viaje"
    Cuando el administrador consulta las métricas operativas del periodo "HOY-7" a "HOY"
    Entonces la respuesta tiene código 200
    Y la respuesta incluye en "approvedExpenseTotals" el campo "PEN" con el valor "120.0"
    Y la respuesta incluye el campo "pendingExpenses" con el valor "1"
    Y la respuesta incluye el campo "partial" con el valor "false"

  Escenario: Las liquidaciones pendientes de cierre se cuentan al terminar un viaje
    Dado que el administrador programa el viaje "viaje" de "Lima" a "Arequipa" con carga "Repuestos" y salida "HOY+5"
    Y que el administrador asigna el viaje "viaje" al vehículo "camión" y al conductor "luis@andes.pe"
    Y que el conductor "luis@andes.pe" inició sesión
    Y que el conductor inicia el viaje "viaje"
    Y que el conductor "luis@andes.pe" finaliza el viaje "viaje"
    Cuando el administrador consulta las métricas operativas
    Entonces la respuesta tiene código 200
    Y la respuesta incluye el campo "pendingSettlements" con el valor "1"

  Escenario: Incidencias abiertas disponibles
    Dado que la empresa tiene 2 incidencias abiertas
    Cuando el administrador consulta las métricas operativas
    Entonces la respuesta tiene código 200
    Y la respuesta incluye en "incidents" el campo "openCount" con el valor "2"
    Y la respuesta incluye en "incidents" el campo "source" con el valor "AVAILABLE"
    Y la respuesta incluye el campo "partial" con el valor "false"

  Escenario: Si el servicio de incidencias no responde, el resto de las métricas se entrega
    Dado que el servicio de incidencias no responde
    Cuando el administrador consulta las métricas operativas
    Entonces la respuesta tiene código 200
    Y la respuesta incluye en "incidents" el campo "source" con el valor "UNAVAILABLE"
    Y la respuesta incluye el campo "partial" con el valor "true"
    Y la respuesta incluye el campo "totalTrips" con el valor "0"

  Escenario: El periodo con fecha final anterior a la inicial se rechaza
    Cuando el administrador consulta las métricas operativas del periodo "HOY" a "HOY-7"
    Entonces la respuesta tiene código 400
    Y el mensaje de error es "La fecha final no puede ser anterior a la inicial."

  Escenario: El periodo con formato de fecha inválido se rechaza
    Cuando el administrador consulta las métricas operativas del periodo "2026/11/01" a "HOY"
    Entonces la respuesta tiene código 400

  Escenario: Estado de flota requiere autenticación
    Cuando una persona sin token consulta el estado de la flota
    Entonces la respuesta tiene código 401

  Escenario: Métricas requieren autenticación
    Cuando una persona sin token consulta las métricas operativas
    Entonces la respuesta tiene código 401
