# language: es
Característica: US12 Registrar y listar vehículos
  Como administrador o responsable de operaciones
  Quiero registrar vehículos en la flota y listar las unidades registradas
  Para tener control de la flota disponible para asignaciones de viajes

  Antecedentes:
    Dado que existe una empresa verificada con RUC "20123456789" y administrador "ana@andes.pe"
    Y que el administrador "ana@andes.pe" inició sesión

  Escenario: Registrar un vehículo exitosamente
    Cuando el administrador registra el vehículo "camion" con placa "ABC-123", marca "Volvo", modelo "FH540", año 2022 y capacidad "15000.00"
    Entonces la respuesta tiene código 201
    Y la respuesta incluye el campo "plateNumber" con el valor "ABC-123"
    Y la respuesta incluye el campo "status" con el valor "AVAILABLE"

  Escenario: Rechazar registro por placa duplicada en la misma empresa
    Dado que el administrador registró el vehículo "camion" con placa "ABC-123", marca "Volvo", modelo "FH540", año 2022 y capacidad "15000.00"
    Cuando el administrador registra el vehículo "otro_camion" con placa "ABC-123", marca "Scania", modelo "R500", año 2023 y capacidad "18000.00"
    Entonces la respuesta tiene código 409
    Y el mensaje de error es "La placa ya se encuentra registrada en la empresa."

  Escenario: Rechazar registro de vehículo con capacidad de carga inválida
    Cuando el administrador intenta registrar un vehículo con capacidad "0"
    Entonces la respuesta tiene código 400

  Escenario: Rechazar registro de vehículo sin placa
    Cuando el administrador intenta registrar un vehículo sin placa
    Entonces la respuesta tiene código 400

  Escenario: Listar vehículos solo de la empresa autenticada
    Dado que el administrador registró el vehículo "camion1" con placa "ABC-123", marca "Volvo", modelo "FH540", año 2022 y capacidad "15000.00"
    Y que existe una empresa verificada con RUC "20987654321" y administrador "otro@otra.pe"
    Y que el administrador "otro@otra.pe" inició sesión
    Y que el administrador registró el vehículo "camion2" con placa "XYZ-789", marca "Mercedes", modelo "Actros", año 2021 y capacidad "12000.00"
    Y que el administrador "ana@andes.pe" inició sesión
    Cuando el administrador lista los vehículos de su empresa
    Entonces la respuesta tiene código 200
    Y la lista contiene 1 vehículos
    Y la lista incluye el vehículo con placa "ABC-123"
    Y la lista no incluye el vehículo con placa "XYZ-789"

  Escenario: Un conductor no puede registrar vehículos
    Dado que el administrador invitó a "Luis Quispe" con el correo "luis@andes.pe" y el rol "DRIVER"
    Y que "luis@andes.pe" definió la contraseña "Clave-De-Luis-2026" con su enlace de invitación
    Cuando el conductor "luis@andes.pe" intenta registrar un vehículo con placa "DEF-456"
    Entonces la respuesta tiene código 403

  Escenario: Rechazar un vehículo que supera el límite de unidades del plan
    Dado que la empresa con RUC "20123456789" tiene un plan con límite de 2 unidades
    Y que el administrador registró el vehículo "camion" con placa "ABC-123", marca "Volvo", modelo "FH540", año 2022 y capacidad "15000.00"
    Y que el administrador registró el vehículo "camion2" con placa "XYZ-789", marca "Mercedes", modelo "Actros", año 2021 y capacidad "12000.00"
    Cuando el administrador registra el vehículo "adicional" con placa "DEF-456", marca "Volvo", modelo "FH", año 2022 y capacidad "5000"
    Entonces la respuesta tiene código 409
    Y el mensaje de error contiene "plan con mayor capacidad"
